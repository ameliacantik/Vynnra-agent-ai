package lol.vynnra.agent.core.memory

import java.util.UUID
import lol.vynnra.agent.data.memory.MemoryRepository

class MemoryManager(
    private val repository: MemoryRepository,
    private val clock: () -> Long = System::currentTimeMillis
) {
    suspend fun rememberExplicitly(
        kind: MemoryKind,
        content: String,
        key: String? = null,
        sessionId: String? = null,
        importance: Float = 0.7f,
        sensitive: Boolean = false,
        pinned: Boolean = false
    ): MemoryRecord {
        val now = clock()
        val memory = MemoryRecord(
            id = UUID.randomUUID().toString(),
            kind = kind,
            key = key?.trim()?.takeIf { it.isNotEmpty() },
            content = content.trim(),
            sourceSessionId = sessionId,
            importance = importance.coerceIn(0f, 1f),
            sensitive = sensitive,
            enabled = true,
            pinned = pinned,
            createdAt = now,
            updatedAt = now,
            lastAccessedAt = null
        )
        require(memory.content.isNotEmpty()) { "Memory content cannot be empty." }
        repository.upsert(memory)
        return memory
    }

    suspend fun forget(id: String) {
        repository.delete(id)
    }

    suspend fun disable(id: String) {
        repository.setEnabled(id, false)
    }

    suspend fun enable(id: String) {
        repository.setEnabled(id, true)
    }

    suspend fun pin(id: String) {
        repository.setPinned(id, true)
    }

    suspend fun unpin(id: String) {
        repository.setPinned(id, false)
    }
}
