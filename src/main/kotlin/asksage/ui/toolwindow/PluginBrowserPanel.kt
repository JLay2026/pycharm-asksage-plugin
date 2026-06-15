package asksage.ui.toolwindow

import com.google.gson.Gson
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import asksage.api.AskSageApiClient
import asksage.api.AskSageApiException
import asksage.api.auth.AuthManager
import asksage.api.models.ExecutePluginRequest
import asksage.api.models.ModelInfo
import asksage.api.models.PluginInfo
import asksage.services.AskSageSettingsState
import asksage.services.ModelRegistryService
import asksage.services.AskSageRefreshListener
import asksage.services.AskSageRefreshTopic
import asksage.util.LiveMode
import asksage.util.MarkdownRenderer
import asksage.util.NotificationHelper
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.DefaultComboBoxModel
import javax.swing.DefaultListCellRenderer
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JTextPane
import javax.swing.SwingUtilities

class PluginBrowserPanel(private val project: Project) : JPanel(BorderLayout()) {

    private val settings = AskSageSettingsState.getInstance()
    private val authManager = AuthManager.getInstance()
    private val modelRegistry = ModelRegistryService.getInstance()
    private val apiClient = AskSageApiClient(settings.baseUrl)
    private val gson = Gson()

    private val pluginComboModel = DefaultComboBoxModel<PluginInfo>()
    private val pluginSelector = ComboBox(pluginComboModel).apply {
        preferredSize = Dimension(250, 28)
        renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean,
            ): Component {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                if (value is PluginInfo) {
                    text = value.displayName
                    toolTipText = value.description ?: ""
                }
                return this
            }
        }
    }

    private val modelSelector = ModelSelector { modelId -> settings.defaultModel = modelId }

    private val inputArea = JBTextArea(3, 0).apply {
        lineWrap = true
        wrapStyleWord = true
        border = BorderFactory.createEmptyBorder(4, 8, 4, 8)
    }

    private val executeButton = JButton("Execute Plugin").apply {
        preferredSize = Dimension(130, 28)
    }

    private val refreshButton = JButton("Refresh").apply {
        preferredSize = Dimension(90, 28)
    }

    private val resultDisplay = JTextPane().apply {
        isEditable = false
        border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
    }

    private val statusLabel = JLabel("Select a plugin and enter a message").apply {
        foreground = JBColor.GRAY
    }

    init {
        setupUI()
        setupActions()
        loadData()
        ApplicationManager.getApplication().messageBus.connect(project)
            .subscribe(AskSageRefreshTopic.TOPIC, AskSageRefreshListener { reload() })
    }

    private fun setupUI() {
        val toolbar = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            border = BorderFactory.createEmptyBorder(4, 4, 4, 4)
            add(JLabel("Plugin:"))
            add(Box.createHorizontalStrut(4))
            add(pluginSelector)
            add(Box.createHorizontalStrut(8))
            add(JLabel("Model:"))
            add(Box.createHorizontalStrut(4))
            add(modelSelector)
            add(Box.createHorizontalStrut(8))
            add(refreshButton)
            add(Box.createHorizontalGlue())
        }

        val inputPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, JBColor.border()),
                BorderFactory.createEmptyBorder(4, 4, 4, 4),
            )
            add(
                JBScrollPane(inputArea).apply {
                    preferredSize = Dimension(0, 70)
                    border = BorderFactory.createLineBorder(JBColor.border())
                },
                BorderLayout.CENTER,
            )
            add(JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                border = BorderFactory.createEmptyBorder(4, 4, 4, 4)
                add(executeButton)
            }, BorderLayout.EAST)
        }

        val statusPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createMatteBorder(1, 0, 0, 0, JBColor.border())
            add(statusLabel, BorderLayout.WEST)
        }

        val bottomArea = JPanel(BorderLayout()).apply {
            add(inputPanel, BorderLayout.CENTER)
            add(statusPanel, BorderLayout.SOUTH)
        }

        add(toolbar, BorderLayout.NORTH)
        add(JBScrollPane(resultDisplay), BorderLayout.CENTER)
        add(bottomArea, BorderLayout.SOUTH)
    }

    private fun setupActions() {
        executeButton.addActionListener { executePlugin() }
        refreshButton.addActionListener { loadData() }
    }

    private fun executePlugin() {
        val plugin = pluginSelector.selectedItem as? PluginInfo ?: return
        val message = inputArea.text.trim()
        if (message.isEmpty()) return

        val selectedModel = (modelSelector.selectedItem as? ModelInfo)?.id
        if (selectedModel.isNullOrBlank()) {
            statusLabel.text = "Please select a model"
            return
        }

        executeButton.isEnabled = false
        statusLabel.text = "Executing ${plugin.displayName}..."

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                apiClient.updateBaseUrl(settings.baseUrl)
                val token = authManager.getAccessToken(apiClient)
                    ?: throw AskSageApiException("Authentication failed")

                val request = ExecutePluginRequest(
                    pluginName = plugin.identifier,
                    pluginValues = gson.toJson(mapOf("message" to message)),
                    model = selectedModel,
                    live = LiveMode.fromValue(settings.defaultLiveMode).value,
                )

                val result = apiClient.executePlugin(token, request).ifBlank { "No response" }

                SwingUtilities.invokeLater {
                    resultDisplay.text = ""
                    MarkdownRenderer.render(resultDisplay, result)
                    executeButton.isEnabled = true
                    statusLabel.text = "Plugin executed successfully"
                }
            } catch (e: AskSageApiException) {
                LOG.warn("Plugin execution failed", e)
                NotificationHelper.error(project, "Plugin Execution Failed", e.message ?: "Unknown error")
                SwingUtilities.invokeLater {
                    resultDisplay.text = "Error: ${e.message}"
                    executeButton.isEnabled = true
                    statusLabel.text = "Error"
                }
            }
        }
    }

    fun reload() {
        loadData()
    }

    fun syncSelectedModel() {
        if (settings.defaultModel.isNotBlank()) {
            modelSelector.setSelectedModelId(settings.defaultModel)
        }
    }

    private fun loadData() {
        if (!authManager.isConfigured()) return

        ApplicationManager.getApplication().executeOnPooledThread {
            apiClient.updateBaseUrl(settings.baseUrl)
            val token = authManager.getAccessToken(apiClient) ?: return@executeOnPooledThread

            try {
                val pluginsResponse = apiClient.getPlugins(token)
                val plugins = pluginsResponse.response ?: emptyList()

                if (modelRegistry.needsRefresh()) {
                    modelRegistry.refreshModels(apiClient)
                }
                val models = modelRegistry.getVisibleModels(settings.includeGovModels)

                SwingUtilities.invokeLater {
                    pluginComboModel.removeAllElements()
                    plugins.forEach { pluginComboModel.addElement(it) }
                    modelSelector.updateModels(models)
                    if (settings.defaultModel.isNotBlank()) {
                        modelSelector.setSelectedModelId(settings.defaultModel)
                    }
                    statusLabel.text = "Loaded ${plugins.size} plugins"
                }
            } catch (e: AskSageApiException) {
                LOG.warn("Failed to load plugins", e)
                NotificationHelper.warn(project, "AskSage", "Failed to load plugins: ${e.message}")
                SwingUtilities.invokeLater {
                    statusLabel.text = "Failed to load plugins: ${e.message}"
                }
            }
        }
    }

    companion object {
        private val LOG = logger<PluginBrowserPanel>()
    }
}
