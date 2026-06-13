package ai.bigbear.pymatic.asksage.api.models

import com.google.gson.Gson
import junit.framework.TestCase

class ApiModelsTest : TestCase() {

    private val gson = Gson()

    fun testQueryRequestDefaults() {
        val request = QueryRequest(
            model = "gpt-4",
            message = "Hello",
        )
        assertEquals(0, request.live)
        assertEquals(0.7, request.temperature)
        assertEquals("medium", request.reasoningEffort)
        assertFalse(request.streaming)
        assertEquals("chat", request.mode)
        assertNull(request.dataset)
        assertNull(request.persona)
    }

    fun testQueryRequestCopyWithStreaming() {
        val request = QueryRequest(model = "gpt-4", message = "test")
        val streaming = request.copy(streaming = true)
        assertTrue(streaming.streaming)
        assertEquals(request.model, streaming.model)
        assertEquals(request.message, streaming.message)
    }

    fun testTokenResponseSerialization() {
        val json = """{"access_token": "abc123", "status": 200}"""
        val response = gson.fromJson(json, TokenResponse::class.java)
        assertEquals("abc123", response.accessToken)
        assertEquals(200, response.status)
    }

    fun testModelInfoSerialization() {
        val json = """{"id": "gpt-4", "name": "GPT-4", "owned_by": "openai"}"""
        val model = gson.fromJson(json, ModelInfo::class.java)
        assertEquals("gpt-4", model.id)
        assertEquals("GPT-4", model.name)
        assertEquals("openai", model.ownedBy)
    }

    fun testPluginInfoSerialization() {
        val json = """{"id": "p1", "name": "Test Plugin", "description": "A test", "category": "utility"}"""
        val plugin = gson.fromJson(json, PluginInfo::class.java)
        assertEquals("p1", plugin.id)
        assertEquals("Test Plugin", plugin.name)
        assertEquals("A test", plugin.description)
        assertEquals("utility", plugin.category)
    }

    fun testAgentInfoSerialization() {
        val json = """{"id": "a1", "name": "Test Agent", "description": "An agent"}"""
        val agent = gson.fromJson(json, AgentInfo::class.java)
        assertEquals("a1", agent.id)
        assertEquals("Test Agent", agent.name)
        assertEquals("An agent", agent.description)
    }

    fun testTrainRequestSerialization() {
        val request = TrainRequest(
            dataset = "my-dataset",
            content = "some code",
            title = "My Title",
        )
        val json = gson.toJson(request)
        assertTrue(json.contains("my-dataset"))
        assertTrue(json.contains("some code"))
        assertTrue(json.contains("My Title"))
    }

    fun testTokenUsageDataSerialization() {
        val json = """{"total_tokens": 1000, "monthly_tokens": 500, "daily_tokens": 50, "remaining_tokens": 9500}"""
        val usage = gson.fromJson(json, TokenUsageData::class.java)
        assertEquals(1000L, usage.totalTokens)
        assertEquals(500L, usage.monthlyTokens)
        assertEquals(50L, usage.dailyTokens)
        assertEquals(9500L, usage.remainingTokens)
    }

    fun testExecutePluginRequestSerialization() {
        val request = ExecutePluginRequest(
            plugin = "summarize",
            message = "Hello",
            model = "gpt-4",
            live = 1,
        )
        val json = gson.toJson(request)
        assertTrue(json.contains("summarize"))
        assertTrue(json.contains("gpt-4"))
    }

    fun testChatMessageRole() {
        val roles = ai.bigbear.pymatic.asksage.services.MessageRole.entries
        assertEquals(3, roles.size)
        assertTrue(roles.contains(ai.bigbear.pymatic.asksage.services.MessageRole.USER))
        assertTrue(roles.contains(ai.bigbear.pymatic.asksage.services.MessageRole.ASSISTANT))
        assertTrue(roles.contains(ai.bigbear.pymatic.asksage.services.MessageRole.ERROR))
    }
}
