package ai.bigbear.pymatic.asksage.services

import org.junit.jupiter.api.Test
import ai.bigbear.pymatic.asksage.services.PromptTemplate
import ai.bigbear.pymatic.asksage.services.PromptTemplateService
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PromptTemplateServiceTest {

    @Test
    fun testPromptTemplateCreation() {
        val template = PromptTemplate(
            name = "Custom Prompt",
            systemPrompt = "You are helpful",
        )
        assertEquals("Custom Prompt", template.name)
    }

    @Test
    fun testTemplateService() {
        val service = PromptTemplateService.getInstance()
        assertNotNull(service)
    }

    @Test
    fun testMultipleTemplates() {
        val templates = listOf(
            PromptTemplate("Template1", "Prompt1"),
            PromptTemplate("Template2", "Prompt2"),
        )
        assertEquals(2, templates.size)
    }

    @Test
    fun testTemplateRetrieval() {
        val service = PromptTemplateService.getInstance()
        val templates = service.getAllTemplates()
        assertNotNull(templates)
    }

    @Test
    fun testEmptyPromptHandling() {
        val template = PromptTemplate("Empty", "")
        assertEquals("", template.systemPrompt)
    }
}
