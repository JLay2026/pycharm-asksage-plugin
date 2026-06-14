package asksage.ui.settings

import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

class AskSageSettingsConfigurable : Configurable {

    private var settingsComponent: AskSageSettingsComponent? = null

    override fun getDisplayName(): String = "Pymatic AskSage"

    override fun createComponent(): JComponent {
        settingsComponent = AskSageSettingsComponent()
        return settingsComponent!!.panel
    }

    override fun isModified(): Boolean {
        return settingsComponent?.isModified() ?: false
    }

    override fun apply() {
        settingsComponent?.apply()
    }

    override fun reset() {
        settingsComponent?.reset()
    }

    override fun disposeUIResources() {
        settingsComponent = null
    }
}
