package asksage.services

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ConversationExportTest : BasePlatformTestCase() {

    fun testExportToJson() {
        val session = ChatSession(sessionId = "test-123")
        session.messages.add(ChatMessage(role = "user", content = "Hello"))
        session.messages.add(ChatMessage(role = "assistant", content = "Hi there"))

        val exporter = ConversationExporter()
        val json = exporter.exportToJson(session)
        assertTrue(json.contains("Hello"))
        assertTrue(json.contains("Hi there"))
    }

    fun testExportToMarkdown() {
        val session = ChatSession(sessionId = "test-123")
        session.messages.add(ChatMessage(role = "user", content = "Hello"))
        session.messages.add(ChatMessage(role = "assistant", content = "Hi there"))

        val exporter = ConversationExporter()
        val markdown = exporter.exportToMarkdown(session)
        assertTrue(markdown.contains("Hello"))
        assertTrue(markdown.contains("Hi there"))
    }

    fun testExportToHtml() {
        val session = ChatSession(sessionId = "test-123")
        session.messages.add(ChatMessage(role = "user", content = "Hello"))

        val exporter = ConversationExporter()
        val html = exporter.exportToHtml(session)
        assertTrue(html.contains("Hello"))
        assertTrue(html.contains("<html"))
    }

    fun testExportToCsv() {
        val session = ChatSession(sessionId = "test-123")
        session.messages.add(ChatMessage(role = "user", content = "Hello"))
        session.messages.add(ChatMessage(role = "assistant", content = "Hi"))

        val exporter = ConversationExporter()
        val csv = exporter.exportToCsv(session)
        assertTrue(csv.contains("user"))
        assertTrue(csv.contains("Hello"))
    }

    fun testExportWithTimestamps() {
        val session = ChatSession(sessionId = "test-123")
        session.messages.add(ChatMessage(role = "user", content = "Hello", timestamp = 1234567890))

        val exporter = ConversationExporter()
        val json = exporter.exportToJson(session)
        assertTrue(json.contains("1234567890"))
    }

    fun testExportPreservesOrder() {
        val session = ChatSession(sessionId = "test-123")
        session.messages.add(ChatMessage(role = "user", content = "First"))
        session.messages.add(ChatMessage(role = "assistant", content = "Second"))
        session.messages.add(ChatMessage(role = "user", content = "Third"))

        val exporter = ConversationExporter()
        val json = exporter.exportToJson(session)
        val firstIndex = json.indexOf("First")
        val secondIndex = json.indexOf("Second")
        val thirdIndex = json.indexOf("Third")
        assertTrue(firstIndex < secondIndex && secondIndex < thirdIndex)
    }

    fun testExportEmptySession() {
        val session = ChatSession(sessionId = "empty")
        val exporter = ConversationExporter()
        val json = exporter.exportToJson(session)
        assertNotNull(json)
        assertTrue(json.contains("empty"))
    }

    fun testExportWithSpecialCharacters() {
        val session = ChatSession(sessionId = "test-123")
        session.messages.add(ChatMessage(role = "user", content = "Test \"quotes\" and <html>"))

        val exporter = ConversationExporter()
        val json = exporter.exportToJson(session)
        assertTrue(json.contains("quotes"))
    }
}
