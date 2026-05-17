package org.jetbrains.plugins.template.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import org.jetbrains.plugins.template.api.AskSageApiClient
import org.jetbrains.plugins.template.api.AskSageApiException
import org.jetbrains.plugins.template.api.auth.AuthManager

@Service(Service.Level.APP)
class DatasetRegistryService {

    @Volatile
    private var datasets: List<String> = emptyList()

    @Volatile
    private var lastFetchTime: Long = 0

    private val lock = Any()

    fun getDatasets(): List<String> = datasets

    fun refreshDatasets(apiClient: AskSageApiClient) {
        val authManager = AuthManager.getInstance()
        val token = authManager.getAccessToken(apiClient) ?: return

        try {
            val response = apiClient.getDatasets(token)
            val datasetList = response.response
            if (datasetList != null) {
                synchronized(lock) {
                    datasets = datasetList.sorted()
                    lastFetchTime = System.currentTimeMillis()
                }
                LOG.info("Fetched ${datasets.size} datasets")
            }
        } catch (e: AskSageApiException) {
            LOG.warn("Failed to fetch datasets", e)
        }
    }

    fun needsRefresh(): Boolean {
        return datasets.isEmpty() || System.currentTimeMillis() - lastFetchTime > REFRESH_INTERVAL_MS
    }

    companion object {
        private val LOG = logger<DatasetRegistryService>()
        private const val REFRESH_INTERVAL_MS = 30 * 60 * 1000L

        fun getInstance(): DatasetRegistryService {
            return ApplicationManager.getApplication().getService(DatasetRegistryService::class.java)
        }
    }
}
