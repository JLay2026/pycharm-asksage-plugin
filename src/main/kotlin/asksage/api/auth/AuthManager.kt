package asksage.api.auth

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import asksage.api.AskSageApiClient
import asksage.api.AskSageApiException
import asksage.util.NotificationHelper

@Service(Service.Level.APP)
class AuthManager {
    private var cachedToken: String? = null
    private var tokenTimestamp: Long = 0

    fun getApiKey(): String? {
        val attributes = createCredentialAttributes(API_KEY_KEY)
        return PasswordSafe.instance.getPassword(attributes)
    }

    fun setApiKey(apiKey: String) {
        val attributes = createCredentialAttributes(API_KEY_KEY)
        PasswordSafe.instance.set(attributes, Credentials("", apiKey))
        cachedToken = null
    }

    fun getEmail(): String? {
        val attributes = createCredentialAttributes(EMAIL_KEY)
        return PasswordSafe.instance.getPassword(attributes)
    }

    fun setEmail(email: String) {
        val attributes = createCredentialAttributes(EMAIL_KEY)
        PasswordSafe.instance.set(attributes, Credentials("", email))
        cachedToken = null
    }

    fun getAccessToken(apiClient: AskSageApiClient): String? {
        val apiKey = getApiKey()
        val email = getEmail()
        if (apiKey.isNullOrBlank() || email.isNullOrBlank()) {
            LOG.warn("API key or email not configured")
            return null
        }
        if (cachedToken != null && System.currentTimeMillis() - tokenTimestamp < TOKEN_TTL_MS) {
            return cachedToken
        }
        return try {
            val response = apiClient.getToken(email, apiKey)
            val token = response.resolveToken()
            if (token != null) {
                cachedToken = token
                tokenTimestamp = System.currentTimeMillis()
                cachedToken
            } else {
                LOG.warn("Token exchange returned no token: status=${response.status} message=${response.message}")
                NotificationHelper.warn(null, "AskSage Auth", "Token exchange failed, using API key directly")
                apiKey
            }
        } catch (e: AskSageApiException) {
            LOG.warn("Token exchange failed, falling back to API key", e)
            NotificationHelper.warn(null, "AskSage Auth", "Token exchange failed: ${e.message}. Using API key directly.")
            apiKey
        }
    }

    fun isConfigured(): Boolean {
        return !getApiKey().isNullOrBlank() && !getEmail().isNullOrBlank()
    }

    fun clearCredentials() {
        val apiKeyAttr = createCredentialAttributes(API_KEY_KEY)
        val emailAttr = createCredentialAttributes(EMAIL_KEY)
        PasswordSafe.instance.set(apiKeyAttr, null)
        PasswordSafe.instance.set(emailAttr, null)
        cachedToken = null
        tokenTimestamp = 0
    }

    private fun createCredentialAttributes(key: String): CredentialAttributes {
        return CredentialAttributes(generateServiceName(SUBSYSTEM, key))
    }

    companion object {
        private val LOG = logger<AuthManager>()
        private const val SUBSYSTEM = "AskSage"
        private const val API_KEY_KEY = "apiKey"
        private const val EMAIL_KEY = "email"
        private const val TOKEN_TTL_MS = 23 * 60 * 60 * 1000L
        fun getInstance(): AuthManager = ApplicationManager.getApplication().getService(AuthManager::class.java)
    }
}
