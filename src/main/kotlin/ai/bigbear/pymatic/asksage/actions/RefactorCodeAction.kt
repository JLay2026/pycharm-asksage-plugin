package ai.bigbear.pymatic.asksage.actions

import ai.bigbear.pymatic.asksage.services.EditorContext
import ai.bigbear.pymatic.asksage.services.EditorContextService

class RefactorCodeAction : AskSageEditorAction("Refactor with AskSage") {

    override fun buildPrompt(context: EditorContext): String {
        val fileContext = EditorContextService.buildFileContextPrompt(context)
        val target = if (context.selectedText != null) "the selected code" else "this file"
        return "Suggest refactoring improvements for $target. " +
            "Focus on readability, maintainability, performance, and best practices for ${context.language}. " +
            "Provide the refactored code with explanations of each change.\n\n$fileContext"
    }
}
