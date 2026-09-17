package lol.vynnra.agent.core.memory

import lol.vynnra.agent.data.local.ChatDao
import lol.vynnra.agent.data.memory.MemoryRepository

/** Builds bounded, user-visible context without persisting hidden chain-of-thought. */
class ConversationContextAssembler(
    private val chatDao: ChatDao,
    private val memoryRepository: MemoryRepository
) {
    suspend fun assemble(
        sessionId: String,
        query: String,
        maxMessages: Int = 12,
        maxMemories: Int = 8
    ): ConversationContext {
        val messages = chatDao.recentMessages(sessionId, maxMessages.coerceIn(1, 50))
            .asReversed()
            .map { ConversationMessage(it.role, it.content, it.createdAt) }
        val memories = memoryRepository.search(query, maxMemories.coerceIn(1, 20))
        memories.forEach { memoryRepository.markAccessed(it.id) }
        return ConversationContext(
            sessionId = sessionId,
            recentMessages = messages,
            relevantMemories = memories
        )
    }
}

data class ConversationContext(
    val sessionId: String,
    val recentMessages: List<ConversationMessage>,
    val relevantMemories: List<MemoryRecord>
)

data class ConversationMessage(
    val role: String,
    val content: String,
    val createdAt: Long
)
