package ai.bigbear.pymatic.asksage.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import ai.bigbear.pymatic.asksage.api.AskSageApiClient
import ai.bigbear.pymatic.asksage.api.AskSageApiException
import ai.bigbear.pymatic.asksage.api.auth.AuthManager
import ai.bigbear.pymatic.asksage.api.models.ModelInfo
import ai.bigbear.pymatic.asksage.util.NotificationHelper

@Service(Service.Level.APP)
class ModelRegistryService {

    @Volatile
    private var models: List<ModelInfo> = emptyList()

    @Volatile
    private var lastFetchTime: Long = 0

    private val lock = Any()

    fun getModels(): List<ModelInfo> = models

    fun getModelsByProvider(): Map<String, List<ModelInfo>> {
        return models.groupBy { it.ownedBy ?: "Unknown" }
    }

    fun refreshModels(apiClient: AskSageApiClient) {
        val authManager = AuthManager.getInstance()
        val token = authManager.getAccessToken(apiClient) ?: return

        try {
            val response = apiClient.getModels(token)
            val modelList = response.response?.data
            if (modelList != null) {
                synchronized(lock) {
                    models = modelList.sortedBy { it.id }
                    lastFetchTime = System.currentTimeMillis()
                }
                LOG.info("Fetched ${models.size} models")
            }
        } catch (e: AskSageApiException) {
            LOG.warn("Failed to fetch models", e)
            NotificationHelper.warn(null, "AskSage", "Failed to refresh models: ${e.message}")
        }
    }

    fun needsRefresh(): Boolean {
        return models.isEmpty() || System.currentTimeMillis() - lastFetchTime > REFRESH_INTERVAL_MS
    }

    companion object {
        private val LOG = logger<ModelRegistryService>()
        private const val REFRESH_INTERVAL_MS = 30 * 60 * 1000L // 30 minutes

        fun getInstance(): ModelRegistryService {
            return ApplicationManager.getApplication().getService(ModelRegistryService::class.java)
        }
    }
}
