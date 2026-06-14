package asksage.api

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import asksage.api.models.ModelsResponse
import java.io.IOException
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpClient.Version
import java.net.http.HttpResponse
import java.net.URI
import java.util.Optional
import javax.net.ssl.SSLSession

class AskSageApiClientRetryTest : BasePlatformTestCase() {

    private fun createMockResponse(statusCode: Int, body: String): HttpResponse<String> {
        return object : HttpResponse<String> {
            override fun statusCode(): Int = statusCode
            override fun body(): String = body
            override fun headers(): HttpHeaders = HttpHeaders.of(emptyMap()) { _, _ -> true }
            override fun request(): HttpRequest = HttpRequest.newBuilder().uri(URI.create("http://test")).build()
            override fun previousResponse(): Optional<HttpResponse<String>> = Optional.empty()
            override fun sslSession(): Optional<SSLSession> = Optional.empty()
            override fun uri(): URI = URI.create("http://test")
            override fun version(): Version = Version.HTTP_1_1
        }
    }

    fun testSuccessOnFirstAttempt() {
        val json = """${""}{"data":[],"status":200}"""
        val client = AskSageApiClient("http://localhost")
        var callCount = 0
        client.requestExecutor = HttpRequestExecutor {
            callCount++
            createMockResponse(200, json)
        }
        val result: ModelsResponse = client.getModels("test-token")
        assertEquals(1, callCount)
        assertNotNull(result)
    }

    fun testRetryOn500ThenSuccess() {
        val json = """${""}{"data":[],"status":200}"""
        val client = AskSageApiClient("http://localhost")
        var callCount = 0
        client.requestExecutor = HttpRequestExecutor {
            callCount++
            if (callCount == 1) {
                createMockResponse(500, "Internal Server Error")
            } else {
                createMockResponse(200, json)
            }
        }
        val result: ModelsResponse = client.getModels("test-token")
        assertEquals(2, callCount)
        assertNotNull(result)
    }

    fun testRetryOn429ThenSuccess() {
        val json = """${""}{"data":[],"status":200}"""
        val client = AskSageApiClient("http://localhost")
        var callCount = 0
        client.requestExecutor = HttpRequestExecutor {
            callCount++
            if (callCount == 1) {
                createMockResponse(429, "Too Many Requests")
            } else {
                createMockResponse(200, json)
            }
        }
        val result: ModelsResponse = client.getModels("test-token")
        assertEquals(2, callCount)
        assertNotNull(result)
    }

    fun testMaxRetriesExhausted() {
        val client = AskSageApiClient("http://localhost")
        var callCount = 0
        client.requestExecutor = HttpRequestExecutor {
            callCount++
            createMockResponse(500, "Internal Server Error")
        }
        try {
            client.getModels("test-token")
            fail("Expected AskSageApiException")
        } catch (e: AskSageApiException) {
            assertEquals(3, callCount)
            assertTrue(e.message!!.contains("failed after 3 attempts"))
        }
    }

    fun testRetryOnIOException() {
        val json = """${""}{"data":[],"status":200}"""
        val client = AskSageApiClient("http://localhost")
        var callCount = 0
        client.requestExecutor = HttpRequestExecutor {
            callCount++
            if (callCount == 1) {
                throw IOException("Connection reset")
            } else {
                createMockResponse(200, json)
            }
        }
        val result: ModelsResponse = client.getModels("test-token")
        assertEquals(2, callCount)
        assertNotNull(result)
    }

    fun testNoRetryOnJsonSyntaxException() {
        val client = AskSageApiClient("http://localhost")
        var callCount = 0
        client.requestExecutor = HttpRequestExecutor {
            callCount++
            createMockResponse(200, "not valid json {{{")
        }
        try {
            client.getModels("test-token")
            fail("Expected AskSageApiException")
        } catch (e: AskSageApiException) {
            assertEquals(1, callCount)
            assertTrue(e.message!!.contains("Invalid response format"))
        }
    }

    fun testNoRetryOnInterruptedException() {
        val client = AskSageApiClient("http://localhost")
        var callCount = 0
        client.requestExecutor = HttpRequestExecutor {
            callCount++
            throw InterruptedException("Thread interrupted")
        }
        try {
            client.getModels("test-token")
            fail("Expected AskSageApiException")
        } catch (e: AskSageApiException) {
            assertEquals(1, callCount)
            assertTrue(e.message!!.contains("Request interrupted"))
        } finally {
            Thread.interrupted()
        }
    }

    fun testRetryOnIOExceptionMaxRetries() {
        val client = AskSageApiClient("http://localhost")
        var callCount = 0
        client.requestExecutor = HttpRequestExecutor {
            callCount++
            throw IOException("Connection refused")
        }
        try {
            client.getModels("test-token")
            fail("Expected AskSageApiException")
        } catch (e: AskSageApiException) {
            assertEquals(3, callCount)
            assertTrue(e.message!!.contains("failed after 3 attempts"))
        }
    }

    fun test401ThrowsAuthExceptionImmediately() {
        val client = AskSageApiClient("http://localhost")
        var callCount = 0
        client.requestExecutor = HttpRequestExecutor {
            callCount++
            createMockResponse(401, """${""}{"error":"Unauthorized"}""")
        }
        try {
            client.getModels("test-token")
            fail("Expected AskSageAuthException")
        } catch (e: AskSageAuthException) {
            assertEquals(1, callCount)
            assertTrue(e.message!!.contains("Authentication expired"))
        }
    }
}
