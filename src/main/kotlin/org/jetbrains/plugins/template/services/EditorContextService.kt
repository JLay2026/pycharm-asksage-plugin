package org.jetbrains.plugins.template.services

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

data class EditorContext(
    val fileName: String,
    val filePath: String,
    val language: String,
    val selectedText: String?,
    val fullContent: String,
    val selectionStartLine: Int?,
    val selectionEndLine: Int?,
)

object EditorContextService {

    fun getEditorContext(project: Project): EditorContext? {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return null
        val document = editor.document
        val virtualFile = editor.virtualFile ?: return null

        val selectedText = getSelectedText(editor)
        val selectionRange = getSelectionLineRange(editor)

        return EditorContext(
            fileName = virtualFile.name,
            filePath = virtualFile.path,
            language = detectLanguage(virtualFile),
            selectedText = selectedText,
            fullContent = document.text,
            selectionStartLine = selectionRange?.first,
            selectionEndLine = selectionRange?.second,
        )
    }

    fun getSelectedText(editor: Editor): String? {
        val selectionModel = editor.selectionModel
        val text = selectionModel.selectedText
        return if (text.isNullOrBlank()) null else text
    }

    private fun getSelectionLineRange(editor: Editor): Pair<Int, Int>? {
        val selectionModel = editor.selectionModel
        if (!selectionModel.hasSelection()) return null

        val document = editor.document
        val startLine = document.getLineNumber(selectionModel.selectionStart) + 1
        val endLine = document.getLineNumber(selectionModel.selectionEnd) + 1
        return Pair(startLine, endLine)
    }

    internal fun detectLanguage(file: VirtualFile): String {
        return detectLanguageFromFileName(file.name)
    }

    internal fun detectLanguageFromFileName(fileName: String): String {
        val extension = if ('.' in fileName) fileName.substringAfterLast('.').lowercase() else null
        return when (extension) {
            "kt", "kts" -> "Kotlin"
            "java" -> "Java"
            "py" -> "Python"
            "js" -> "JavaScript"
            "ts" -> "TypeScript"
            "tsx" -> "TypeScript (React)"
            "jsx" -> "JavaScript (React)"
            "go" -> "Go"
            "rs" -> "Rust"
            "c" -> "C"
            "cpp", "cc", "cxx" -> "C++"
            "cs" -> "C#"
            "rb" -> "Ruby"
            "php" -> "PHP"
            "swift" -> "Swift"
            "sql" -> "SQL"
            "xml" -> "XML"
            "json" -> "JSON"
            "yaml", "yml" -> "YAML"
            "html", "htm" -> "HTML"
            "css" -> "CSS"
            "scss" -> "SCSS"
            "sh", "bash" -> "Shell"
            "md" -> "Markdown"
            "gradle" -> "Gradle"
            "toml" -> "TOML"
            else -> extension ?: "Unknown"
        }
    }

    fun buildFileContextPrompt(context: EditorContext): String {
        val sb = StringBuilder()
        sb.appendLine("File: ${context.fileName} (${context.language})")
        sb.appendLine("Path: ${context.filePath}")

        if (context.selectedText != null) {
            sb.appendLine("Selected code (lines ${context.selectionStartLine}-${context.selectionEndLine}):")
            sb.appendLine("```${context.language.lowercase()}")
            sb.appendLine(context.selectedText)
            sb.appendLine("```")
        } else {
            sb.appendLine("Full file content:")
            sb.appendLine("```${context.language.lowercase()}")
            sb.appendLine(context.fullContent)
            sb.appendLine("```")
        }

        return sb.toString()
    }
}
