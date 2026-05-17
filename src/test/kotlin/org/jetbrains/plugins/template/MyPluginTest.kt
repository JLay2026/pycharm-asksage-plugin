package org.jetbrains.plugins.template

import com.intellij.openapi.extensions.PluginId
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MyPluginTest : BasePlatformTestCase() {

    fun testPluginDescriptorIsValid() {
        val descriptor = PluginManagerCore.getPlugin(
            PluginId.getId("org.jetbrains.plugins.template")
        )
        assertNotNull("Plugin descriptor should be loadable", descriptor)
        assertEquals("IntelliJ Platform Plugin Template", descriptor!!.name)
    }
}
