package org.jetbrains.plugins.template.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import org.jetbrains.plugins.template.api.AskSageApiClient
import org.jetbrains.plugins.template.api.AskSageApiException
import org.jetbrains.plugins.template.api.auth.AuthManager
import org.jetbrains.plugins.template.api.models.PersonaInfo

@Service(Service.Level.APP)
class PersonaRegistryService {

    private var personas: List<PersonaInfo> = emptyList()
    private var lastFetchTime: Long = 0

    fun getPersonas(): List<PersonaInfo> = personas

    fun refreshPersonas(apiClient: AskSageApiClient) {
        val authManager = AuthManager.getInstance()
        val token = authManager.getAccessToken(apiClient) ?: return

        try {
            val response = apiClient.getPersonas(token)
            val personaList = response.response
            if (personaList != null) {
                personas = personaList.sortedBy { it.name }
                lastFetchTime = System.currentTimeMillis()
                LOG.info("Fetched ${personas.size} personas")
            }
        } catch (e: AskSageApiException) {
            LOG.warn("Failed to fetch personas", e)
        }
    }

    fun needsRefresh(): Boolean {
        return personas.isEmpty() || System.currentTimeMillis() - lastFetchTime > REFRESH_INTERVAL_MS
    }

    companion object {
        private val LOG = logger<PersonaRegistryService>()
        private const val REFRESH_INTERVAL_MS = 30 * 60 * 1000L

        fun getInstance(): PersonaRegistryService {
            return ApplicationManager.getApplication().getService(PersonaRegistryService::class.java)
        }
    }
}
