package asksage.ui.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import asksage.util.AskSageIcons

class AskSageToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()

        val chatPanel = ChatPanel(project)
        val chatContent = contentFactory.createContent(chatPanel, "Chat", false).apply {
            icon = AskSageIcons.Chat
        }
        toolWindow.contentManager.addContent(chatContent)

        val pluginBrowserPanel = PluginBrowserPanel(project)
        val pluginsContent = contentFactory.createContent(pluginBrowserPanel, "Plugins", false).apply {
            icon = AskSageIcons.Plugin
        }
        toolWindow.contentManager.addContent(pluginsContent)

        // Agents tab hidden for now (AgentPanel retained for future re-enable).
        // val agentPanel = AgentPanel(project)
        // val agentsContent = contentFactory.createContent(agentPanel, "Agents", false).apply {
        //     icon = AskSageIcons.Agent
        // }
        // toolWindow.contentManager.addContent(agentsContent)

        val tokenUsagePanel = TokenUsagePanel(project)
        val usageContent = contentFactory.createContent(tokenUsagePanel, "Usage", false).apply {
            icon = AskSageIcons.Usage
        }
        toolWindow.contentManager.addContent(usageContent)

        // Keep the model dropdown in sync across the Chat and Plugins tabs.
        toolWindow.contentManager.addContentManagerListener(object : ContentManagerListener {
            override fun selectionChanged(event: ContentManagerEvent) {
                when (val component = event.content.component) {
                    is ChatPanel -> component.syncSelectedModel()
                    is PluginBrowserPanel -> component.syncSelectedModel()
                }
            }
        })
    }

    override fun shouldBeAvailable(project: Project) = true
}
