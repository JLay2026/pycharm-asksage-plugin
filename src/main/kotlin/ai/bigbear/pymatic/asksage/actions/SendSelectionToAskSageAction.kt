package ai.bigbear.pymatic.asksage.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.wm.ToolWindowManager
import ai.bigbear.pymatic.asksage.services.EditorContextService
import ai.bigbear.pymatic.asksage.ui.toolwindow.ChatPanel

class SendSelectionToAskSageAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return

        val selectedText = EditorContextService.getSelectedText(editor) ?: return
        val context = EditorContextService.getEditorContext(project) ?: return

        val fileContext = EditorContextService.buildFileContextPrompt(context)
        val prompt = "I have a question about this code:\n\n$fileContext"

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
        val hasSelection = editor?.selectionModel?.hasSelection() == true
        e.presentation.isEnabledAndVisible = editor != null && hasSelection
    }
}
