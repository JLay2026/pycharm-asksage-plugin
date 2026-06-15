package asksage.ui.toolwindow

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import asksage.api.AskSageApiClient
import asksage.api.AskSageApiException
import asksage.api.auth.AuthManager
import asksage.api.models.ModelInfo
import asksage.api.models.QueryRequest
import asksage.services.AskSageSettingsState
import asksage.services.AskSageRefreshListener
import asksage.services.AskSageRefreshTopic
import asksage.services.ChatMessage
import asksage.services.ChatSessionService
import asksage.services.DatasetRegistryService
import asksage.services.MessageRole
import asksage.services.ModelRegistryService
import asksage.services.PersonaRegistryService
import asksage.services.ProjectContextService
import asksage.services.PromptTemplate
import asksage.util.LiveMode
import asksage.util.MarkdownRenderer
import asksage.util.NotificationHelper
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollBar
import javax.swing.JTextPane
import javax.swing.SwingUtilities
import javax.swing.Timer
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import asksage.util.AskSageIcons

class ChatPanel(private val project: Project) : JPanel(BorderLayout()) {

    private val chatSessionService = project.service<ChatSessionService>()
    private val settings = AskSageSettingsState.getInstance()
    private val authManager = AuthManager.getInstance()
    private val modelRegistry = ModelRegistryService.getInstance()
    private val personaRegistry = PersonaRegistryService.getInstance()
    private val datasetRegistry = DatasetRegistryService.getInstance()
    private val apiClient = AskSageApiClient(settings.baseUrl)

    private val chatDisplay = JTextPane().apply {
        isEditable = false
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

    private val copyLastButton = JButton("", AskSageIcons.Copy).apply {
        toolTipText = "Copy last response"
        preferredSize = Dimension(28, 28)
        isFocusPainted = false
    }

    private val exportButton = JButton("", AskSageIcons.Export).apply {
        toolTipText = "Export conversation as Markdown"
        preferredSize = Dimension(28, 28)
        isFocusPainted = false
    }

    private val liveModeToggle = LiveModeToggle(
        initialMode = LiveMode.fromValue(settings.defaultLiveMode),
        onModeChanged = { /* persisted via selectedMode property */ },
    )

    private val modelSelector = ModelSelector(
        onModelChanged@{ modelId ->
            settings.defaultModel = modelId
        },
    )

    private var selectedPersonaId: Int? = if (settings.defaultPersona > 0) settings.defaultPersona else null
    private val personaSelector = PersonaSelector(
        onPersonaChanged@{ personaId ->
            selectedPersonaId = personaId
            settings.defaultPersona = personaId ?: 0
        },
    )

    private var selectedDataset: String? = settings.defaultDataset.ifBlank { null }
    private val datasetSelector = DatasetSelector(
        onDatasetChanged@{ dataset ->
            selectedDataset = dataset
            settings.defaultDataset = dataset ?: ""
        },
    )

    private var selectedTemplate: PromptTemplate? = null
    private val templateSelector = PromptTemplateSelector(
        onTemplateChanged@{ template ->
            selectedTemplate = template
        },
    )

    private val projectContextService = project.service<ProjectContextService>()

    private val followUpPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = BorderFactory.createEmptyBorder(4, 8, 4, 8)
        isVisible = false
    }

    private val statusLabel = JLabel("Ready").apply {
        foreground = JBColor.GRAY
    }

    private val elapsedLabel = JLabel("").apply {
        foreground = JBColor.GRAY
    }

    private var streamingStartTime: Long = 0
    private var elapsedTimer: Timer? = null
    private var streamingDocOffset = 0

    init {
        setupUI()
        setupActions()
        loadRegistries()
        ApplicationManager.getApplication().messageBus.connect(project)
            .subscribe(AskSageRefreshTopic.TOPIC, AskSageRefreshListener { reload() })
    }

    private fun setupUI() {
        // Top toolbar row 1: live mode + model selector
        val toolbarRow1 = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            border = BorderFactory.createEmptyBorder(4, 4, 2, 4)
            add(liveModeToggle)
            add(Box.createHorizontalStrut(8))
            add(JLabel("Model:"))
            add(Box.createHorizontalStrut(4))
            add(modelSelector)
            add(Box.createHorizontalGlue())
        }

        // Top toolbar row 2: persona selector
        // (Dataset selector hidden for now; datasetSelector retained for future re-enable.)
        val toolbarRow2 = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            border = BorderFactory.createEmptyBorder(2, 4, 2, 4)
            add(JLabel("Persona:"))
            add(Box.createHorizontalStrut(4))
            add(personaSelector)
            add(Box.createHorizontalGlue())
        }

        // Top toolbar row 3: prompt template selector
        val toolbarRow3 = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            border = BorderFactory.createEmptyBorder(2, 4, 4, 4)
            add(JLabel("Template:"))
            add(Box.createHorizontalStrut(4))
            add(templateSelector)
            add(Box.createHorizontalGlue())
        }

        val toolbar = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(toolbarRow1)
            add(toolbarRow2)
            add(toolbarRow3)
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
            add(Box.createVerticalStrut(4))
            add(copyLastButton)
            add(Box.createVerticalStrut(4))
            add(exportButton)
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
            add(buttonPanel, BorderLayout.EAST)
        }

        // Status bar with elapsed time
        val statusPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            border = BorderFactory.createMatteBorder(1, 0, 0, 0, JBColor.border())
            add(statusLabel)
            add(Box.createHorizontalStrut(12))
            add(elapsedLabel)
        }

        // Bottom area: follow-up panel + input + status
        val bottomArea = JPanel(BorderLayout()).apply {
            add(followUpPanel, BorderLayout.NORTH)
            add(inputPanel, BorderLayout.CENTER)
            add(statusPanel, BorderLayout.SOUTH)
        }

        add(toolbar, BorderLayout.NORTH)
        add(chatScrollPane, BorderLayout.CENTER)
        add(bottomArea, BorderLayout.SOUTH)

        // Restore chat history
        for (msg in chatSessionService.getMessages()) {
            appendMessage(msg.role, msg.content, msg.model)
        }
    }

    private fun setupActions() {
        sendButton.addActionListener { sendMessage() }
        clearButton.addActionListener { clearChat() }
        copyLastButton.addActionListener { copyLastResponse() }
        exportButton.addActionListener { exportConversation() }

        inputArea.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER && !e.isShiftDown) {
                    e.consume()
                    sendMessage()
                }
            }
        })
    }

    fun sendMessageWithContext(message: String) {
        inputArea.text = message
        sendMessage()
    }

    private fun sendMessage() {
        val message = inputArea.text.trim()
        if (message.isEmpty()) return

        if (!authManager.isConfigured()) {
            appendMessage(
                MessageRole.ERROR,
                "Please configure your API key and email in Settings > Tools > AskSage",
                null,
            )
            return
        }

        val selectedModel = (modelSelector.selectedItem as? ModelInfo)?.id
        if (selectedModel.isNullOrBlank()) {
            appendMessage(MessageRole.ERROR, "Please select a model", null)
            return
        }

        inputArea.text = ""
        followUpPanel.isVisible = false
        appendMessage(MessageRole.USER, message, null)
        chatSessionService.addMessage(ChatMessage(MessageRole.USER, message))

        setLoading(true)
        statusLabel.text = "Querying AskSage..."
        startElapsedTimer()

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                apiClient.updateBaseUrl(settings.baseUrl)
                val token = authManager.getAccessToken(apiClient)
                    ?: throw AskSageApiException("Authentication failed")

                // Build context from conversation history for multi-turn
                val conversationContext = chatSessionService.getConversationContext()
                val contextualMessage = if (conversationContext.isNotBlank()) {
                    "Previous conversation:\n$conversationContext\n\nCurrent question: $message"
                } else {
                    message
                }

                // Add project context prefix for multi-project awareness
                val projectPrefix = projectContextService.buildContextPrefix()
                val fullMessage = projectPrefix + contextualMessage

                val queryRequest = QueryRequest(
                    model = selectedModel,
                    message = fullMessage,
                    live = liveModeToggle.selectedMode.value,
                    dataset = selectedDataset,
                    persona = selectedPersonaId,
                    systemPrompt = selectedTemplate?.systemPrompt,
                    temperature = settings.temperature,
                    reasoningEffort = settings.reasoningEffort,
                )

                // Try streaming first, fall back to non-streaming
                val responseText = StringBuilder()

                try {
                    SwingUtilities.invokeAndWait {
                        appendStreamingHeader(selectedModel)
                    }

                    apiClient.queryStreaming(token, queryRequest) { chunk ->
                        responseText.append(chunk)
                        SwingUtilities.invokeLater {
                            updateStreamingContent(responseText.toString())
                        }
                    }
                } catch (e: AskSageApiException) {
                    LOG.info("Streaming not available, falling back to standard query", e)
                    responseText.clear()
                    val response = apiClient.query(token, queryRequest)
                    responseText.append(response.answer() ?: "No response received")

                    SwingUtilities.invokeAndWait {
                        if (streamingDocOffset > 0) {
                            val doc = chatDisplay.styledDocument
                            doc.remove(streamingDocOffset, doc.length - streamingDocOffset)
                            streamingDocOffset = 0
                        }
                        appendMessage(MessageRole.ASSISTANT, responseText.toString(), selectedModel)
                    }
                }

                val finalResponse = responseText.toString()
                if (finalResponse.isNotBlank() && streamingDocOffset > 0) {
                    SwingUtilities.invokeLater {
                        finalizeStreamingContent(finalResponse)
                    }
                }

                chatSessionService.addMessage(
                    ChatMessage(MessageRole.ASSISTANT, finalResponse, selectedModel),
                )

                SwingUtilities.invokeLater {
                    setLoading(false)
                    stopElapsedTimer()
                    statusLabel.text = "Ready"
                }

                fetchFollowUpQuestions(token, message, selectedModel)
            } catch (e: AskSageApiException) {
                LOG.warn("Query failed", e)
                NotificationHelper.error(project, "AskSage Query Failed", e.message ?: "Unknown error")
                SwingUtilities.invokeLater {
                    appendMessage(MessageRole.ERROR, "Error: ${e.message}", null)
                    chatSessionService.addMessage(ChatMessage(MessageRole.ERROR, "Error: ${e.message}"))
                    setLoading(false)
                    stopElapsedTimer()
                    statusLabel.text = "Error"
                }
            }
        }
    }

    private fun appendStreamingHeader(model: String) {
        val doc = chatDisplay.styledDocument
        val headerAttrs = SimpleAttributeSet()
        StyleConstants.setBold(headerAttrs, true)
        StyleConstants.setForeground(
            headerAttrs,
            ASSISTANT_HEADER_COLOR,
        )

        if (doc.length > 0) {
            doc.insertString(doc.length, "\n\n", null)
        }
        doc.insertString(doc.length, "AskSage [$model]:\n", headerAttrs)
        streamingDocOffset = doc.length
    }

    private fun updateStreamingContent(content: String) {
        if (streamingDocOffset <= 0) return
        val doc = chatDisplay.styledDocument
        val currentLen = doc.length
        if (currentLen > streamingDocOffset) {
            doc.remove(streamingDocOffset, currentLen - streamingDocOffset)
        }
        doc.insertString(streamingDocOffset, content, null)
        scrollToBottom()
    }

    private fun finalizeStreamingContent(content: String) {
        if (streamingDocOffset <= 0) return
        val doc = chatDisplay.styledDocument
        val currentLen = doc.length
        if (currentLen > streamingDocOffset) {
            doc.remove(streamingDocOffset, currentLen - streamingDocOffset)
        }
        val tempPane = JTextPane()
        MarkdownRenderer.render(tempPane, content)
        val srcDoc = tempPane.styledDocument
        for (i in 0 until srcDoc.length) {
            val attrs = srcDoc.getCharacterElement(i).attributes
            val char = srcDoc.getText(i, 1)
            doc.insertString(doc.length, char, attrs)
        }
        streamingDocOffset = 0
        scrollToBottom()
    }

    private fun appendMessage(role: MessageRole, content: String, model: String?) {
        val doc = chatDisplay.styledDocument

        val prefix: String
        val headerAttrs = SimpleAttributeSet()
        StyleConstants.setBold(headerAttrs, true)

        when (role) {
            MessageRole.USER -> {
                StyleConstants.setForeground(headerAttrs, JBColor.foreground())
                prefix = "You"
            }
            MessageRole.ASSISTANT -> {
                        StyleConstants.setForeground(
                            headerAttrs,
                            ASSISTANT_HEADER_COLOR,
                        )
                val modelTag = if (model != null) " [$model]" else ""
                prefix = "AskSage$modelTag"
            }
            MessageRole.ERROR -> {
                StyleConstants.setForeground(headerAttrs, JBColor.RED)
                StyleConstants.setItalic(headerAttrs, true)
                prefix = "Error"
            }
        }

        if (doc.length > 0) {
            doc.insertString(doc.length, "\n\n", null)
        }
        doc.insertString(doc.length, "$prefix:\n", headerAttrs)

        if (role == MessageRole.ASSISTANT) {
            MarkdownRenderer.render(chatDisplay, content)
        } else if (role == MessageRole.ERROR) {
            val errorAttrs = SimpleAttributeSet()
            StyleConstants.setForeground(errorAttrs, JBColor.RED)
            StyleConstants.setItalic(errorAttrs, true)
            doc.insertString(doc.length, content, errorAttrs)
        } else {
            doc.insertString(doc.length, content, null)
        }

        scrollToBottom()
    }

    private fun scrollToBottom() {
        SwingUtilities.invokeLater {
            val scrollBar: JScrollBar = chatScrollPane.verticalScrollBar
            scrollBar.value = scrollBar.maximum
        }
    }

    private fun clearChat() {
        chatSessionService.clearHistory()
        chatDisplay.text = ""
        followUpPanel.isVisible = false
        statusLabel.text = "Ready"
        stopElapsedTimer()
    }

    private fun setLoading(loading: Boolean) {
        sendButton.isEnabled = !loading
        sendButton.text = if (loading) "..." else "Send"
        inputArea.isEnabled = !loading
    }

    private fun startElapsedTimer() {
        streamingStartTime = System.currentTimeMillis()
        elapsedLabel.text = "0s"
        elapsedTimer?.stop()
        elapsedTimer = Timer(1000) {
            val elapsed = (System.currentTimeMillis() - streamingStartTime) / 1000
            elapsedLabel.text = "${elapsed}s"
        }
        elapsedTimer?.start()
    }

    private fun stopElapsedTimer() {
        elapsedTimer?.stop()
        elapsedTimer = null
        elapsedLabel.text = ""
    }

    private fun fetchFollowUpQuestions(token: String, message: String, model: String) {
        if (!settings.showFollowUpQuestions) return
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val followUpResponse = apiClient.getFollowUpQuestions(token, message, model)
                val questions = followUpResponse.questions()
                if (questions.isNotEmpty()) {
                    SwingUtilities.invokeLater {
                        showFollowUpQuestions(questions)
                    }
                }
            } catch (e: AskSageApiException) {
                LOG.debug("Failed to fetch follow-up questions", e)
            }
        }
    }

    private fun showFollowUpQuestions(questions: List<String>) {
        followUpPanel.removeAll()

        val headerLabel = JLabel("Suggested follow-ups:").apply {
            foreground = JBColor.GRAY
            border = BorderFactory.createEmptyBorder(4, 0, 4, 0)
        }
        followUpPanel.add(headerLabel)

        for (question in questions.take(3)) {
            val questionLabel = JLabel("<html><u>$question</u></html>").apply {
                foreground = FOLLOW_UP_LINK_COLOR
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                border = BorderFactory.createEmptyBorder(2, 8, 2, 0)
                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent) {
                        inputArea.text = question
                        sendMessage()
                    }
                })
            }
            followUpPanel.add(questionLabel)
        }

        followUpPanel.isVisible = true
        followUpPanel.revalidate()
        followUpPanel.repaint()
    }

    fun reload() {
        loadRegistries()
    }

    fun syncSelectedModel() {
        if (settings.defaultModel.isNotBlank()) {
            modelSelector.setSelectedModelId(settings.defaultModel)
        }
    }

    private fun loadRegistries() {
        if (!authManager.isConfigured()) return

        ApplicationManager.getApplication().executeOnPooledThread {
            apiClient.updateBaseUrl(settings.baseUrl)

            // Load models
            if (modelRegistry.needsRefresh()) {
                modelRegistry.refreshModels(apiClient)
            }
            val models = modelRegistry.getVisibleModels(settings.includeGovModels)
            SwingUtilities.invokeLater {
                modelSelector.updateModels(models)
                if (settings.defaultModel.isNotBlank()) {
                    modelSelector.setSelectedModelId(settings.defaultModel)
                }
            }

            // Load personas
            if (personaRegistry.needsRefresh()) {
                personaRegistry.refreshPersonas(apiClient)
            }
            val personas = personaRegistry.getPersonas()
            SwingUtilities.invokeLater {
                personaSelector.updatePersonas(personas)
                if (settings.defaultPersona > 0) {
                    personaSelector.setSelectedPersonaId(settings.defaultPersona)
                }
            }

            // Load datasets
            if (datasetRegistry.needsRefresh()) {
                datasetRegistry.refreshDatasets(apiClient)
            }
            val datasets = datasetRegistry.getDatasets()
            SwingUtilities.invokeLater {
                datasetSelector.updateDatasets(datasets)
            }
        }
    }

    private fun copyLastResponse() {
        val lastAssistant = chatSessionService.getMessages()
            .lastOrNull { it.role == MessageRole.ASSISTANT }
        if (lastAssistant != null) {
            val selection = StringSelection(lastAssistant.content)
            Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
            statusLabel.text = "Response copied to clipboard"
        } else {
            statusLabel.text = "No response to copy"
        }
    }

    private fun exportConversation() {
        val messages = chatSessionService.getMessages()
        if (messages.isEmpty()) {
            statusLabel.text = "No conversation to export"
            return
        }

        val fileChooser = JFileChooser().apply {
            dialogTitle = "Export Conversation"
            addChoosableFileFilter(FileNameExtensionFilter("Markdown files (*.md)", "md"))
            addChoosableFileFilter(FileNameExtensionFilter("JSON files (*.json)", "json"))
            fileFilter = FileNameExtensionFilter("Markdown files (*.md)", "md")
            selectedFile = java.io.File("asksage-conversation.md")
        }

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            var file = fileChooser.selectedFile
            val isJson = fileChooser.fileFilter.description.contains("JSON") ||
                file.name.endsWith(".json")

            if (isJson) {
                if (!file.name.endsWith(".json")) {
                    file = java.io.File(file.absolutePath + ".json")
                }
                file.writeText(exportAsJson(messages))
            } else {
                if (!file.name.endsWith(".md")) {
                    file = java.io.File(file.absolutePath + ".md")
                }
                file.writeText(exportAsMarkdown(messages))
            }
            statusLabel.text = "Conversation exported to ${file.name}"
        }
    }

    private fun exportAsMarkdown(messages: List<ChatMessage>): String {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        return buildString {
            appendLine("# AskSage Conversation")
            appendLine("*Exported: ${dateFormat.format(java.util.Date())}*")
            appendLine("*Project: ${project.name}*")
            appendLine()
            for (msg in messages) {
                val ts = dateFormat.format(java.util.Date(msg.timestamp))
                when (msg.role) {
                    MessageRole.USER -> {
                        appendLine("## You *($ts)*")
                        appendLine(msg.content)
                        appendLine()
                    }
                    MessageRole.ASSISTANT -> {
                        val modelTag = if (msg.model != null) " [${msg.model}]" else ""
                        appendLine("## AskSage$modelTag *($ts)*")
                        appendLine(msg.content)
                        appendLine()
                    }
                    MessageRole.ERROR -> {
                        appendLine("> **Error** *($ts)*: ${msg.content}")
                        appendLine()
                    }
                }
            }
        }
    }

    private fun exportAsJson(messages: List<ChatMessage>): String {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        val export = mapOf(
            "project" to project.name,
            "exportedAt" to dateFormat.format(java.util.Date()),
            "messages" to messages.map { msg ->
                mapOf(
                    "role" to msg.role.name.lowercase(),
                    "content" to msg.content,
                    "model" to msg.model,
                    "timestamp" to dateFormat.format(java.util.Date(msg.timestamp)),
                )
            },
        )
        return com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(export)
    }

    companion object {
        private val LOG = logger<ChatPanel>()
        private val ASSISTANT_HEADER_COLOR = JBColor(0x006400, 0x64C864)
        private val FOLLOW_UP_LINK_COLOR = JBColor(0x1E64B4, 0x64A0E6)
    }
}
