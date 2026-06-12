package ai.bigbear.pymatic.asksage.util

import org.junit.jupiter.api.Test
import ai.bigbear.pymatic.asksage.util.MarkdownRenderer
import javax.swing.JTextPane
import kotlin.test.assertNotNull

class MarkdownRendererTest {

    @Test
    fun testMarkdownRendering() {
        val pane = JTextPane()
        val markdown = "# Hello\n\nThis is **bold** text."
        MarkdownRenderer.render(pane, markdown)
        assertNotNull(pane.text)
    }

    @Test
    fun testCodeBlockRendering() {
        val pane = JTextPane()
        val markdown = "```kotlin\nval x = 42\n```"
        MarkdownRenderer.render(pane, markdown)
        assertNotNull(pane.text)
    }

    @Test
    fun testListRendering() {
        val pane = JTextPane()
        val markdown = "- Item 1\n- Item 2\n- Item 3"
        MarkdownRenderer.render(pane, markdown)
        assertNotNull(pane.text)
    }
}
