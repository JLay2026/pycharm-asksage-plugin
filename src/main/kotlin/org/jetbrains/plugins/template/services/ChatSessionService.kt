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
