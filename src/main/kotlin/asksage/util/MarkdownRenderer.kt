package asksage.util

import com.intellij.ui.JBColor
import java.awt.Font
import javax.swing.JTextPane
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import javax.swing.text.StyledDocument

object MarkdownRenderer {
    private val CODE_HEADER_BG = JBColor(0x282C34, 0x282C34)
    private val CODE_HEADER_FG = JBColor(0x969696, 0x969696)
    private val CODE_BLOCK_BG = JBColor(0xF5F5F5, 0x2B2B2B)
    private val CODE_BLOCK_FG = JBColor(0x323232, 0xC8C8C8)
    private val BLOCKQUOTE_FG = JBColor(0x646464, 0xA0A0A0)
    private val HR_COLOR = JBColor(0xC8C8C8, 0x505050)
    private val INLINE_CODE_BG = JBColor(0xF0F0F0, 0x323232)
    private val INLINE_CODE_FG = JBColor(0xC82828, 0xE67878)
    fun render(textPane: JTextPane, markdown: String) {
        val doc = textPane.styledDocument
        val lines = markdown.split("\n")
        var inCodeBlock = false
        var codeLanguage = ""
        val codeBuffer = StringBuilder()
        for ((index, line) in lines.withIndex()) {
            if (line.trimStart().startsWith("```")) {
                if (!inCodeBlock) {
                    inCodeBlock = true
                    codeLanguage = line.trimStart().removePrefix("```").trim()
                    codeBuffer.clear()
                } else {
                    inCodeBlock = false
                    appendCodeBlock(doc, codeBuffer.toString(), codeLanguage)
                    codeLanguage = ""
                }
                continue
            }
            if (inCodeBlock) {
                if (codeBuffer.isNotEmpty()) codeBuffer.append("\n")
                codeBuffer.append(line)
                continue
            }
            if (index > 0 || doc.length > 0) { doc.insertString(doc.length, "\n", null) }
            when {
                line.startsWith("### ") -> appendHeader(doc, line.removePrefix("### "), 3)
                line.startsWith("## ") -> appendHeader(doc, line.removePrefix("## "), 2)
                line.startsWith("# ") -> appendHeader(doc, line.removePrefix("# "), 1)
                line.startsWith("- ") || line.startsWith("* ") -> appendListItem(doc, line.substring(2))
                line.matches(Regex("^\\d+\\.\\s.*")) -> appendListItem(doc, line.replaceFirst(Regex("^\\d+\\.\\s"), ""), ordered = true)
                line.startsWith("> ") -> appendBlockquote(doc, line.removePrefix("> "))
                line.startsWith("---") || line.startsWith("***") -> appendHorizontalRule(doc)
                else -> appendInlineFormatted(doc, line)
            }
        }
        if (inCodeBlock && codeBuffer.isNotEmpty()) { appendCodeBlock(doc, codeBuffer.toString(), codeLanguage) }
    }
    private fun appendHeader(doc: StyledDocument, text: String, level: Int) {
        val attrs = SimpleAttributeSet()
        StyleConstants.setBold(attrs, true)
        val size = when (level) { 1 -> 18; 2 -> 16; else -> 14 }
        StyleConstants.setFontSize(attrs, size)
        StyleConstants.setForeground(attrs, JBColor.foreground())
        doc.insertString(doc.length, text, attrs)
    }
    private fun appendCodeBlock(doc: StyledDocument, code: String, language: String) {
        val headerAttrs = SimpleAttributeSet()
        StyleConstants.setBackground(headerAttrs, CODE_HEADER_BG)
        StyleConstants.setForeground(headerAttrs, CODE_HEADER_FG)
        StyleConstants.setFontSize(headerAttrs, 11)
        if (language.isNotBlank()) { doc.insertString(doc.length, "\n $language\n", headerAttrs) }
        else { doc.insertString(doc.length, "\n", null) }
        val codeAttrs = SimpleAttributeSet()
        StyleConstants.setFontFamily(codeAttrs, Font.MONOSPACED)
        StyleConstants.setFontSize(codeAttrs, 12)
        StyleConstants.setBackground(codeAttrs, CODE_BLOCK_BG)
        StyleConstants.setForeground(codeAttrs, CODE_BLOCK_FG)
        doc.insertString(doc.length, code, codeAttrs)
        doc.insertString(doc.length, "\n", null)
    }
    private fun appendListItem(doc: StyledDocument, text: String, ordered: Boolean = false) {
        val bulletAttrs = SimpleAttributeSet()
        StyleConstants.setForeground(bulletAttrs, JBColor.foreground())
        val bullet = if (ordered) "  " else "  bullet "
        doc.insertString(doc.length, bullet, bulletAttrs)
        appendInlineFormatted(doc, text)
    }
    private fun appendBlockquote(doc: StyledDocument, text: String) {
        val attrs = SimpleAttributeSet()
        StyleConstants.setItalic(attrs, true)
        StyleConstants.setForeground(attrs, BLOCKQUOTE_FG)
        StyleConstants.setLeftIndent(attrs, 16f)
        doc.insertString(doc.length, "line $text", attrs)
    }
    private fun appendHorizontalRule(doc: StyledDocument) {
        val attrs = SimpleAttributeSet()
        StyleConstants.setForeground(attrs, HR_COLOR)
        doc.insertString(doc.length, "dash".repeat(40), attrs)
    }
    fun appendInlineFormatted(doc: StyledDocument, text: String) {
        var i = 0
        val len = text.length
        while (i < len) {
            when {
                i + 1 < len && text[i] == '*' && text[i + 1] == '*' -> {
                    val end = text.indexOf("**", i + 2)
                    if (end > i) {
                        val attrs = SimpleAttributeSet()
                        StyleConstants.setBold(attrs, true)
                        doc.insertString(doc.length, text.substring(i + 2, end), attrs)
                        i = end + 2
                    } else {
                        doc.insertString(doc.length, text[i].toString(), null)
                        i++
                    }
                }
                text[i] == '*' -> {
                    val end = text.indexOf('*', i + 1)
                    if (end > i) {
                        val attrs = SimpleAttributeSet()
                        StyleConstants.setItalic(attrs, true)
                        doc.insertString(doc.length, text.substring(i + 1, end), attrs)
                        i = end + 1
                    } else {
                        doc.insertString(doc.length, text[i].toString(), null)
                        i++
                    }
                }
                text[i] == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end > i) {
                        val attrs = SimpleAttributeSet()
                        StyleConstants.setFontFamily(attrs, Font.MONOSPACED)
                        StyleConstants.setBackground(attrs, INLINE_CODE_BG)
                        StyleConstants.setForeground(attrs, INLINE_CODE_FG)
                        doc.insertString(doc.length, text.substring(i + 1, end), attrs)
                        i = end + 1
                    } else {
                        doc.insertString(doc.length, text[i].toString(), null)
                        i++
                    }
                }
                else -> {
                    doc.insertString(doc.length, text[i].toString(), null)
                    i++
                }
            }
        }
    }
}
