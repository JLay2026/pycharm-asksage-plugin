package ai.bigbear.pymatic.asksage.api.models

import org.junit.jupiter.api.Test
import ai.bigbear.pymatic.asksage.api.models.ModelInfo
import ai.bigbear.pymatic.asksage.api.models.QueryRequest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ApiModelsTest {

    @Test
    fun testModelInfoCreation() {
        val model = ModelInfo(
            id = "gpt-4",
            ownedBy = "openai",
        )
        assertEquals("gpt-4", model.id)
        assertEquals("openai", model.ownedBy)
    }

    @Test
    fun testQueryRequestCreation() {
        val request = QueryRequest(
            model = "gpt-4",
            message = "Test message",
            live = "enabled",
        )
        assertNotNull(request)
        assertEquals("Test message", request.message)
    }

    @Test
    fun testQueryRequestWithOptionalFields() {
        val request = QueryRequest(
            model = "gpt-4",
            message = "Test",
            live = "disabled",
            dataset = "dataset-1",
            persona = 1,
        )
        assertEquals("dataset-1", request.dataset)
        assertEquals(1, request.persona)
    }

    @Test
    fun testQueryRequestDefaults() {
        val request = QueryRequest(
            model = "gpt-3.5",
            message = "Hello",
            live = "disabled",
        )
        assertNotNull(request.temperature)
    }
}
