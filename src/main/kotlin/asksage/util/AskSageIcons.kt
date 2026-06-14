package asksage.util

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object AskSageIcons {
    @JvmField val ToolWindow: Icon = IconLoader.getIcon("/icons/asksage.svg", AskSageIcons::class.java)
    @JvmField val Chat: Icon = IconLoader.getIcon("/icons/chat.svg", AskSageIcons::class.java)
    @JvmField val Plugin: Icon = IconLoader.getIcon("/icons/plugin.svg", AskSageIcons::class.java)
    @JvmField val Agent: Icon = IconLoader.getIcon("/icons/agent.svg", AskSageIcons::class.java)
    @JvmField val Usage: Icon = IconLoader.getIcon("/icons/usage.svg", AskSageIcons::class.java)
    @JvmField val Copy: Icon = IconLoader.getIcon("/icons/copy.svg", AskSageIcons::class.java)
    @JvmField val Export: Icon = IconLoader.getIcon("/icons/export.svg", AskSageIcons::class.java)
}
