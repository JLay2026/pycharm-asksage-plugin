package asksage.ui.settings

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import asksage.api.AskSageApiClient
import asksage.api.auth.AuthManager
import asksage.services.AskSageRefreshListener
import asksage.services.AskSageRefreshTopic
import asksage.services.AskSageSettingsState
import asksage.services.DatasetRegistryService
import asksage.services.ModelRegistryService
import asksage.services.PersonaRegistryService
import asksage.util.LiveMode
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SwingUtilities
import javax.swing.SpinnerNumberModel

class AskSageSettingsComponent {

    val panel: JPanel
    private val emailField = JBTextField()
    private val apiKeyField = JBPasswordField()
    private val baseUrlField = JBTextField()
    private val defaultLiveModeCombo = JComboBox(LiveMode.entries.toTypedArray())
    private val temperatureSpinner = JSpinner(SpinnerNumberModel(0.7, 0.0, 2.0, 0.1))
    private val reasoningEffortCombo = JComboBox(arrayOf("low", "medium", "high"))
    private val showFollowUpsCheckbox = JCheckBox("Show suggested follow-up questions")
    private val includeGovModelsCheckbox = JCheckBox("Include government (gov) models")

    private val clearCredentialsButton = JButton("Clear Credentials / Sign Out")
    private val resetSettingsButton = JButton("Reset Settings")

    private val testConnectionButton = JButton("Test Connection")
    private val testModelsButton = JButton("Test Model Discovery")
    private val testStatusLabel = JBLabel(" ")
    private val connectedColor = JBColor(0x2E7D32, 0x6CC04A)
    private val errorColor = JBColor(0xC62828, 0xFF6B68)

    init {
        defaultLiveModeCombo.renderer = LiveModeRenderer()

        val actionsPanel = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
            add(clearCredentialsButton)
            add(resetSettingsButton)
        }

        val testButtonsPanel = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
            add(testConnectionButton)
            add(testModelsButton)
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
            .addComponent(testButtonsPanel)
            .addComponent(testStatusLabel)
            .addSeparator()
            .addLabeledComponent(JBLabel("Default Live Mode:"), defaultLiveModeCombo, 1, false)
            .addLabeledComponent(JBLabel("Temperature:"), temperatureSpinner, 1, false)
            .addLabeledComponent(JBLabel("Reasoning Effort:"), reasoningEffortCombo, 1, false)
            .addComponent(showFollowUpsCheckbox)
            .addComponent(includeGovModelsCheckbox)
            .addSeparator()
            .addComponent(actionsPanel)
            .addComponent(persistenceNote)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        clearCredentialsButton.addActionListener { onClearCredentials() }
        resetSettingsButton.addActionListener { onResetSettings() }
        testConnectionButton.addActionListener { onTestConnection() }
        testModelsButton.addActionListener { onTestModels() }

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
            Messages.showInfoMessage(panel, "Credentials cleared.", "AskSage")
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
            Messages.showInfoMessage(panel, "Settings reset to defaults.", "AskSage")
        }
    }

    private fun credsForTest(): Pair<String, String>? {
        val email = emailField.text.trim()
        val apiKey = String(apiKeyField.password).ifBlank { AuthManager.getInstance().getApiKey() ?: "" }
        if (email.isBlank() || apiKey.isBlank()) {
            setTestStatus("Enter email and API key first", JBColor.GRAY, AllIcons.General.BalloonWarning)
            return null
        }
        return email to apiKey
    }

    private fun setTestStatus(text: String, color: java.awt.Color, icon: javax.swing.Icon?) {
        testStatusLabel.text = text
        testStatusLabel.foreground = color
        testStatusLabel.icon = icon
    }

    private fun setTestsEnabled(enabled: Boolean) {
        testConnectionButton.isEnabled = enabled
        testModelsButton.isEnabled = enabled
    }

    /** Persist the tested credentials and live-refresh registries + open panels (no IDE restart). */
    private fun persistAndRefresh(email: String, apiKey: String, baseUrl: String) {
        val auth = AuthManager.getInstance()
        auth.setEmail(email)
        auth.setApiKey(apiKey)
        AskSageSettingsState.getInstance().baseUrl = baseUrl
        val app = ApplicationManager.getApplication()
        app.executeOnPooledThread {
            val client = AskSageApiClient(baseUrl)
            ModelRegistryService.getInstance().refreshModels(client)
            PersonaRegistryService.getInstance().refreshPersonas(client)
            DatasetRegistryService.getInstance().refreshDatasets(client)
            app.messageBus.syncPublisher(AskSageRefreshTopic.TOPIC).onRefreshRequested()
        }
    }

    private fun onTestConnection() {
        val (email, apiKey) = credsForTest() ?: return
        val baseUrl = baseUrlField.text.trim()
        setTestStatus("Testing connection…", JBColor.GRAY, null)
        setTestsEnabled(false)
        ApplicationManager.getApplication().executeOnPooledThread {
            var ok = false
            var message: String
            try {
                val token = AskSageApiClient(baseUrl).getToken(email, apiKey).resolveToken()
                ok = !token.isNullOrBlank()
                message = if (ok) "Connected as $email" else "Login failed — check email/API key"
            } catch (e: Exception) {
                message = e.message ?: "Connection failed"
            }
            val success = ok
            val msg = message
            SwingUtilities.invokeLater {
                if (success) {
                    setTestStatus("✓ $msg", connectedColor, AllIcons.General.InspectionsOK)
                    persistAndRefresh(email, apiKey, baseUrl)
                } else {
                    setTestStatus("✗ $msg", errorColor, AllIcons.General.BalloonError)
                }
                setTestsEnabled(true)
            }
        }
    }

    private fun onTestModels() {
        val (email, apiKey) = credsForTest() ?: return
        val baseUrl = baseUrlField.text.trim()
        setTestStatus("Discovering models…", JBColor.GRAY, null)
        setTestsEnabled(false)
        ApplicationManager.getApplication().executeOnPooledThread {
            var ok = false
            var message: String
            try {
                val token = AskSageApiClient(baseUrl).getToken(email, apiKey).resolveToken()
                if (token.isNullOrBlank()) {
                    message = "Login failed — cannot fetch models"
                } else {
                    val models = AskSageApiClient(baseUrl).getModels(token).resolveModels()
                    ok = models.isNotEmpty()
                    message = if (ok) "${models.size} models available" else "No models returned"
                }
            } catch (e: Exception) {
                message = e.message ?: "Model discovery failed"
            }
            val success = ok
            val msg = message
            SwingUtilities.invokeLater {
                if (success) {
                    setTestStatus("✓ $msg", connectedColor, AllIcons.General.InspectionsOK)
                    persistAndRefresh(email, apiKey, baseUrl)
                } else {
                    setTestStatus("✗ $msg", errorColor, AllIcons.General.BalloonError)
                }
                setTestsEnabled(true)
            }
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
            reasoningEffortCombo.selectedItem != settings.reasoningEffort ||
            showFollowUpsCheckbox.isSelected != settings.showFollowUpQuestions ||
            includeGovModelsCheckbox.isSelected != settings.includeGovModels
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
        settings.showFollowUpQuestions = showFollowUpsCheckbox.isSelected
        settings.includeGovModels = includeGovModelsCheckbox.isSelected
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
        showFollowUpsCheckbox.isSelected = settings.showFollowUpQuestions
        includeGovModelsCheckbox.isSelected = settings.includeGovModels
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
