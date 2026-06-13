package ai.bigbear.pymatic.asksage.ui.settings

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import ai.bigbear.pymatic.asksage.api.auth.AuthManager
import ai.bigbear.pymatic.asksage.services.AskSageSettingsState
import ai.bigbear.pymatic.asksage.util.LiveMode
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

    init {
        defaultLiveModeCombo.renderer = LiveModeRenderer()

        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Email:"), emailField, 1, false)
            .addLabeledComponent(JBLabel("API Key:"), apiKeyField, 1, false)
            .addLabeledComponent(JBLabel("Base URL:"), baseUrlField, 1, false)
            .addSeparator()
            .addLabeledComponent(JBLabel("Default Live Mode:"), defaultLiveModeCombo, 1, false)
            .addLabeledComponent(JBLabel("Temperature:"), temperatureSpinner, 1, false)
            .addLabeledComponent(JBLabel("Reasoning Effort:"), reasoningEffortCombo, 1, false)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        reset()
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
