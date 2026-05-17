package org.jetbrains.plugins.template.ui.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class AskSageToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()

        val chatPanel = ChatPanel(project)
        val chatContent = contentFactory.createContent(chatPanel, "Chat", false)
        toolWindow.contentManager.addContent(chatContent)

        val pluginBrowserPanel = PluginBrowserPanel(project)
        val pluginsContent = contentFactory.createContent(pluginBrowserPanel, "Plugins", false)
        toolWindow.contentManager.addContent(pluginsContent)

        val agentPanel = AgentPanel(project)
        val agentsContent = contentFactory.createContent(agentPanel, "Agents", false)
        toolWindow.contentManager.addContent(agentsContent)

        val tokenUsagePanel = TokenUsagePanel(project)
        val usageContent = contentFactory.createContent(tokenUsagePanel, "Usage", false)
        toolWindow.contentManager.addContent(usageContent)
    }

    override fun shouldBeAvailable(project: Project) = true
}
