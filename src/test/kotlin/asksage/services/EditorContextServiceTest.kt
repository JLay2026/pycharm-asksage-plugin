package asksage.services

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class EditorContextServiceTest : BasePlatformTestCase() {

    fun testGetSelectedText() {
        myFixture.configureByText("Test.kt", "fun main() {}")
        val service = EditorContextService.getInstance(project)
        assertNotNull(service)
    }

    fun testGetCurrentFile() {
        myFixture.configureByText("Test.kt", "fun main() {}")
        val service = EditorContextService.getInstance(project)
        assertNotNull(service.getCurrentFile())
    }

    fun testGetOpenFiles() {
        myFixture.configureByText("Test1.kt", "fun main1() {}")
        myFixture.configureByText("Test2.kt", "fun main2() {}")
        val service = EditorContextService.getInstance(project)
        val files = service.getOpenFiles()
        assertTrue(files.isNotEmpty())
    }
}
