package ai.bigbear.pymatic.asksage.api.models

import org.junit.jupiter.api.Test
import ai.bigbear.pymatic.asksage.api.models.ModelInfo
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OpenAiAnthropicModelsTest {

    @Test
    fun testOpenAiModelCreation() {
        val model = ModelInfo(
            id = "gpt-4",
            ownedBy = "openai",
        )
        assertEquals("gpt-4", model.id)
        assertEquals("openai", model.ownedBy)
    }

    @Test
    fun testGpt35TurboModel() {
        val model = ModelInfo(
            id = "gpt-3.5-turbo",
            ownedBy = "openai",
        )
        assertTrue(model.id.contains("3.5"))
    }

    @Test
    fun testAnthropicModelCreation() {
        val model = ModelInfo(
            id = "claude-3-opus",
            ownedBy = "anthropic",
        )
        assertEquals("claude-3-opus", model.id)
        assertEquals("anthropic", model.ownedBy)
    }

    @Test
    fun testClaudeModelVariants() {
        val models = listOf(
            ModelInfo("claude-3-opus", "anthropic"),
            ModelInfo("claude-3-sonnet", "anthropic"),
            ModelInfo("claude-3-haiku", "anthropic"),
        )
        assertEquals(3, models.size)
    }

    @Test
    fun testModelIdValidation() {
        val validModel = ModelInfo(
            id = "gpt-4-turbo",
            ownedBy = "openai",
        )
        assertTrue(validModel.id.isNotEmpty())
    }

    @Test
    fun testOwnerValidation() {
        val model = ModelInfo(
            id = "test-model",
            ownedBy = "test-provider",
        )
        assertTrue(model.ownedBy!!.isNotEmpty())
    }
}
