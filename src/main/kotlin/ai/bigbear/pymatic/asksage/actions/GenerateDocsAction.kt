package ai.bigbear.pymatic.asksage.actions

import ai.bigbear.pymatic.asksage.services.EditorContext
import ai.bigbear.pymatic.asksage.services.EditorContextService

class GenerateDocsAction : AskSageEditorAction("Generate Docs") {

    override fun buildPrompt(context: EditorContext): String {
        val fileContext = EditorContextService.buildFileContextPrompt(context)
        val target = if (context.selectedText != null) "the selected code" else "all public APIs in this file"
        return "Generate comprehensive documentation for $target. " +
            "Include parameter descriptions, return values, usage examples, and any important notes. " +
            "Use the standard documentation format for ${context.language}.\n\n$fileContext"
    }
}
