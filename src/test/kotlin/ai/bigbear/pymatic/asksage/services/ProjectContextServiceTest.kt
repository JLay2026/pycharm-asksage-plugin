package ai.bigbear.pymatic.asksage.services

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ProjectContextServiceTest : BasePlatformTestCase() {

    private lateinit var contextService: ProjectContextService

    override fun setUp() {
        super.setUp()
        contextService = project.getService(ProjectContextService::class.java)
    }

    fun testGetProjectNameNotBlank() {
        val name = contextService.getProjectName()
        assertTrue("Project name should not be blank", name.isNotBlank())
    }

    fun testGetProjectSummaryContainsProjectName() {
        val summary = contextService.getProjectSummary()
        assertTrue(
            "Summary should contain 'Project:'",
            summary.contains("Project:"),
        )
    }

    fun testBuildContextPrefixFormat() {
        val prefix = contextService.buildContextPrefix()
        assertTrue("Prefix should start with [Context:", prefix.startsWith("[Context:"))
        assertTrue("Prefix should end with newlines", prefix.endsWith("\n\n"))
    }

    fun testBuildContextPrefixContainsProjectName() {
        val prefix = contextService.buildContextPrefix()
        val projectName = contextService.getProjectName()
        assertTrue(
            "Prefix should contain the project name",
            prefix.contains(projectName),
        )
    }
}
