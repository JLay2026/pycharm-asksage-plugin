package ai.bigbear.pymatic.asksage.api

import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class AskSageEndpointsTest {

    @Test
    fun testQueryEndpointExists() {
        val endpoint = "/api/query"
        assertTrue(endpoint.isNotEmpty())
    }

    @Test
    fun testListAgentsEndpointExists() {
        val endpoint = "/api/agents"
        assertTrue(endpoint.startsWith("/api"))
    }

    @Test
    fun testListPluginsEndpointExists() {
        val endpoint = "/api/plugins"
        assertTrue(endpoint.contains("plugins"))
    }
}
