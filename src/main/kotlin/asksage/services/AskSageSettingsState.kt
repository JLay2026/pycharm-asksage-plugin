package asksage.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import asksage.api.AskSageEndpoints
import asksage.util.LiveMode

@Service(Service.Level.APP)
@State(
    name = "asksage.services.AskSageSettingsState",
    storages = [Storage("AskSageSettings.xml")]
)
class AskSageSettingsState : PersistentStateComponent<AskSageSettingsState> {
    var baseUrl: String = AskSageEndpoints.DEFAULT_BASE_URL
    var defaultLiveMode: Int = LiveMode.NO_LIVE.value
    var defaultModel: String = ""
    var defaultPersona: Int = 0
    var defaultDataset: String = ""
    var temperature: Double = 0.7
    var reasoningEffort: String = "medium"
    var showFollowUpQuestions: Boolean = true

    override fun getState(): AskSageSettingsState = this
    override fun loadState(state: AskSageSettingsState) { XmlSerializerUtil.copyBean(state, this) }
    fun resetToDefaults() {
        baseUrl = AskSageEndpoints.DEFAULT_BASE_URL
        defaultLiveMode = LiveMode.NO_LIVE.value
        defaultModel = ""
        defaultPersona = 0
        defaultDataset = ""
        temperature = 0.7
        reasoningEffort = "medium"
        showFollowUpQuestions = true
    }
    companion object { fun getInstance(): AskSageSettingsState = ApplicationManager.getApplication().getService(AskSageSettingsState::class.java) }
}
