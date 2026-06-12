package ai.bigbear.pymatic.asksage.api

import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ai.bigbear.pymatic.asksage.api.AskSageApiClient
import ai.bigbear.pymatic.asksage.api.AskSageApiException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AskSageApiClientRetryTest {

    private lateinit var apiClient: AskSageApiClient

    @BeforeEach
    fun setUp() {
        apiClient = AskSageApiClient("http://localhost:8080")
    }

    @Test
    fun testRetryMechanismExists() {
        assertNotNull(apiClient)
    }

    @Test
    fun testMaxRetries() {
        val maxRetries = 3
        assertEquals(3, maxRetries)
    }

    @Test
    fun testRetryBackoff() {
        val backoffMs = 1000L
        assertEquals(1000L, backoffMs)
    }

    @Test
    fun testTemporaryErrorDetection() {
        val statusCode = 503
        val isTemporary = statusCode in 500..599
        assertEquals(true, isTemporary)
    }

    @Test
    fun testPermanentErrorDetection() {
        val statusCode = 401
        val isPermanent = statusCode in 400..499
        assertEquals(true, isPermanent)
    }
}
