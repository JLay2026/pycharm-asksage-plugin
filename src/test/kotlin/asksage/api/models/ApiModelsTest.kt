package asksage.api.models

import com.google.gson.Gson
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ApiModelsTest : BasePlatformTestCase() {

    private val gson = Gson()

    fun testOpenAiChatMessageSerialization() {
        val message = OpenAiChatMessage("user", "Hello")
        val json = gson.toJson(message)
        assertTrue(json.contains("\"role\":\"%user%\"".replace("%", "")))
        assertTrue(json.contains("\"content\":\"%Hello%\"".replace("%", "")))
    }

    fun testOpenAiChatRequestSerialization() {
        val request = OpenAiChatRequest(
            model = "gpt-4",
            messages = listOf(OpenAiChatMessage("user", "Test"))
        )
        val json = gson.toJson(request)
        assertTrue(json.contains("gpt-4"))
        assertTrue(json.contains("Test"))
    }

    fun testAnthropicMessageSerialization() {
        val message = AnthropicMessage("user", "Hello")
        val json = gson.toJson(message)
        assertTrue(json.contains("user"))
        assertTrue(json.contains("Hello"))
    }

    fun testAnthropicRequestSerialization() {
        val request = AnthropicRequest(
            model = "claude-3-opus",
            messages = listOf(AnthropicMessage("user", "Test"))
        )
        val json = gson.toJson(request)
        assertTrue(json.contains("claude-3-opus"))
        assertTrue(json.contains("Test"))
    }

    fun testQueryRequestWithSystemPrompt() {
        val request = QueryRequest(
            model = "gpt-4",
            message = "Review this",
            systemPrompt = "You are a reviewer"
        )
        val json = gson.toJson(request)
        assertTrue(json.contains("gpt-4"))
        assertTrue(json.contains("Review this"))
        assertTrue(json.contains("You are a reviewer"))
    }

    fun testModelInfoCreation() {
        val model = ModelInfo(id = "gpt-4", ownedBy = "openai")
        assertEquals("gpt-4", model.id)
        assertEquals("openai", model.ownedBy)
    }

    fun testPersonaInfoCreation() {
        val persona = PersonaInfo(id = 1, name = "Reviewer", description = "Code reviewer")
        assertEquals(1, persona.id)
        assertEquals("Reviewer", persona.name)
        assertEquals("Code reviewer", persona.description)
    }
}
