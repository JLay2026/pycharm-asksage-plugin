package ai.bigbear.pymatic.asksage.util

import org.junit.jupiter.api.Test
import ai.bigbear.pymatic.asksage.util.LiveMode
import kotlin.test.assertEquals

class LiveModeTest {

    @Test
    fun testLiveModeValues() {
        assertEquals("disabled", LiveMode.NO_LIVE.value)
        assertEquals("enabled", LiveMode.LIVE.value)
        assertEquals("plus", LiveMode.LIVE_PLUS.value)
    }
}
