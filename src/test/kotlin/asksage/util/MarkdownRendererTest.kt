package asksage.util

import junit.framework.TestCase
import javax.swing.JTextPane

class MarkdownRendererTest : TestCase() {

    fun testRenderPlainText() {
        val pane = JTextPane()
        MarkdownRenderer.render(pane, "Hello world")
        val text = pane.styledDocument.getText(0, pane.styledDocument.length)
        assertTrue(text.contains("Hello world"))
    }

    fun testRenderHeader() {
        val pane = JTextPane()
        MarkdownRenderer.render(pane, "# Title")
        val text = pane.styledDocument.getText(0, pane.styledDocument.length)
        assertTrue(text.contains("Title"))
    }

    fun testRenderCodeBlock() {
        val pane = JTextPane()
        MarkdownRenderer.render(pane, "```kotlin\nfun main() {}\n```")
        val text = pane.styledDocument.getText(0, pane.styledDocument.length)
        assertTrue(text.contains("fun main() {}"))
    }

    fun testRenderBulletList() {
        val pane = JTextPane()
        MarkdownRenderer.render(pane, "- Item 1\n- Item 2")
        val text = pane.styledDocument.getText(0, pane.styledDocument.length)
        assertTrue(text.contains("Item 1"))
        assertTrue(text.contains("Item 2"))
    }

    fun testRenderBlockquote() {
        val pane = JTextPane()
        MarkdownRenderer.render(pane, "> Quote text")
        val text = pane.styledDocument.getText(0, pane.styledDocument.length)
        assertTrue(text.contains("Quote text"))
    }

    fun testRenderHorizontalRule() {
        val pane = JTextPane()
        MarkdownRenderer.render(pane, "---")
        val text = pane.styledDocument.getText(0, pane.styledDocument.length)
        assertTrue(text.contains("─"))
    }

    fun testRenderMultipleHeaders() {
        val pane = JTextPane()
        MarkdownRenderer.render(pane, "# H1\n## H2\n### H3")
        val text = pane.styledDocument.getText(0, pane.styledDocument.length)
        assertTrue(text.contains("H1"))
        assertTrue(text.contains("H2"))
        assertTrue(text.contains("H3"))
    }

    fun testRenderEmptyString() {
        val pane = JTextPane()
        MarkdownRenderer.render(pane, "")
        val text = pane.styledDocument.getText(0, pane.styledDocument.length)
        assertEquals("", text.trim())
    }

    fun testRenderUnclosedCodeBlock() {
        val pane = JTextPane()
        MarkdownRenderer.render(pane, "```\ncode without closing")
        val text = pane.styledDocument.getText(0, pane.styledDocument.length)
        assertTrue(text.contains("code without closing"))
    }
}
