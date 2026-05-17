package org.jetbrains.plugins.template.actions

import org.jetbrains.plugins.template.services.EditorContext
import org.jetbrains.plugins.template.services.EditorContextService

class ExplainCodeAction : AskSageEditorAction("Explain Code") {

    override fun buildPrompt(context: EditorContext): String {
        val fileContext = EditorContextService.buildFileContextPrompt(context)
        val target = if (context.selectedText != null) "the selected code" else "this file"
        return "Explain $target in detail. What does it do, and how does it work?\n\n$fileContext"
    }
}
