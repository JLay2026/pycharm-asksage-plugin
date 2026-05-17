package org.jetbrains.plugins.template.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.wm.ToolWindowManager
import org.jetbrains.plugins.template.services.EditorContext
import org.jetbrains.plugins.template.services.EditorContextService
import org.jetbrains.plugins.template.ui.toolwindow.ChatPanel

abstract class AskSageEditorAction(
    private val actionLabel: String,
) : AnAction() {

    abstract fun buildPrompt(context: EditorContext): String

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return

        val context = EditorContextService.getEditorContext(project)
        if (context == null) {
            LOG.warn("No editor context available for $actionLabel")
            return
        }

        val prompt = buildPrompt(context)

        // Open AskSage tool window and send the message
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("AskSage")
        if (toolWindow != null) {
            toolWindow.show {
                val content = toolWindow.contentManager.getContent(0)
                val chatPanel = content?.component as? ChatPanel
                chatPanel?.sendMessageWithContext(prompt)
            }
        }
    }

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabledAndVisible = editor != null
    }

    companion object {
        private val LOG = logger<AskSageEditorAction>()
    }
}
