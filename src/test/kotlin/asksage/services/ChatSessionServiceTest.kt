package asksage.services

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ChatSessionServiceTest : BasePlatformTestCase() {

    fun testSessionCreation() {
        val session = ChatSession(sessionId = "test-123")
        assertEquals("test-123", session.sessionId)
        assertNotNull(session.messages)
    }

    fun testAddMessage() {
        val session = ChatSession(sessionId = "test-123")
        val message = ChatMessage(role = "user", content = "Hello")
        session.messages.add(message)
        assertEquals(1, session.messages.size)
        assertEquals("Hello", session.messages[0].content)
    }

    fun testSessionTimestamp() {
        val session = ChatSession(sessionId = "test-123")
        assertNotNull(session.createdAt)
    }
}
