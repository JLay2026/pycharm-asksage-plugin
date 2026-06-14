package ai.bigbear.pymatic.asksage.ui.settings

import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import ai.bigbear.pymatic.asksage.api.auth.AuthManager
import ai.bigbear.pymatic.asksage.services.AskSageSettingsState
import ai.bigbear.pymatic.asksage.util.LiveMode
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

class AskSageSettingsComponent {

    val panel: JPanel
    private val emailField = JBTextField()
    private val apiKeyField = JBPasswordField()
    private val baseUrlField = JBTextField()
    private val defaultLiveModeCombo = JComboBox(LiveMode.entries.toTypedArray())
    private val temperatureSpinner = JSpinner(SpinnerNumberModel(0.7, 0.0, 2.0, 0.1))
    private val reasoningEffortCombo = JComboBox(arrayOf("low", "medium", "high"))

    private val clearCredentialsButton = JButton("Clear Credentials / Sign Out")
    private val resetSettingsButton = JButton("Reset Settings")

    init {
        defaultLiveModeCombo.renderer = LiveModeRenderer()

        val actionsPanel = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
            add(clearCredentialsButton)
            add(resetSettingsButton)
        }

        val persistenceNote = JBLabel(
            "<html>Your API key and email are stored in the IDE's secure storage and are " +
                "<b>not removed when the plugin is uninstalled</b>. Use &ldquo;Clear Credentials&rdquo; to remove them.</html>",
        ).apply {
            foreground = JBColor.GRAY
        }

        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Email:"), emailField, 1, false)
            .addLabeledComponent(JBLabel("API Key:"), apiKeyField, 1, false)
            .addLabeledComponent(JBLabel("Base URL:"), baseUrlField, 1, false)
            .addSeparator()
            .addLabeledComponent(JBLabel("Default Live Mode:"), defaultLiveModeCombo, 1, false)
            .addLabeledComponent(JBLabel("Temperature:"), temperatureSpinner, 1, false)
            .addLabeledComponent(JBLabel("Reasoning Effort:"), reasoningEffortCombo, 1, false)
            .addSeparator()
            .addComponent(actionsPanel)
            .addComponent(persistenceNote)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        clearCredentialsButton.addActionListener { onClearCredentials() }
        resetSettingsButton.addActionListener { onResetSettings() }

        reset()
    }

    private fun onClearCredentials() {
        val choice = Messages.showYesNoDialog(
            panel,
            "Remove the stored API key and email from this IDE's secure storage? " +
                "You'll need to re-enter them to use the plugin again.",
            "Clear Credentials",
            Messages.getQuestionIcon(),
        )
        if (choice == Messages.YES) {
            AuthManager.getInstance().clearCredentials()
            emailField.text = ""
            apiKeyField.text = ""
            Messages.showInfoMessage(panel, "Credentials cleared.", "Pymatic AskSage")
        }
    }

    private fun onResetSettings() {
        val choice = Messages.showYesNoDialog(
            panel,
            "Reset the server URL and preferences to their defaults? Your credentials are not affected.",
            "Reset Settings",
            Messages.getQuestionIcon(),
        )
        if (choice == Messages.YES) {
            AskSageSettingsState.getInstance().resetToDefaults()
            reset()
            Messages.showInfoMessage(panel, "Settings reset to defaults.", "Pymatic AskSage")
        }
    }

    fun isModified(): Boolean {
        val authManager = AuthManager.getInstance()
        val settings = AskSageSettingsState.getInstance()

        return emailField.text != (authManager.getEmail() ?: "") ||
            String(apiKeyField.password) != (authManager.getApiKey() ?: "") ||
            baseUrlField.text != settings.baseUrl ||
            (defaultLiveModeCombo.selectedItem as LiveMode).value != settings.defaultLiveMode ||
            (temperatureSpinner.value as Double) != settings.temperature ||
            reasoningEffortCombo.selectedItem != settings.reasoningEffort
    }

    fun apply() {
        val authManager = AuthManager.getInstance()
        val settings = AskSageSettingsState.getInstance()

        authManager.setEmail(emailField.text)
        val apiKey = String(apiKeyField.password)
        if (apiKey.isNotBlank()) {
            authManager.setApiKey(apiKey)
        }

        settings.baseUrl = baseUrlField.text
        settings.defaultLiveMode = (defaultLiveModeCombo.selectedItem as LiveMode).value
        settings.temperature = temperatureSpinner.value as Double
        settings.reasoningEffort = reasoningEffortCombo.selectedItem as String
    }

    fun reset() {
        val authManager = AuthManager.getInstance()
        val settings = AskSageSettingsState.getInstance()

        emailField.text = authManager.getEmail() ?: ""
        apiKeyField.text = authManager.getApiKey() ?: ""
        baseUrlField.text = settings.baseUrl
        defaultLiveModeCombo.selectedItem = LiveMode.fromValue(settings.defaultLiveMode)
        temperatureSpinner.value = settings.temperature
        reasoningEffortCombo.selectedItem = settings.reasoningEffort
    }

    private class LiveModeRenderer : javax.swing.DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: javax.swing.JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean,
        ): java.awt.Component {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            if (value is LiveMode) {
                text = "${value.displayName} - ${value.description}"
            }
            return this
        }
    }
}
