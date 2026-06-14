package asksage.util

import junit.framework.TestCase

class LiveModeTest : TestCase() {

    fun testLiveModeValues() {
        val modes = LiveMode.entries
        assertTrue(modes.isNotEmpty())
    }

    fun testLiveModeFromValue() {
        val mode = LiveMode.fromValue("live")
        assertEquals(LiveMode.LIVE, mode)
    }

    fun testLiveModeDisplayName() {
        for (mode in LiveMode.entries) {
            assertNotNull(mode.displayName)
            assertFalse(mode.displayName.isEmpty())
        }
    }

    fun testLiveModeDescription() {
        for (mode in LiveMode.entries) {
            assertNotNull(mode.description)
        }
    }
}
