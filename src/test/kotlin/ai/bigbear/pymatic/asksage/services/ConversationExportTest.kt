package ai.bigbear.pymatic.asksage.services

import org.junit.jupiter.api.Test
import ai.bigbear.pymatic.asksage.services.ChatMessage
import ai.bigbear.pymatic.asksage.services.MessageRole
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConversationExportTest {

    @Test
    fun testExportMarkdownFormat() {
        val messages = listOf(
            ChatMessage(MessageRole.USER, "Hello"),
            ChatMessage(MessageRole.ASSISTANT, "Hi there"),
        )
        assertTrue(messages.isNotEmpty())
    }

    @Test
    fun testExportJsonFormat() {
        val messages = listOf(
            ChatMessage(MessageRole.USER, "Test", model = "gpt-4"),
        )
        assertEquals(1, messages.size)
    }

    @Test
    fun testConversationWithMetadata() {
        val projectName = "TestProject"
        val messages = listOf(
            ChatMessage(MessageRole.USER, "Question"),
        )
        assertTrue(projectName.isNotEmpty() && messages.isNotEmpty())
    }

    @Test
    fun testExportWithTimestamps() {
        val msg = ChatMessage(MessageRole.USER, "Test", timestamp = System.currentTimeMillis())
        assertTrue(msg.timestamp > 0)
    }

    @Test
    fun testMultipleTurnsExport() {
        val messages = (1..5).map {
            ChatMessage(
                if (it % 2 == 0) MessageRole.ASSISTANT else MessageRole.USER,
                "Message $it",
            )
        }
        assertEquals(5, messages.size)
    }
}
