package asksage.services

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ChatSessionServiceTest : BasePlatformTestCase() {

    private lateinit var chatService: ChatSessionService

    override fun setUp() {
        super.setUp()
        chatService = project.getService(ChatSessionService::class.java)
        chatService.clearHistory()
    }

    fun testAddAndGetMessages() {
        chatService.addMessage(ChatMessage(MessageRole.USER, "Hello"))
        chatService.addMessage(ChatMessage(MessageRole.ASSISTANT, "Hi there", "gpt-4"))

        val messages = chatService.getMessages()
        assertEquals(2, messages.size)
        assertEquals(MessageRole.USER, messages[0].role)
        assertEquals("Hello", messages[0].content)
        assertEquals(MessageRole.ASSISTANT, messages[1].role)
        assertEquals("gpt-4", messages[1].model)
    }

    fun testClearHistory() {
        chatService.addMessage(ChatMessage(MessageRole.USER, "Test"))
        assertEquals(1, chatService.getMessages().size)

        chatService.clearHistory()
        assertEquals(0, chatService.getMessages().size)
    }

    fun testGetConversationContextEmpty() {
        val context = chatService.getConversationContext()
        assertEquals("", context)
    }

    fun testGetConversationContextFiltersErrors() {
        chatService.addMessage(ChatMessage(MessageRole.USER, "Hello"))
        chatService.addMessage(ChatMessage(MessageRole.ERROR, "Network error"))
        chatService.addMessage(ChatMessage(MessageRole.ASSISTANT, "Hi"))

        val context = chatService.getConversationContext()
        assertTrue(context.contains("User: Hello"))
        assertTrue(context.contains("Assistant: Hi"))
        assertFalse(context.contains("Network error"))
    }

    fun testGetConversationContextLimitsHistory() {
        for (i in 1..25) {
            chatService.addMessage(ChatMessage(MessageRole.USER, "Q$i"))
            chatService.addMessage(ChatMessage(MessageRole.ASSISTANT, "A$i"))
        }

        val context = chatService.getConversationContext(maxTurns = 5)
        assertFalse(context.contains("Q1"))
        assertTrue(context.contains("Q25"))
    }

    fun testGetMessagesReturnsCopy() {
        chatService.addMessage(ChatMessage(MessageRole.USER, "Test"))
        val messages = chatService.getMessages()
        assertEquals(1, messages.size)

        chatService.addMessage(ChatMessage(MessageRole.USER, "Test2"))
        assertEquals(1, messages.size)
        assertEquals(2, chatService.getMessages().size)
    }
}
