package org.jetbrains.plugins.template.ui.toolwindow

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import org.jetbrains.plugins.template.api.AskSageApiClient
import org.jetbrains.plugins.template.api.AskSageApiException
import org.jetbrains.plugins.template.api.auth.AuthManager
import org.jetbrains.plugins.template.api.models.AgentInfo
import org.jetbrains.plugins.template.api.models.ExecuteAgentRequest
import org.jetbrains.plugins.template.api.models.ModelInfo
import org.jetbrains.plugins.template.services.AskSageSettingsState
import org.jetbrains.plugins.template.services.ModelRegistryService
import org.jetbrains.plugins.template.util.LiveMode
import org.jetbrains.plugins.template.util.MarkdownRenderer
import org.jetbrains.plugins.template.util.NotificationHelper
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

class AgentPanel(private val project: Project) : JPanel(BorderLayout()) {

    private val settings = AskSageSettingsState.getInstance()
    private val authManager = AuthManager.getInstance()
    private val modelRegistry = ModelRegistryService.getInstance()
    private val apiClient = AskSageApiClient(settings.baseUrl)

    private val agentComboModel = DefaultComboBoxModel<AgentInfo>()
    private val agentSelector = ComboBox(agentComboModel).apply {
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
                if (value is AgentInfo) {
                    text = value.name
                    toolTipText = value.description ?: ""
                }
                return this
            }
        }
    }

    private val modelSelector = ModelSelector { /* selection tracked via selectedItem */ }

    private val inputArea = JBTextArea(3, 0).apply {
        lineWrap = true
        wrapStyleWord = true
        border = BorderFactory.createEmptyBorder(4, 8, 4, 8)
    }

    private val executeButton = JButton("Run Agent").apply {
        preferredSize = Dimension(110, 28)
    }

    private val refreshButton = JButton("Refresh").apply {
        preferredSize = Dimension(90, 28)
    }

    private val resultDisplay = JTextPane().apply {
        isEditable = false
        border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
    }

    private val statusLabel = JLabel("Select an agent and enter a message").apply {
        foreground = JBColor.GRAY
    }

    init {
        setupUI()
        setupActions()
        loadData()
    }

    private fun setupUI() {
        val toolbar = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            border = BorderFactory.createEmptyBorder(4, 4, 4, 4)
            add(JLabel("Agent:"))
            add(Box.createHorizontalStrut(4))
            add(agentSelector)
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
        executeButton.addActionListener { executeAgent() }
        refreshButton.addActionListener { loadData() }
    }

    private fun executeAgent() {
        val agent = agentSelector.selectedItem as? AgentInfo ?: return
        val message = inputArea.text.trim()
        if (message.isEmpty()) return

        val selectedModel = (modelSelector.selectedItem as? ModelInfo)?.id
        if (selectedModel.isNullOrBlank()) {
            statusLabel.text = "Please select a model"
            return
        }

        executeButton.isEnabled = false
        statusLabel.text = "Running ${agent.name}..."

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                apiClient.updateBaseUrl(settings.baseUrl)
                val token = authManager.getAccessToken(apiClient)
                    ?: throw AskSageApiException("Authentication failed")

                val request = ExecuteAgentRequest(
                    agent = agent.name,
                    message = message,
                    model = selectedModel,
                    live = LiveMode.fromValue(settings.defaultLiveMode).value,
                )

                val response = apiClient.executeAgent(token, request)
                val result = response.response ?: response.message ?: "No response"

                SwingUtilities.invokeLater {
                    resultDisplay.text = ""
                    MarkdownRenderer.render(resultDisplay, result)
                    executeButton.isEnabled = true
                    statusLabel.text = "Agent completed successfully"
                }
            } catch (e: AskSageApiException) {
                LOG.warn("Agent execution failed", e)
                NotificationHelper.error(project, "Agent Execution Failed", e.message ?: "Unknown error")
                SwingUtilities.invokeLater {
                    resultDisplay.text = "Error: ${e.message}"
                    executeButton.isEnabled = true
                    statusLabel.text = "Error"
                }
            }
        }
    }

    private fun loadData() {
        if (!authManager.isConfigured()) return

        ApplicationManager.getApplication().executeOnPooledThread {
            apiClient.updateBaseUrl(settings.baseUrl)
            val token = authManager.getAccessToken(apiClient) ?: return@executeOnPooledThread

            try {
                val agentsResponse = apiClient.listAgents(token)
                val agents = agentsResponse.response ?: emptyList()

                if (modelRegistry.needsRefresh()) {
                    modelRegistry.refreshModels(apiClient)
                }
                val models = modelRegistry.getModels()

                SwingUtilities.invokeLater {
                    agentComboModel.removeAllElements()
                    agents.forEach { agentComboModel.addElement(it) }
                    modelSelector.updateModels(models)
                    if (settings.defaultModel.isNotBlank()) {
                        modelSelector.setSelectedModelId(settings.defaultModel)
                    }
                    statusLabel.text = "Loaded ${agents.size} agents"
                }
            } catch (e: AskSageApiException) {
                LOG.warn("Failed to load agents", e)
                NotificationHelper.warn(project, "AskSage", "Failed to load agents: ${e.message}")
                SwingUtilities.invokeLater {
                    statusLabel.text = "Failed to load agents: ${e.message}"
                }
            }
        }
    }

    companion object {
        private val LOG = logger<AgentPanel>()
    }
}
