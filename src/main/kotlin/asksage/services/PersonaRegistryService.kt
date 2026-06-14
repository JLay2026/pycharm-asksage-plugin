package asksage.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import asksage.api.AskSageApiClient
import asksage.api.AskSageApiException
import asksage.api.auth.AuthManager
import asksage.api.models.PersonaInfo
import asksage.util.NotificationHelper

@Service(Service.Level.APP)
class PersonaRegistryService {
    @Volatile private var personas: List<PersonaInfo> = emptyList()
    @Volatile private var lastFetchTime: Long = 0
    private val lock = Any()
    fun getPersonas(): List<PersonaInfo> = personas
    fun refreshPersonas(apiClient: AskSageApiClient) {
        val authManager = AuthManager.getInstance()
        val token = authManager.getAccessToken(apiClient) ?: return
        try {
            val response = apiClient.getPersonas(token)
            val personaList = response.response
            if (personaList != null) {
                synchronized(lock) {
                    personas = personaList.sortedBy { it.name }
                    lastFetchTime = System.currentTimeMillis()
                }
                LOG.info("Fetched ${personas.size} personas")
            }
        } catch (e: AskSageApiException) {
            LOG.warn("Failed to fetch personas", e)
            NotificationHelper.warn(null, "AskSage", "Failed to refresh personas: ${e.message}")
        }
    }
    fun needsRefresh(): Boolean = personas.isEmpty() || System.currentTimeMillis() - lastFetchTime > REFRESH_INTERVAL_MS
    companion object {
        private val LOG = logger<PersonaRegistryService>()
        private const val REFRESH_INTERVAL_MS = 30 * 60 * 1000L
        fun getInstance(): PersonaRegistryService = ApplicationManager.getApplication().getService(PersonaRegistryService::class.java)
    }
}
