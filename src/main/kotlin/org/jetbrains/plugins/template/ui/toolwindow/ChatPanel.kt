package org.jetbrains.plugins.template.ui.toolwindow

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import org.jetbrains.plugins.template.api.AskSageApiClient
import org.jetbrains.plugins.template.api.AskSageApiException
import org.jetbrains.plugins.template.api.auth.AuthManager
import org.jetbrains.plugins.template.api.models.QueryRequest
import org.jetbrains.plugins.template.services.AskSageSettingsState
import org.jetbrains.plugins.template.services.ChatMessage
import org.jetbrains.plugins.template.services.ChatSessionService
import org.jetbrains.plugins.template.services.MessageRole
import org.jetbrains.plugins.template.services.ModelRegistryService
import org.jetbrains.plugins.template.util.LiveMode
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollBar
import javax.swing.JTextPane
import javax.swing.SwingUtilities
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants

class ChatPanel(private val project: Project) : JPanel(BorderLayout()) {

    private val chatSessionService = project.service<ChatSessionService>()
    private val settings = AskSageSettingsState.getInstance()
    private val authManager = AuthManager.getInstance()
    private val modelRegistry = ModelRegistryService.getInstance()
    private val apiClient = AskSageApiClient(settings.baseUrl)

    private val chatDisplay = JTextPane().apply {
        isEditable = false
        contentType = "text/plain"
        border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
    }
    private val chatScrollPane = JBScrollPane(chatDisplay)

    private val inputArea = JBTextArea(3, 0).apply {
        lineWrap = true
        wrapStyleWord = true
        border = BorderFactory.createEmptyBorder(4, 8, 4, 8)
    }

    private val sendButton = JButton("Send").apply {
        preferredSize = Dimension(80, 28)
    }

    private val clearButton = JButton("Clear").apply {
        preferredSize = Dimension(70, 28)
    }

    private val liveModeToggle = LiveModeToggle(
        initialMode = LiveMode.fromValue(settings.defaultLiveMode),
        onModeChanged = { /* persisted via selectedMode property */ }
    )

    private val modelSelector = ModelSelector(
        onModeChanged@{ modelId ->
            settings.defaultModel = modelId
        }
    )

    private val statusLabel = JLabel("Ready").apply {
        foreground = JBColor.GRAY
    }

    init {
        setupUI()
        setupActions()
        loadModels()
    }

    private fun setupUI() {
        // Top toolbar: live mode toggle + model selector
        val toolbar = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            border = BorderFactory.createEmptyBorder(4, 4, 4, 4)
            add(liveModeToggle)
            add(Box.createHorizontalStrut(8))
            add(JLabel("Model:"))
            add(Box.createHorizontalStrut(4))
            add(modelSelector)
            add(Box.createHorizontalGlue())
        }

        // Chat display area
        chatScrollPane.border = BorderFactory.createEmptyBorder()

        // Input panel: text area + send/clear buttons
        val buttonPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(4, 4, 4, 4)
            add(sendButton)
            add(Box.createVerticalStrut(4))
            add(clearButton)
        }

        val inputPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, JBColor.border()),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)
            )
            add(JBScrollPane(inputArea).apply {
                preferredSize = Dimension(0, 70)
                border = BorderFactory.createLineBorder(JBColor.border())
            }, BorderLayout.CENTER)
            add(buttonPanel, BorderLayout.EAST)
        }

        // Status bar
        val statusPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            border = BorderFactory.createMatteBorder(1, 0, 0, 0, JBColor.border())
            add(statusLabel)
        }

        add(toolbar, BorderLayout.NORTH)
        add(chatScrollPane, BorderLayout.CENTER)
        add(JPanel(BorderLayout()).apply {
            add(inputPanel, BorderLayout.CENTER)
            add(statusPanel, BorderLayout.SOUTH)
        }, BorderLayout.SOUTH)

        // Restore chat history
        for (msg in chatSessionService.getMessages()) {
            appendMessage(msg.role, msg.content, msg.model)
        }
    }

    private fun setupActions() {
        sendButton.addActionListener { sendMessage() }
        clearButton.addActionListener { clearChat() }

        // Enter to send (Shift+Enter for newline)
        inputArea.addKeyListener(object : java.awt.event.KeyAdapter() {
            override fun keyPressed(e: java.awt.event.KeyEvent) {
                if (e.keyCode == java.awt.event.KeyEvent.VK_ENTER && !e.isShiftDown) {
                    e.consume()
                    sendMessage()
                }
            }
        })
    }

    private fun sendMessage() {
        val message = inputArea.text.trim()
        if (message.isEmpty()) return

        if (!authManager.isConfigured()) {
            appendMessage(MessageRole.ERROR, "Please configure your API key and email in Settings > Tools > Pymatic AskSage", null)
            return
        }

        val selectedModel = (modelSelector.selectedItem as? org.jetbrains.plugins.template.api.models.ModelInfo)?.id
        if (selectedModel.isNullOrBlank()) {
            appendMessage(MessageRole.ERROR, "Please select a model", null)
            return
        }

        inputArea.text = ""
        appendMessage(MessageRole.USER, message, null)
        chatSessionService.addMessage(ChatMessage(MessageRole.USER, message))

        setLoading(true)
        statusLabel.text = "Querying AskSage..."

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                apiClient.updateBaseUrl(settings.baseUrl)
                val token = authManager.getAccessToken(apiClient)
                    ?: throw AskSageApiException("Authentication failed")

                val queryRequest = QueryRequest(
                    model = selectedModel,
                    message = message,
                    live = liveModeToggle.selectedMode.value,
                    dataset = settings.defaultDataset.ifBlank { null },
                    persona = if (settings.defaultPersona > 0) settings.defaultPersona else null,
                    temperature = settings.temperature,
                    reasoningEffort = settings.reasoningEffort,
                )

                val response = apiClient.query(token, queryRequest)
                val responseText = response.response ?: response.message ?: "No response received"

                SwingUtilities.invokeLater {
                    appendMessage(MessageRole.ASSISTANT, responseText, selectedModel)
                    chatSessionService.addMessage(ChatMessage(MessageRole.ASSISTANT, responseText, selectedModel))
                    setLoading(false)
                    statusLabel.text = "Ready"
                }
            } catch (e: AskSageApiException) {
                LOG.warn("Query failed", e)
                SwingUtilities.invokeLater {
                    appendMessage(MessageRole.ERROR, "Error: ${e.message}", null)
                    chatSessionService.addMessage(ChatMessage(MessageRole.ERROR, "Error: ${e.message}"))
                    setLoading(false)
                    statusLabel.text = "Error"
                }
            }
        }
    }

    private fun appendMessage(role: MessageRole, content: String, model: String?) {
        val doc = chatDisplay.styledDocument
        val attrs = SimpleAttributeSet()

        val prefix = when (role) {
            MessageRole.USER -> {
                StyleConstants.setBold(attrs, true)
                StyleConstants.setForeground(attrs, JBColor.foreground())
                "You"
            }
            MessageRole.ASSISTANT -> {
                StyleConstants.setForeground(attrs, JBColor(java.awt.Color(0, 100, 0), java.awt.Color(100, 200, 100)))
                val modelTag = if (model != null) " [$model]" else ""
                "AskSage$modelTag"
            }
            MessageRole.ERROR -> {
                StyleConstants.setForeground(attrs, JBColor.RED)
                StyleConstants.setItalic(attrs, true)
                "Error"
            }
        }

        val headerAttrs = SimpleAttributeSet(attrs)
        StyleConstants.setBold(headerAttrs, true)

        val contentAttrs = SimpleAttributeSet()
        if (role == MessageRole.ERROR) {
            StyleConstants.setForeground(contentAttrs, JBColor.RED)
            StyleConstants.setItalic(contentAttrs, true)
        }

        if (doc.length > 0) {
            doc.insertString(doc.length, "\n\n", null)
        }
        doc.insertString(doc.length, "$prefix:\n", headerAttrs)
        doc.insertString(doc.length, content, contentAttrs)

        // Scroll to bottom
        SwingUtilities.invokeLater {
            val scrollBar: JScrollBar = chatScrollPane.verticalScrollBar
            scrollBar.value = scrollBar.maximum
        }
    }

    private fun clearChat() {
        chatSessionService.clearHistory()
        chatDisplay.text = ""
        statusLabel.text = "Ready"
    }

    private fun setLoading(loading: Boolean) {
        sendButton.isEnabled = !loading
        inputArea.isEnabled = !loading
    }

    private fun loadModels() {
        if (!authManager.isConfigured()) return

        ApplicationManager.getApplication().executeOnPooledThread {
            apiClient.updateBaseUrl(settings.baseUrl)
            if (modelRegistry.needsRefresh()) {
                modelRegistry.refreshModels(apiClient)
            }
            val models = modelRegistry.getModels()
            SwingUtilities.invokeLater {
                modelSelector.updateModels(models)
                if (settings.defaultModel.isNotBlank()) {
                    modelSelector.setSelectedModelId(settings.defaultModel)
                }
            }
        }
    }

    companion object {
        private val LOG = logger<ChatPanel>()
    }
}
