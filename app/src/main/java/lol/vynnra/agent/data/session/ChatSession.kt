package lol.vynnra.agent.data.session

import java.util.UUID

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Chat",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val role: Role,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    enum class Role { USER, ASSISTANT, SYSTEM, TOOL }
}
