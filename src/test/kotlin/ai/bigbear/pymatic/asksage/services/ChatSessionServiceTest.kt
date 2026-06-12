package ai.bigbear.pymatic.asksage.services

import org.junit.jupiter.api.Test
import ai.bigbear.pymatic.asksage.services.ChatMessage
import ai.bigbear.pymatic.asksage.services.MessageRole
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChatSessionServiceTest {

    @Test
    fun testChatMessageCreation() {
        val msg = ChatMessage(MessageRole.USER, "Hello")
        assertEquals(MessageRole.USER, msg.role)
        assertEquals("Hello", msg.content)
    }

    @Test
    fun testMessageRoleVariants() {
        assertTrue(MessageRole.entries.isNotEmpty())
    }
}
