package asksage.util

import junit.framework.TestCase

class MarkdownRendererTest : TestCase() {

    fun testRenderHeading() {
        val markdown = "# Heading"
        val rendered = MarkdownRenderer.render(markdown)
        assertTrue(rendered.contains("Heading"))
    }

    fun testRenderBold() {
        val markdown = "**bold text**"
        val rendered = MarkdownRenderer.render(markdown)
        assertTrue(rendered.contains("bold"))
    }

    fun testRenderItalic() {
        val markdown = "*italic text*"
        val rendered = MarkdownRenderer.render(markdown)
        assertTrue(rendered.contains("italic"))
    }

    fun testRenderCode() {
        val markdown = "`code`"
        val rendered = MarkdownRenderer.render(markdown)
        assertTrue(rendered.contains("code"))
    }

    fun testRenderCodeBlock() {
        val markdown = """```kotlin
fun main() {}
```"""
        val rendered = MarkdownRenderer.render(markdown)
        assertTrue(rendered.contains("main"))
    }

    fun testRenderLink() {
        val markdown = "[Google](https://google.com)"
        val rendered = MarkdownRenderer.render(markdown)
        assertTrue(rendered.contains("Google"))
    }

    fun testRenderList() {
        val markdown = """- Item 1
- Item 2
- Item 3"""
        val rendered = MarkdownRenderer.render(markdown)
        assertTrue(rendered.contains("Item"))
    }

    fun testRenderTable() {
        val markdown = """| Header 1 | Header 2 |
|----------|----------|
| Value 1  | Value 2  |"""
        val rendered = MarkdownRenderer.render(markdown)
        assertTrue(rendered.contains("Header"))
    }
}
