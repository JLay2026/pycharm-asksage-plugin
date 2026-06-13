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
import java.awt.Dimension
import java.awt.Font
import java.text.NumberFormat
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities

class TokenUsagePanel(private val project: Project) : JPanel(BorderLayout()) {

    private val settings = AskSageSettingsState.getInstance()
    private val authManager = AuthManager.getInstance()
    private val apiClient = AskSageApiClient(settings.baseUrl)

    private val monthlyLabel = createStatLabel("Monthly Tokens (this app)", "—")

    private val refreshButton = JButton("Refresh").apply {
        preferredSize = Dimension(90, 28)
    }

    private val statusLabel = JLabel("Click Refresh to load token usage").apply {
        foreground = JBColor.GRAY
    }

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
            add(monthlyLabel)
            add(Box.createVerticalStrut(8))
            add(JLabel("Monthly token count for the current application, from the Ask Sage API.").apply {
                foreground = JBColor.GRAY
            })
        }

        val statusPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, JBColor.border()),
                BorderFactory.createEmptyBorder(4, 8, 4, 8),
            )
            add(statusLabel, BorderLayout.WEST)
        }

        add(toolbar, BorderLayout.NORTH)
        add(JBScrollPane(statsPanel), BorderLayout.CENTER)
        add(statusPanel, BorderLayout.SOUTH)
    }

    private fun setupActions() {
        refreshButton.addActionListener { loadUsage() }
    }

    private fun loadUsage() {
        if (!authManager.isConfigured()) {
            statusLabel.text = "Please configure API credentials in Settings > Tools > Pymatic AskSage"
            return
        }

        refreshButton.isEnabled = false
        statusLabel.text = "Loading token usage..."

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                apiClient.updateBaseUrl(settings.baseUrl)
                val token = authManager.getAccessToken(apiClient)
                    ?: throw AskSageApiException("Authentication failed")

                val response = apiClient.countMonthlyTokens(token)
                val monthly = response.count()

                SwingUtilities.invokeLater {
                    val fmt = NumberFormat.getNumberInstance()
                    updateStatLabel(monthlyLabel, "Monthly Tokens (this app)", monthly?.let { fmt.format(it) } ?: "—")
                    refreshButton.isEnabled = true
                    statusLabel.text = "Last updated: now"
                }
            } catch (e: AskSageApiException) {
                LOG.warn("Failed to load token usage", e)
                NotificationHelper.warn(project, "AskSage", "Failed to load token usage: ${e.message}")
                SwingUtilities.invokeLater {
                    refreshButton.isEnabled = true
                    statusLabel.text = "Error: ${e.message}"
                }
            }
        }
    }

    private fun createStatLabel(title: String, value: String): JPanel {
        return JPanel(BorderLayout()).apply {
            maximumSize = Dimension(Int.MAX_VALUE, 36)
            border = BorderFactory.createEmptyBorder(4, 0, 4, 0)
            add(JLabel(title).apply {
                foreground = JBColor.GRAY
            }, BorderLayout.WEST)
            add(JLabel(value).apply {
                font = font.deriveFont(Font.BOLD, 18f)
                name = "value"
            }, BorderLayout.EAST)
        }
    }

    private fun updateStatLabel(panel: JPanel, title: String, value: String) {
        for (comp in panel.components) {
            if (comp is JLabel && comp.name == "value") {
                comp.text = value
            }
        }
    }

    companion object {
        private val LOG = logger<TokenUsagePanel>()
    }
}
