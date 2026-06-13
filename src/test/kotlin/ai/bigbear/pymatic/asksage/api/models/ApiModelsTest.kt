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

    fun testQueryResponseAnswerPrefersMessage() {
        // The generated answer is in `message`; `response` holds a status string.
        val json = """{"response": "OK", "message": "The answer", "status": 200}"""
        val r = gson.fromJson(json, QueryResponse::class.java)
        assertEquals("The answer", r.answer())
    }

    fun testQueryResponseAnswerFallsBackToResponse() {
        val json = """{"response": "Only here", "status": 200}"""
        val r = gson.fromJson(json, QueryResponse::class.java)
        assertEquals("Only here", r.answer())
    }

    fun testTokenResponseTopLevelAccessToken() {
        val json = """{"access_token": "abc123", "status": 200}"""
        val response = gson.fromJson(json, TokenResponse::class.java)
        assertEquals("abc123", response.accessToken)
        assertEquals(200, response.status)
        assertEquals("abc123", response.resolveToken())
    }

    fun testTokenResponseObjectResponse() {
        val json = """{"response": {"access_token": "nested-token"}, "status": 200}"""
        val response = gson.fromJson(json, TokenResponse::class.java)
        assertEquals("nested-token", response.resolveToken())
    }

    fun testTokenResponseBareStringResponse() {
        val json = """{"response": "string-token", "status": 200}"""
        val response = gson.fromJson(json, TokenResponse::class.java)
        assertEquals("string-token", response.resolveToken())
    }

    fun testModelsResponseRealShape() {
        val json = """{"response": ["gpt-4"], "object": "list", "data": [{"id": "gpt-4", "object": "model", "created": "2024", "name": "GPT-4", "owned_by": "openai"}], "status": 200}"""
        val response = gson.fromJson(json, ModelsResponse::class.java)
        val models = response.resolveModels()
        assertEquals(1, models.size)
        assertEquals("gpt-4", models[0].id)
        assertEquals("GPT-4", models[0].name)
        assertEquals("openai", models[0].ownedBy)
    }

    fun testModelsResponseFallsBackToStringList() {
        val json = """{"response": ["model-a", "model-b"], "status": 200}"""
        val response = gson.fromJson(json, ModelsResponse::class.java)
        val models = response.resolveModels()
        assertEquals(2, models.size)
        assertEquals("model-a", models[0].id)
        assertEquals("model-a", models[0].name)
    }

    fun testModelInfoSerialization() {
        val json = """{"id": "gpt-4", "name": "GPT-4", "owned_by": "openai"}"""
        val model = gson.fromJson(json, ModelInfo::class.java)
        assertEquals("gpt-4", model.id)
        assertEquals("GPT-4", model.name)
        assertEquals("openai", model.ownedBy)
    }

    fun testPluginInfoRealShape() {
        // API items use plugin_name/title, not name.
        val json = """{"id": "p1", "plugin_name": "summarize", "title": "Summarize", "description": "A test", "category": "utility"}"""
        val plugin = gson.fromJson(json, PluginInfo::class.java)
        assertEquals("p1", plugin.id)
        assertEquals("summarize", plugin.pluginName)
        assertEquals("Summarize", plugin.displayName)
        assertEquals("summarize", plugin.identifier)
        assertEquals("utility", plugin.category)
    }

    fun testAgentInfoSerialization() {
        val json = """{"id": 1, "uuid": "u-1", "name": "Test Agent", "description": "An agent"}"""
        val agent = gson.fromJson(json, AgentInfo::class.java)
        assertEquals(1, agent.id)
        assertEquals("Test Agent", agent.name)
        assertEquals("An agent", agent.description)
    }

    fun testExecuteAgentResponseNestedText() {
        val json = """{"status": 200, "execution_status": "completed", "response": {"response": "agent answer", "type": "text", "source": "node1"}}"""
        val r = gson.fromJson(json, ExecuteAgentResponse::class.java)
        assertEquals("agent answer", r.text())
    }

    fun testFollowUpResponseQuestionsFromJsonArray() {
        val json = """{"message": "[\"Q1?\", \"Q2?\"]", "status": 200}"""
        val r = gson.fromJson(json, FollowUpResponse::class.java)
        val questions = r.questions()
        assertEquals(2, questions.size)
        assertEquals("Q1?", questions[0])
    }

    fun testTokenUsageResponseInteger() {
        val json = """{"response": 12345, "status": 200}"""
        val r = gson.fromJson(json, TokenUsageResponse::class.java)
        assertEquals(12345L, r.count())
    }

    fun testTrainRequestSerialization() {
        val request = TrainRequest(
            content = "some code",
            forceDataset = "my-dataset",
        )
        val json = gson.toJson(request)
        assertTrue(json.contains("my-dataset"))
        assertTrue(json.contains("some code"))
        assertTrue(json.contains("force_dataset"))
    }

    fun testExecutePluginRequestSerialization() {
        val request = ExecutePluginRequest(
            pluginName = "summarize",
            pluginValues = """{"message":"Hello"}""",
            model = "gpt-4",
            live = 1,
        )
        val json = gson.toJson(request)
        assertTrue(json.contains("plugin_name"))
        assertTrue(json.contains("plugin_values"))
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
