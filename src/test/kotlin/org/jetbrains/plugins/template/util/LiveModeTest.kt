package org.jetbrains.plugins.template.util

import junit.framework.TestCase

class LiveModeTest : TestCase() {

    fun testFromValueNoLive() {
        val mode = LiveMode.fromValue(0)
        assertEquals(LiveMode.NO_LIVE, mode)
        assertEquals(0, mode.value)
        assertEquals("No Live", mode.displayName)
    }

    fun testFromValueLive() {
        val mode = LiveMode.fromValue(1)
        assertEquals(LiveMode.LIVE, mode)
        assertEquals(1, mode.value)
    }

    fun testFromValueLivePlus() {
        val mode = LiveMode.fromValue(2)
        assertEquals(LiveMode.LIVE_PLUS, mode)
        assertEquals(2, mode.value)
    }

    fun testFromValueInvalidDefaultsToNoLive() {
        val mode = LiveMode.fromValue(99)
        assertEquals(LiveMode.NO_LIVE, mode)
    }

    fun testFromValueNegativeDefaultsToNoLive() {
        val mode = LiveMode.fromValue(-1)
        assertEquals(LiveMode.NO_LIVE, mode)
    }

    fun testAllModesHaveDescriptions() {
        for (mode in LiveMode.entries) {
            assertTrue(mode.description.isNotBlank())
            assertTrue(mode.displayName.isNotBlank())
        }
    }
}
