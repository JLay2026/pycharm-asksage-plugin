package asksage.api

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import asksage.api.models.AnthropicResponse
import asksage.api.models.OpenAiChatResponse
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpClient.Version
import java.net.http.HttpResponse
import java.net.URI
import java.util.Optional
import javax.net.ssl.SSLSession

class AskSageApiClientEndpointTest : BasePlatformTestCase() {

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

    fun testOpenAiChatCompletionsSuccess() {
        val json = """{
            "id": "chatcmpl-abc",
            "choices": [{"index": 0, "message": {"role": "assistant", "content": "Hi"}, "finish_reason": "stop"}],
            "usage": {"prompt_tokens": 5, "completion_tokens": 2, "total_tokens": 7}
        }"""
        val client = AskSageApiClient("http://localhost")
        client.requestExecutor = HttpRequestExecutor { createMockResponse(200, json) }

        val request = asksage.api.models.OpenAiChatRequest(
            model = "gpt-4",
            messages = listOf(asksage.api.models.OpenAiChatMessage("user", "Hello")),
        )
        val response: OpenAiChatResponse = client.openAiChatCompletions("test-token", request)
        assertEquals("chatcmpl-abc", response.id)
        assertEquals(1, response.choices!!.size)
        assertEquals("Hi", response.choices!![0].message!!.content)
    }

    fun testAnthropicMessagesSuccess() {
        val json = """{
            "id": "msg_abc",
            "type": "message",
            "role": "assistant",
            "content": [{"type": "text", "text": "Hello there"}],
            "usage": {"input_tokens": 10, "output_tokens": 3}
        }"""
        val client = AskSageApiClient("http://localhost")
        client.requestExecutor = HttpRequestExecutor { createMockResponse(200, json) }

        val request = asksage.api.models.AnthropicRequest(
            model = "claude-3-opus",
            messages = listOf(asksage.api.models.AnthropicMessage("user", "Hello")),
        )
        val response: AnthropicResponse = client.anthropicMessages("test-token", request)
        assertEquals("msg_abc", response.id)
        assertEquals("assistant", response.role)
        assertEquals(1, response.content!!.size)
        assertEquals("Hello there", response.content!![0].text)
    }

    fun testOpenAiChatCompletions401ThrowsAuthException() {
        val client = AskSageApiClient("http://localhost")
        client.requestExecutor = HttpRequestExecutor {
            createMockResponse(401, """{"error":"Unauthorized"}""")
        }

        val request = asksage.api.models.OpenAiChatRequest(
            model = "gpt-4",
            messages = listOf(asksage.api.models.OpenAiChatMessage("user", "Hello")),
        )
        try {
            client.openAiChatCompletions("bad-token", request)
            fail("Expected AskSageAuthException")
        } catch (e: AskSageAuthException) {
            assertTrue(e.message!!.contains("Authentication expired"))
        }
    }

    fun testAnthropicMessages500RetriesAndFails() {
        val client = AskSageApiClient("http://localhost")
        var callCount = 0
        client.requestExecutor = HttpRequestExecutor {
            callCount++
            createMockResponse(500, "Internal Server Error")
        }

        val request = asksage.api.models.AnthropicRequest(
            model = "claude-3-opus",
            messages = listOf(asksage.api.models.AnthropicMessage("user", "Hello")),
        )
        try {
            client.anthropicMessages("test-token", request)
            fail("Expected AskSageApiException")
        } catch (e: AskSageApiException) {
            assertEquals(3, callCount)
            assertTrue(e.message!!.contains("failed after 3 attempts"))
        }
    }

    fun testQueryWithSystemPrompt() {
        val json = """{"response": "test response", "status": 200}"""
        val client = AskSageApiClient("http://localhost")
        client.requestExecutor = HttpRequestExecutor { request ->
            createMockResponse(200, json)
        }

        val queryRequest = asksage.api.models.QueryRequest(
            model = "gpt-4",
            message = "Review this code",
            systemPrompt = "You are a code reviewer",
        )
        val response = client.query("test-token", queryRequest)
        assertNotNull(response)
        assertEquals("test response", response.response)
    }

    fun testUpdateBaseUrl() {
        val client = AskSageApiClient("http://original")
        client.updateBaseUrl("http://updated")
        val json = """{"data":[],"status":200}"""
        client.requestExecutor = HttpRequestExecutor { createMockResponse(200, json) }
        val result = client.getModels("test-token")
        assertNotNull(result)
    }
}
