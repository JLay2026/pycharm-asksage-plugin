package asksage.services

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class PromptTemplateServiceTest : BasePlatformTestCase() {

    private lateinit var service: PromptTemplateService

    override fun setUp() {
        super.setUp()
        service = PromptTemplateService.getInstance()
        // Clear any custom templates from previous tests
        service.customTemplateNames.clear()
        service.customTemplatePrompts.clear()
    }

    fun testBuiltInTemplatesNotEmpty() {
        val builtIn = service.getBuiltInTemplates()
        assertTrue("Built-in templates should not be empty", builtIn.isNotEmpty())
    }

    fun testBuiltInTemplatesContainNone() {
        val builtIn = service.getBuiltInTemplates()
        val none = builtIn.find { it.name == "(None)" }
        assertNotNull("Built-in templates should contain (None)", none)
        assertEquals("", none!!.systemPrompt)
        assertTrue(none.builtin)
    }

    fun testBuiltInTemplatesContainCodeReview() {
        val builtIn = service.getBuiltInTemplates()
        val codeReview = builtIn.find { it.name == "Code Review" }
        assertNotNull("Built-in templates should contain Code Review", codeReview)
        assertTrue(codeReview!!.systemPrompt.isNotBlank())
        assertTrue(codeReview.builtin)
    }

    fun testBuiltInTemplatesContainSecurityAudit() {
        val builtIn = service.getBuiltInTemplates()
        val securityAudit = builtIn.find { it.name == "Security Audit" }
        assertNotNull("Built-in templates should contain Security Audit", securityAudit)
        assertTrue(securityAudit!!.systemPrompt.contains("security"))
    }

    fun testBuiltInTemplatesContainPerformanceAnalysis() {
        val builtIn = service.getBuiltInTemplates()
        val perf = builtIn.find { it.name == "Performance Analysis" }
        assertNotNull("Built-in templates should contain Performance Analysis", perf)
        assertTrue(perf!!.systemPrompt.contains("performance"))
    }

    fun testBuiltInTemplatesContainUnitTestGenerator() {
        val builtIn = service.getBuiltInTemplates()
        val testGen = builtIn.find { it.name == "Unit Test Generator" }
        assertNotNull("Built-in templates should contain Unit Test Generator", testGen)
        assertTrue(testGen!!.systemPrompt.contains("test"))
    }

    fun testBuiltInTemplatesContainArchitectureReview() {
        val builtIn = service.getBuiltInTemplates()
        val arch = builtIn.find { it.name == "Architecture Review" }
        assertNotNull("Built-in templates should contain Architecture Review", arch)
    }

    fun testBuiltInTemplatesContainDocumentationWriter() {
        val builtIn = service.getBuiltInTemplates()
        val docs = builtIn.find { it.name == "Documentation Writer" }
        assertNotNull("Built-in templates should contain Documentation Writer", docs)
    }

    fun testBuiltInTemplatesContainExplainLikeImJunior() {
        val builtIn = service.getBuiltInTemplates()
        val explain = builtIn.find { it.name == "Explain Like I'm Junior" }
        assertNotNull("Built-in templates should contain Explain Like I'm Junior", explain)
    }

    fun testBuiltInTemplateCount() {
        val builtIn = service.getBuiltInTemplates()
        assertEquals("Should have 8 built-in templates (including None)", 8, builtIn.size)
    }

    fun testAllBuiltInTemplatesAreMarkedBuiltin() {
        val builtIn = service.getBuiltInTemplates()
        for (template in builtIn) {
            assertTrue("Template '${template.name}' should be marked as builtin", template.builtin)
        }
    }

    fun testAddCustomTemplate() {
        service.addCustomTemplate("My Template", "Custom system prompt")
        val custom = service.getCustomTemplates()
        assertEquals(1, custom.size)
        assertEquals("My Template", custom[0].name)
        assertEquals("Custom system prompt", custom[0].systemPrompt)
        assertFalse(custom[0].builtin)
    }

    fun testAddMultipleCustomTemplates() {
        service.addCustomTemplate("Template A", "Prompt A")
        service.addCustomTemplate("Template B", "Prompt B")
        val custom = service.getCustomTemplates()
        assertEquals(2, custom.size)
        assertEquals("Template A", custom[0].name)
        assertEquals("Template B", custom[1].name)
    }

    fun testRemoveCustomTemplate() {
        service.addCustomTemplate("To Remove", "Prompt")
        assertEquals(1, service.getCustomTemplates().size)

        service.removeCustomTemplate("To Remove")
        assertEquals(0, service.getCustomTemplates().size)
    }

    fun testRemoveNonExistentCustomTemplate() {
        service.addCustomTemplate("Existing", "Prompt")
        service.removeCustomTemplate("NonExistent")
        assertEquals(1, service.getCustomTemplates().size)
    }

    fun testGetAllTemplatesIncludesBothBuiltInAndCustom() {
        service.addCustomTemplate("Custom", "Custom prompt")
        val all = service.getAllTemplates()
        val builtIn = service.getBuiltInTemplates()
        assertEquals(builtIn.size + 1, all.size)
    }

    fun testGetAllTemplatesBuiltInFirst() {
        service.addCustomTemplate("Custom", "Custom prompt")
        val all = service.getAllTemplates()
        assertEquals("(None)", all[0].name)
        assertEquals("Custom", all.last().name)
    }

    fun testCustomTemplatesEmptyByDefault() {
        val custom = service.getCustomTemplates()
        assertTrue("Custom templates should be empty by default", custom.isEmpty())
    }
}
