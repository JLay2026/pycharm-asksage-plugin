package ai.bigbear.pymatic.asksage

import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import ai.bigbear.pymatic.asksage.AskSageBundle

@TestDataPath("${'$'}PROJECT_ROOT/src/test/testData")
class MyPluginTest : BasePlatformTestCase() {

    fun testAskSageBundleAccess() {
        val message = AskSageBundle.message("msg.sample")
        assertEquals("Sample message", message)
    }

    override fun getTestDataPath() = "src/test/testData"
}
