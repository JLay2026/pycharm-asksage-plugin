package ai.bigbear.pymatic.asksage.services

import org.junit.jupiter.api.Test
import ai.bigbear.pymatic.asksage.services.AskSageSettingsState
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AskSageSettingsStateTest {

    @Test
    fun testSettingsInstanceCreation() {
        val settings = AskSageSettingsState.getInstance()
        assertNotNull(settings)
    }

    @Test
    fun testDefaultBaseUrl() {
        val settings = AskSageSettingsState.getInstance()
        assertNotNull(settings.baseUrl)
    }
}
