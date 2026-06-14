package asksage.api

import junit.framework.TestCase

class AskSageEndpointsTest : TestCase() {

    fun testDefaultBaseUrl() {
        assertEquals("https://api.asksage.ai", AskSageEndpoints.DEFAULT_BASE_URL)
    }

    fun testGetTokenEndpoint() {
        assertEquals("/user/get-token-with-api-key", AskSageEndpoints.GET_TOKEN)
    }

    fun testGetModelsEndpoint() {
        assertEquals("/server/get-models", AskSageEndpoints.GET_MODELS)
    }

    fun testGetPersonasEndpoint() {
        assertEquals("/server/get-personas", AskSageEndpoints.GET_PERSONAS)
    }

    fun testGetDatasetsEndpoint() {
        assertEquals("/server/get-datasets", AskSageEndpoints.GET_DATASETS)
    }

    fun testQueryEndpoint() {
        assertEquals("/server/query", AskSageEndpoints.QUERY)
    }

    fun testFollowUpQuestionsEndpoint() {
        assertEquals("/server/follow-up-questions", AskSageEndpoints.FOLLOW_UP_QUESTIONS)
    }

    fun testGetPluginsEndpoint() {
        assertEquals("/server/get-plugins", AskSageEndpoints.GET_PLUGINS)
    }

    fun testExecutePluginEndpoint() {
        assertEquals("/server/execute-plugin", AskSageEndpoints.EXECUTE_PLUGIN)
    }

    fun testListAgentsEndpoint() {
        assertEquals("/server/list-agents", AskSageEndpoints.LIST_AGENTS)
    }

    fun testExecuteAgentEndpoint() {
        assertEquals("/server/execute-agent", AskSageEndpoints.EXECUTE_AGENT)
    }

    fun testTrainEndpoint() {
        assertEquals("/server/train", AskSageEndpoints.TRAIN)
    }

    fun testCountMonthlyTokensEndpoint() {
        assertEquals("/server/count-monthly-tokens", AskSageEndpoints.COUNT_MONTHLY_TOKENS)
    }

    fun testOpenAiChatCompletionsEndpoint() {
        assertEquals("/openai/v1/chat/completions", AskSageEndpoints.OPENAI_CHAT_COMPLETIONS)
    }

    fun testAnthropicMessagesEndpoint() {
        assertEquals("/anthropic/v1/messages", AskSageEndpoints.ANTHROPIC_MESSAGES)
    }

    fun testAllEndpointsStartWithSlash() {
        val endpoints = listOf(
            AskSageEndpoints.GET_TOKEN,
            AskSageEndpoints.GET_MODELS,
            AskSageEndpoints.GET_PERSONAS,
            AskSageEndpoints.GET_DATASETS,
            AskSageEndpoints.QUERY,
            AskSageEndpoints.FOLLOW_UP_QUESTIONS,
            AskSageEndpoints.GET_PLUGINS,
            AskSageEndpoints.EXECUTE_PLUGIN,
            AskSageEndpoints.LIST_AGENTS,
            AskSageEndpoints.EXECUTE_AGENT,
            AskSageEndpoints.TRAIN,
            AskSageEndpoints.COUNT_MONTHLY_TOKENS,
            AskSageEndpoints.OPENAI_CHAT_COMPLETIONS,
            AskSageEndpoints.ANTHROPIC_MESSAGES,
        )
        for (endpoint in endpoints) {
            assertTrue("Endpoint '$endpoint' should start with /", endpoint.startsWith("/"))
        }
    }
}
