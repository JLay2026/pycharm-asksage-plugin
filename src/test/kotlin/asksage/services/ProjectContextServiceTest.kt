package asksage.services

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ProjectContextServiceTest : BasePlatformTestCase() {

    fun testGetProjectName() {
        val service = ProjectContextService.getInstance(project)
        assertNotNull(service.getProjectName())
    }

    fun testGetProjectPath() {
        val service = ProjectContextService.getInstance(project)
        assertNotNull(service.getProjectPath())
    }
}
