package ai.bigbear.pymatic.asksage.ui.toolwindow

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import ai.bigbear.pymatic.asksage.api.AskSageApiClient
import ai.bigbear.pymatic.asksage.api.AskSageApiException
import ai.bigbear.pymatic.asksage.api.auth.AuthManager
import ai.bigbear.pymatic.asksage.services.AskSageSettingsState
import ai.bigbear.pymatic.asksage.util.NotificationHelper
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import java.text.NumberFormat
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.ScrollPaneConstants
import javax.swing.SwingUtilities

class TokenUsagePanel(private val project: Project) : JPanel(BorderLayout()) {

    private val settings = AskSageSettingsState.getInstance()
    private val authManager = AuthManager.getInstance()
    private val apiClient = AskSageApiClient(settings.baseUrl)

    private val monthlyTitleLabel = JLabel("Monthly Tokens (this app)").apply {
        foreground = JBColor.GRAY
        alignmentX = LEFT_ALIGNMENT
    }
    private val monthlyValueLabel = JLabel("—").apply {
        font = font.deriveFont(Font.BOLD, 22f)
        alignmentX = LEFT_ALIGNMENT
    }

    private val descriptionText = wrappingText(
        "Monthly token count for the current application, from the Ask Sage API.",
    )

    private val refreshButton = JButton("Refresh").apply {
        preferredSize = Dimension(90, 28)
    }

    private val statusText = wrappingText("Click Refresh to load token usage")

    init {
        setupUI()
        setupActions()
        loadUsage()
    }

    private fun setupUI() {
        val toolbar = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            border = BorderFactory.createEmptyBorder(8, 8, 4, 8)
            add(JLabel("Token Usage").apply {
                font = font.deriveFont(Font.BOLD, 16f)
            })
            add(Box.createHorizontalGlue())
            add(refreshButton)
        }

        val statsPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(8, 16, 8, 16)
            add(monthlyTitleLabel)
            add(Box.createVerticalStrut(2))
            add(monthlyValueLabel)
            add(Box.createVerticalStrut(10))
            add(descriptionText)
        }

        // Never scroll horizontally so the content wraps to the panel width
        // (the token count is visible without scrolling).
        val scroll = JBScrollPane(statsPanel).apply {
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            border = BorderFactory.createEmptyBorder()
        }

        val statusPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, JBColor.border()),
                BorderFactory.createEmptyBorder(4, 8, 4, 8),
            )
            add(statusText, BorderLayout.CENTER)
        }

        add(toolbar, BorderLayout.NORTH)
        add(scroll, BorderLayout.CENTER)
        add(statusPanel, BorderLayout.SOUTH)
    }

    private fun setupActions() {
        refreshButton.addActionListener { loadUsage() }
    }

    private fun loadUsage() {
        if (!authManager.isConfigured()) {
            statusText.text = "Please configure API credentials in Settings > Tools > Pymatic AskSage"
            return
        }

        refreshButton.isEnabled = false
        statusText.text = "Loading token usage..."

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                apiClient.updateBaseUrl(settings.baseUrl)
                val token = authManager.getAccessToken(apiClient)
                    ?: throw AskSageApiException("Authentication failed")

                val response = apiClient.countMonthlyTokens(token)
                val monthly = response.count()

                SwingUtilities.invokeLater {
                    val fmt = NumberFormat.getNumberInstance()
                    monthlyValueLabel.text = monthly?.let { fmt.format(it) } ?: "—"
                    refreshButton.isEnabled = true
                    statusText.text = "Last updated: now"
                }
            } catch (e: AskSageApiException) {
                LOG.warn("Failed to load token usage", e)
                NotificationHelper.warn(project, "AskSage", "Failed to load token usage: ${e.message}")
                SwingUtilities.invokeLater {
                    refreshButton.isEnabled = true
                    statusText.text = "Error: ${e.message}"
                }
            }
        }
    }

    /** A label-styled, read-only text component that wraps long lines. */
    private fun wrappingText(text: String): JTextArea {
        return JTextArea(text).apply {
            isEditable = false
            isFocusable = false
            lineWrap = true
            wrapStyleWord = true
            isOpaque = false
            border = null
            foreground = JBColor.GRAY
            font = JLabel().font
            alignmentX = Component.LEFT_ALIGNMENT
            maximumSize = Dimension(Int.MAX_VALUE, Int.MAX_VALUE)
        }
    }

    companion object {
        private val LOG = logger<TokenUsagePanel>()
    }
}
