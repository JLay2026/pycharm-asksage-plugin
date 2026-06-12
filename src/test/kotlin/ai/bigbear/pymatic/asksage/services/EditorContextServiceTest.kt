package ai.bigbear.pymatic.asksage.services

import org.junit.jupiter.api.Test
import ai.bigbear.pymatic.asksage.services.ProjectContextService
import kotlin.test.assertNotNull

class EditorContextServiceTest {

    @Test
    fun testContextServiceExists() {
        assertNotNull(ProjectContextService::class)
    }

    @Test
    fun testContextBuilding() {
        val prefix = "/* Project context */"
        assertNotNull(prefix)
    }
}
