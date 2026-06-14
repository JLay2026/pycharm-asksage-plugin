package asksage.services

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AskSageSettingsStateTest : BasePlatformTestCase() {

    fun testGetInstance() {
        val settings = AskSageSettingsState.getInstance()
        assertNotNull(settings)
    }

    fun testDefaultLiveMode() {
        val settings = AskSageSettingsState.getInstance()
        assertEquals("no-live", settings.defaultLiveMode)
    }

    fun testDefaultTemperature() {
        val settings = AskSageSettingsState.getInstance()
        assertEquals(0.7, settings.temperature, 0.01)
    }
}
