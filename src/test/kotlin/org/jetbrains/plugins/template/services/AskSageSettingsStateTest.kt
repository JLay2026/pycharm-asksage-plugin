package org.jetbrains.plugins.template.services

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.plugins.template.api.AskSageEndpoints
import org.jetbrains.plugins.template.util.LiveMode

class AskSageSettingsStateTest : BasePlatformTestCase() {

    private lateinit var settings: AskSageSettingsState

    override fun setUp() {
        super.setUp()
        settings = AskSageSettingsState.getInstance()
    }

    fun testDefaultBaseUrl() {
        assertEquals(AskSageEndpoints.DEFAULT_BASE_URL, settings.baseUrl)
    }

    fun testDefaultLiveMode() {
        assertEquals(LiveMode.NO_LIVE.value, settings.defaultLiveMode)
    }

    fun testDefaultTemperature() {
        assertEquals(0.7, settings.temperature)
    }

    fun testDefaultReasoningEffort() {
        assertEquals("medium", settings.reasoningEffort)
    }

    fun testDefaultModelEmpty() {
        assertEquals("", settings.defaultModel)
    }

    fun testDefaultDatasetEmpty() {
        assertEquals("", settings.defaultDataset)
    }

    fun testDefaultPersonaZero() {
        assertEquals(0, settings.defaultPersona)
    }

    fun testGetStateReturnsSelf() {
        val state = settings.getState()
        assertSame(settings, state)
    }

    fun testLoadStateCopiesValues() {
        val other = AskSageSettingsState()
        other.baseUrl = "https://custom.api.com"
        other.temperature = 0.9
        other.reasoningEffort = "high"
        other.defaultLiveMode = LiveMode.LIVE_PLUS.value

        settings.loadState(other)
        assertEquals("https://custom.api.com", settings.baseUrl)
        assertEquals(0.9, settings.temperature)
        assertEquals("high", settings.reasoningEffort)
        assertEquals(LiveMode.LIVE_PLUS.value, settings.defaultLiveMode)

        // Reset for other tests
        settings.loadState(AskSageSettingsState())
    }

    fun testGetInstanceReturnsNonNull() {
        val instance = AskSageSettingsState.getInstance()
        assertNotNull(instance)
    }
}
