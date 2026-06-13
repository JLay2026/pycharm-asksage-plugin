package ai.bigbear.pymatic.asksage.ui.toolwindow

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import ai.bigbear.pymatic.asksage.api.AskSageApiClient
import ai.bigbear.pymatic.asksage.api.AskSageApiException
import ai.bigbear.pymatic.asksage.api.auth.AuthManager
import ai.bigbear.pymatic.asksage.api.models.TokenUsageData
import ai.bigbear.pymatic.asksage.services.AskSageSettingsState
import ai.bigbear.pymatic.asksage.util.NotificationHelper
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
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

    private val monthlyLabel = createStatLabel("Monthly Tokens", "—")
    private val dailyLabel = createStatLabel("Daily Tokens", "—")
    private val totalLabel = createStatLabel("Total Tokens", "—")
    private val remainingLabel = createStatLabel("Remaining", "—")
    private val usageBar = UsageBar()

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
            add(JLabel("Token Usage Dashboard").apply {
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
            add(dailyLabel)
            add(Box.createVerticalStrut(8))
            add(totalLabel)
            add(Box.createVerticalStrut(8))
            add(remainingLabel)
            add(Box.createVerticalStrut(16))
            add(usageBar.apply {
                preferredSize = Dimension(0, 40)
                maximumSize = Dimension(Int.MAX_VALUE, 40)
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
                val usage = response.response

                SwingUtilities.invokeLater {
                    updateDisplay(usage)
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

    private fun updateDisplay(usage: TokenUsageData?) {
        val fmt = NumberFormat.getNumberInstance()
        val monthly = usage?.monthlyTokens ?: 0
        val daily = usage?.dailyTokens ?: 0
        val total = usage?.totalTokens ?: 0
        val remaining = usage?.remainingTokens ?: 0

        updateStatLabel(monthlyLabel, "Monthly Tokens", fmt.format(monthly))
        updateStatLabel(dailyLabel, "Daily Tokens", fmt.format(daily))
        updateStatLabel(totalLabel, "Total Tokens", fmt.format(total))
        updateStatLabel(remainingLabel, "Remaining", fmt.format(remaining))

        val usedRatio = if (monthly + remaining > 0) {
            monthly.toDouble() / (monthly + remaining).toDouble()
        } else {
            0.0
        }
        usageBar.setUsage(usedRatio, fmt.format(monthly), fmt.format(monthly + remaining))
    }

    private fun createStatLabel(title: String, value: String): JPanel {
        return JPanel(BorderLayout()).apply {
            maximumSize = Dimension(Int.MAX_VALUE, 36)
            border = BorderFactory.createEmptyBorder(4, 0, 4, 0)
            add(JLabel(title).apply {
                foreground = JBColor.GRAY
            }, BorderLayout.WEST)
            add(JLabel(value).apply {
                font = font.deriveFont(Font.BOLD)
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

    private class UsageBar : JPanel() {
        private var usageRatio: Double = 0.0
        private var usedText: String = "0"
        private var totalText: String = "0"

        fun setUsage(ratio: Double, used: String, total: String) {
            usageRatio = ratio.coerceIn(0.0, 1.0)
            usedText = used
            totalText = total
            repaint()
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2 = g as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            val barHeight = 20
            val barY = (height - barHeight) / 2

            // Background
            g2.color = BAR_BACKGROUND
            g2.fillRoundRect(0, barY, width, barHeight, 8, 8)

            // Usage fill
            val fillWidth = (width * usageRatio).toInt()
            if (fillWidth > 0) {
                val fillColor = when {
                    usageRatio < 0.5 -> BAR_FILL_GREEN
                    usageRatio < 0.8 -> BAR_FILL_YELLOW
                    else -> BAR_FILL_RED
                }
                g2.color = fillColor
                g2.fillRoundRect(0, barY, fillWidth, barHeight, 8, 8)
            }

            // Label
            g2.color = JBColor.foreground()
            g2.font = g2.font.deriveFont(11f)
            val label = "$usedText / $totalText tokens"
            val labelWidth = g2.fontMetrics.stringWidth(label)
            g2.drawString(label, (width - labelWidth) / 2, barY + barHeight + 14)
        }
    }

    companion object {
        private val LOG = logger<TokenUsagePanel>()
        private val BAR_BACKGROUND = JBColor(0xE6E6E6, 0x3C3C3C)
        private val BAR_FILL_GREEN = JBColor(0x4CAF50, 0x4CAF50)
        private val BAR_FILL_YELLOW = JBColor(0xFFC107, 0xFFC107)
        private val BAR_FILL_RED = JBColor(0xF44336, 0xF44336)
    }
}
