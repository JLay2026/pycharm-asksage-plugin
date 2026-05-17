package org.jetbrains.plugins.template.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import org.jetbrains.plugins.template.api.AskSageEndpoints
import org.jetbrains.plugins.template.util.LiveMode

@Service(Service.Level.APP)
@State(
    name = "org.jetbrains.plugins.template.services.AskSageSettingsState",
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

    companion object {
        fun getInstance(): AskSageSettingsState {
            return ApplicationManager.getApplication().getService(AskSageSettingsState::class.java)
        }
    }
}
