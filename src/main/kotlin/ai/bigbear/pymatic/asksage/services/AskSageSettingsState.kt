package ai.bigbear.pymatic.asksage.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import ai.bigbear.pymatic.asksage.api.AskSageEndpoints
import ai.bigbear.pymatic.asksage.util.LiveMode

@Service(Service.Level.APP)
@State(
    name = "ai.bigbear.pymatic.asksage.services.AskSageSettingsState",
    storages = [Storage("PymaticAskSageSettings.xml")]
)
class AskSageSettingsState : PersistentStateComponent<AskSageSettingsState> {

    var baseUrl: String = AskSageEndpoints.DEFAULT_BASE_URL
    var defaultLiveMode: Int = LiveMode.NO_LIVE.value
    var defaultModel: String = ""
    var defaultPersona: Int = 0
    var defaultDataset: String = ""
    var temperature: Double = 0.7
    var reasoningEffort: String = "medium"

    override fun getState(): AskSageSettingsState = this

    override fun loadState(state: AskSageSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    /** Restore all settings (server URL and preferences) to their defaults. Does not touch credentials. */
    fun resetToDefaults() {
        baseUrl = AskSageEndpoints.DEFAULT_BASE_URL
        defaultLiveMode = LiveMode.NO_LIVE.value
        defaultModel = ""
        defaultPersona = 0
        defaultDataset = ""
        temperature = 0.7
        reasoningEffort = "medium"
    }

    companion object {
        fun getInstance(): AskSageSettingsState {
            return ApplicationManager.getApplication().getService(AskSageSettingsState::class.java)
        }
    }
}
