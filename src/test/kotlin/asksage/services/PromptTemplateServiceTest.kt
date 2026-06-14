package asksage.services

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class PromptTemplateServiceTest : BasePlatformTestCase() {

    fun testGetDefaultTemplate() {
        val service = PromptTemplateService.getInstance()
        val templates = service.getAllTemplates()
        assertTrue(templates.isNotEmpty())
    }

    fun testTemplateHasName() {
        val service = PromptTemplateService.getInstance()
        val templates = service.getAllTemplates()
        for (template in templates) {
            assertNotNull(template.name)
            assertFalse(template.name.isEmpty())
        }
    }

    fun testGetTemplateById() {
        val service = PromptTemplateService.getInstance()
        val templates = service.getAllTemplates()
        if (templates.isNotEmpty()) {
            val firstTemplate = templates[0]
            val retrieved = service.getTemplate(firstTemplate.id)
            assertNotNull(retrieved)
            assertEquals(firstTemplate.id, retrieved?.id)
        }
    }

    fun testCreateCustomTemplate() {
        val service = PromptTemplateService.getInstance()
        val template = PromptTemplate(
            id = "custom-test",
            name = "Test Template",
            systemPrompt = "You are helpful"
        )
        service.saveTemplate(template)
        val retrieved = service.getTemplate("custom-test")
        assertNotNull(retrieved)
        assertEquals("Test Template", retrieved?.name)
    }

    fun testUpdateTemplate() {
        val service = PromptTemplateService.getInstance()
        val template = PromptTemplate(
            id = "update-test",
            name = "Original Name",
            systemPrompt = "Original prompt"
        )
        service.saveTemplate(template)

        val updated = template.copy(name = "Updated Name")
        service.saveTemplate(updated)

        val retrieved = service.getTemplate("update-test")
        assertEquals("Updated Name", retrieved?.name)
    }
}
