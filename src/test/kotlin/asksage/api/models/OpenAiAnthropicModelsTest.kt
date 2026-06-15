package asksage.api.models

import com.google.gson.Gson
import junit.framework.TestCase

class OpenAiAnthropicModelsTest : TestCase() {

    private val gson = Gson()

    // --- OpenAI-Compatible ---

    fun testOpenAiChatMessageSerialization() {
        val msg = OpenAiChatMessage(role = "user", content = "Hello")
        val json = gson.toJson(msg)
        assertTrue(json.contains("\"role\":\"user\""))
        assertTrue(json.contains("\"content\":\"Hello\""))
    }

    fun testOpenAiChatRequestDefaults() {
        val request = OpenAiChatRequest(
            model = "gpt-4",
            messages = listOf(OpenAiChatMessage("user", "Hi")),
        )
        assertEquals(0.7, request.temperature)
        assertFalse(request.stream)
    }

    fun testOpenAiChatRequestSerialization() {
        val request = OpenAiChatRequest(
            model = "gpt-4",
            messages = listOf(
                OpenAiChatMessage("system", "You are helpful"),
                OpenAiChatMessage("user", "Hello"),
            ),
            temperature = 0.5,
            stream = true,
        )
        val json = gson.toJson(request)
        assertTrue(json.contains("gpt-4"))
        assertTrue(json.contains("\"stream\":true"))
        assertTrue(json.contains("You are helpful"))
    }

    fun testOpenAiChatResponseDeserialization() {
        val json = """{
            "id": "chatcmpl-123",
            "choices": [
                {
                    "index": 0,
                    "message": {"role": "assistant", "content": "Hello!"},
                    "finish_reason": "stop"
                }
            ],
            "usage": {
                "prompt_tokens": 10,
                "completion_tokens": 5,
                "total_tokens": 15
            }
        }"""
        val response = gson.fromJson(json, OpenAiChatResponse::class.java)
        assertEquals("chatcmpl-123", response.id)
        assertNotNull(response.choices)
        assertEquals(1, response.choices!!.size)
        assertEquals(0, response.choices!![0].index)
        assertEquals("assistant", response.choices!![0].message!!.role)
        assertEquals("Hello!", response.choices!![0].message!!.content)
        assertEquals("stop", response.choices!![0].finishReason)
        assertEquals(10, response.usage!!.promptTokens)
        assertEquals(5, response.usage!!.completionTokens)
        assertEquals(15, response.usage!!.totalTokens)
    }

    fun testOpenAiChatResponseNullFields() {
        val json = """{"id": null, "choices": null, "usage": null}"""
        val response = gson.fromJson(json, OpenAiChatResponse::class.java)
        assertNull(response.id)
        assertNull(response.choices)
        assertNull(response.usage)
    }

    fun testOpenAiUsageDeserialization() {
        val json = """{"prompt_tokens": 100, "completion_tokens": 50, "total_tokens": 150}"""
        val usage = gson.fromJson(json, OpenAiUsage::class.java)
        assertEquals(100, usage.promptTokens)
        assertEquals(50, usage.completionTokens)
        assertEquals(150, usage.totalTokens)
    }

    // --- Anthropic-Compatible ---

    fun testAnthropicMessageSerialization() {
        val msg = AnthropicMessage(role = "user", content = "Hello")
        val json = gson.toJson(msg)
        assertTrue(json.contains("\"role\":\"user\""))
        assertTrue(json.contains("\"content\":\"Hello\""))
    }

    fun testAnthropicRequestDefaults() {
        val request = AnthropicRequest(
            model = "claude-3-opus",
            messages = listOf(AnthropicMessage("user", "Hi")),
        )
        assertEquals(4096, request.maxTokens)
        assertEquals(0.7, request.temperature)
        assertNull(request.system)
    }

    fun testAnthropicRequestWithSystemPrompt() {
        val request = AnthropicRequest(
            model = "claude-3-opus",
            messages = listOf(AnthropicMessage("user", "Hi")),
            system = "You are a helpful assistant",
        )
        assertEquals("You are a helpful assistant", request.system)
    }

    fun testAnthropicRequestSerialization() {
        val request = AnthropicRequest(
            model = "claude-3-sonnet",
            messages = listOf(AnthropicMessage("user", "Test")),
            maxTokens = 2048,
            temperature = 0.3,
            system = "Be concise",
        )
        val json = gson.toJson(request)
        assertTrue(json.contains("claude-3-sonnet"))
        assertTrue(json.contains("max_tokens"))
        assertTrue(json.contains("Be concise"))
    }

    fun testAnthropicResponseDeserialization() {
        val json = """{
            "id": "msg_123",
            "type": "message",
            "role": "assistant",
            "content": [
                {"type": "text", "text": "Hello!"}
            ],
            "usage": {
                "input_tokens": 10,
                "output_tokens": 5
            }
        }"""
        val response = gson.fromJson(json, AnthropicResponse::class.java)
        assertEquals("msg_123", response.id)
        assertEquals("message", response.type)
        assertEquals("assistant", response.role)
        assertNotNull(response.content)
        assertEquals(1, response.content!!.size)
        assertEquals("text", response.content!![0].type)
        assertEquals("Hello!", response.content!![0].text)
        assertEquals(10, response.usage!!.inputTokens)
        assertEquals(5, response.usage!!.outputTokens)
    }

    fun testAnthropicResponseMultipleContentBlocks() {
        val json = """{
            "id": "msg_456",
            "type": "message",
            "role": "assistant",
            "content": [
                {"type": "text", "text": "First block"},
                {"type": "text", "text": "Second block"}
            ]
        }"""
        val response = gson.fromJson(json, AnthropicResponse::class.java)
        assertEquals(2, response.content!!.size)
        assertEquals("First block", response.content!![0].text)
        assertEquals("Second block", response.content!![1].text)
    }

    fun testAnthropicContentBlockDeserialization() {
        val json = """{"type": "text", "text": "Some content"}"""
        val block = gson.fromJson(json, AnthropicContentBlock::class.java)
        assertEquals("text", block.type)
        assertEquals("Some content", block.text)
    }

    fun testAnthropicUsageDeserialization() {
        val json = """{"input_tokens": 200, "output_tokens": 100}"""
        val usage = gson.fromJson(json, AnthropicUsage::class.java)
        assertEquals(200, usage.inputTokens)
        assertEquals(100, usage.outputTokens)
    }
}
