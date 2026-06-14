package asksage.api.models

import com.google.gson.Gson
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class OpenAiAnthropicModelsTest : BasePlatformTestCase() {

    private val gson = Gson()

    fun testOpenAiChatResponseDeserialization() {
        val json = """{
            "id": "chatcmpl-123",
            "object": "chat.completion",
            "created": 1234567890,
            "model": "gpt-4",
            "choices": [
                {
                    "index": 0,
                    "message": {
                        "role": "assistant",
                        "content": "Hello, I am Claude."
                    },
                    "finish_reason": "stop"
                }
            ],
            "usage": {
                "prompt_tokens": 10,
                "completion_tokens": 8,
                "total_tokens": 18
            }
        }"""
        val response = gson.fromJson(json, OpenAiChatResponse::class.java)
        assertEquals("chatcmpl-123", response.id)
        assertEquals(1, response.choices?.size)
        assertEquals("Hello, I am Claude.", response.choices?.get(0)?.message?.content)
        assertEquals(18, response.usage?.total_tokens)
    }

    fun testAnthropicResponseDeserialization() {
        val json = """{
            "id": "msg-123",
            "type": "message",
            "role": "assistant",
            "content": [
                {
                    "type": "text",
                    "text": "Hello, this is Anthropic."
                }
            ],
            "model": "claude-3-opus",
            "stop_reason": "end_turn",
            "usage": {
                "input_tokens": 15,
                "output_tokens": 10
            }
        }"""
        val response = gson.fromJson(json, AnthropicResponse::class.java)
        assertEquals("msg-123", response.id)
        assertEquals("assistant", response.role)
        assertEquals(1, response.content?.size)
        assertEquals("Hello, this is Anthropic.", response.content?.get(0)?.text)
        assertEquals(10, response.usage?.output_tokens)
    }

    fun testModelsResponseDeserialization() {
        val json = """{
            "data": [
                {"id": "gpt-4", "owned_by": "openai"},
                {"id": "claude-3-opus", "owned_by": "anthropic"}
            ],
            "status": 200
        }"""
        val response = gson.fromJson(json, ModelsResponse::class.java)
        assertEquals(2, response.data?.size)
        assertEquals("gpt-4", response.data?.get(0)?.id)
        assertEquals("claude-3-opus", response.data?.get(1)?.id)
    }

    fun testPersonasResponseDeserialization() {
        val json = """{
            "response": [
                {"id": 1, "name": "Reviewer", "description": "Code reviewer"},
                {"id": 2, "name": "Writer", "description": "Technical writer"}
            ],
            "status": 200
        }"""
        val response = gson.fromJson(json, PersonasResponse::class.java)
        assertEquals(2, response.response?.size)
        assertEquals("Reviewer", response.response?.get(0)?.name)
        assertEquals("Writer", response.response?.get(1)?.name)
    }

    fun testOpenAiChatMessageWithoutContent() {
        val message = OpenAiChatMessage("system", null)
        val json = gson.toJson(message)
        assertTrue(json.contains("\"role\":\"%system%\"".replace("%", "")))
    }

    fun testAnthropicContentBlockText() {
        val block = AnthropicContentBlock(type = "text", text = "Test content")
        val json = gson.toJson(block)
        assertTrue(json.contains("text"))
        assertTrue(json.contains("Test content"))
    }
}
