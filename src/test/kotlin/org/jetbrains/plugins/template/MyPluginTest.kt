package org.jetbrains.plugins.template

import com.intellij.openapi.extensions.PluginId
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MyPluginTest : BasePlatformTestCase() {

    fun testPluginDescriptorIsValid() {
        val descriptor = PluginManagerCore.getPlugin(
            PluginId.getId("ai.bigbear.pymatic.asksage")
        )
        assertNotNull("Plugin descriptor should be loadable", descriptor)
        assertEquals("Pymatic AskSage", descriptor!!.name)
    }
}
