package org.jetbrains.plugins.template.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class ChatSessionService(private val project: Project) {

    private val messages = mutableListOf<ChatMessage>()

    fun addMessage(message: ChatMessage) {
        messages.add(message)
    }

    fun getMessages(): List<ChatMessage> = messages.toList()

    fun clearHistory() {
        messages.clear()
    }

    /**
     * Returns conversation history formatted for the AskSage API context.
     * Includes only USER and ASSISTANT messages (not ERROR), limited to the
     * most recent [maxTurns] exchanges to keep context manageable.
     */
    fun getConversationContext(maxTurns: Int = MAX_CONTEXT_TURNS): String {
        val conversationMessages = messages.filter { it.role != MessageRole.ERROR }
        val recentMessages = if (conversationMessages.size > maxTurns * 2) {
            conversationMessages.takeLast(maxTurns * 2)
        } else {
            conversationMessages
        }

        if (recentMessages.isEmpty()) return ""

        return recentMessages.joinToString("\n\n") { msg ->
            val roleLabel = when (msg.role) {
                MessageRole.USER -> "User"
                MessageRole.ASSISTANT -> "Assistant"
                MessageRole.ERROR -> ""
            }
            "$roleLabel: ${msg.content}"
        }
    }

    companion object {
        private const val MAX_CONTEXT_TURNS = 10
    }
}

data class ChatMessage(
    val role: MessageRole,
    val content: String,
    val model: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
)

enum class MessageRole {
    USER, ASSISTANT, ERROR
}
