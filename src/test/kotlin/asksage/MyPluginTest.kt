package asksage

import com.intellij.openapi.extensions.PluginId
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MyPluginTest : BasePlatformTestCase() {

    fun testPluginDescriptorIsValid() {
        val descriptor = PluginManagerCore.getPlugin(
            PluginId.getId("asksage")
        )
        assertNotNull("Plugin descriptor should be loadable", descriptor)
        assertEquals("AskSage", descriptor!!.name)
    }
}
