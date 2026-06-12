package ai.bigbear.pymatic.asksage.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindowManager
import ai.bigbear.pymatic.asksage.services.EditorContextService
import ai.bigbear.pymatic.asksage.ui.toolwindow.ChatPanel

class AskAboutFileAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val context = EditorContextService.getEditorContext(project)
        if (context == null) {
            Messages.showWarningDialog(project, "No file is currently open in the editor.", "AskSage")
            return
        }

        val question = Messages.showInputDialog(
            project,
            "Ask about ${context.fileName}:",
            "Ask AskSage About This File",
            null,
        )

        if (question.isNullOrBlank()) return

        val fileContext = EditorContextService.buildFileContextPrompt(context)
        val prompt = "$question\n\n$fileContext"

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
}
