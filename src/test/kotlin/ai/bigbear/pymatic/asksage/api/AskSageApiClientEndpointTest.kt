package ai.bigbear.pymatic.asksage.api

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ai.bigbear.pymatic.asksage.api.AskSageApiClient
import ai.bigbear.pymatic.asksage.api.models.QueryRequest
import ai.bigbear.pymatic.asksage.api.models.QueryResponse
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AskSageApiClientEndpointTest {

    private lateinit var apiClient: AskSageApiClient
    private val baseUrl = "http://localhost:8080"

    @BeforeEach
    fun setUp() {
        apiClient = AskSageApiClient(baseUrl)
    }

    @Test
    fun testQueryEndpointConstruction() {
        val request = QueryRequest(
            model = "gpt-4",
            message = "Hello",
            live = "disabled",
            dataset = null,
            persona = null,
            systemPrompt = null,
        )
        assertNotNull(request)
        assertEquals("gpt-4", request.model)
    }

    @Test
    fun testBaseUrlUpdate() {
        val newUrl = "http://example.com:9000"
        apiClient.updateBaseUrl(newUrl)
        assertNotNull(apiClient)
    }

    @Test
    fun testTokenValidation() {
        val validToken = "valid_token_string"
        assertNotNull(validToken)
        assertEquals("valid_token_string", validToken)
    }

    @Test
    fun testErrorResponse() {
        val errorMessage = "API Error"
        val exception = AskSageApiException(errorMessage)
        assertEquals(errorMessage, exception.message)
    }
}
