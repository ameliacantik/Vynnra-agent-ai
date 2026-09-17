package lol.vynnra.agent.core.memory

import lol.vynnra.agent.data.local.MemoryEntity

enum class MemoryKind {
    FACT,
    PREFERENCE,
    PROFILE,
    INSTRUCTION,
    CONTEXT
}

data class MemoryRecord(
    val id: String,
    val kind: MemoryKind,
    val key: String?,
    val content: String,
    val sourceSessionId: String?,
    val importance: Float,
    val sensitive: Boolean,
    val enabled: Boolean,
    val pinned: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val lastAccessedAt: Long?
)

fun MemoryRecord.toEntity(): MemoryEntity = MemoryEntity(
    id = id,
    kind = kind.name,
    key = key,
    content = content,
    sourceSessionId = sourceSessionId,
    importance = importance.coerceIn(0f, 1f),
    sensitive = sensitive,
    enabled = enabled,
    pinned = pinned,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastAccessedAt = lastAccessedAt
)

fun MemoryEntity.toRecord(): MemoryRecord = MemoryRecord(
    id = id,
    kind = runCatching { MemoryKind.valueOf(kind) }.getOrDefault(MemoryKind.CONTEXT),
    key = key,
    content = content,
    sourceSessionId = sourceSessionId,
    importance = importance,
    sensitive = sensitive,
    enabled = enabled,
    pinned = pinned,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastAccessedAt = lastAccessedAt
)

object MemoryRelevanceScorer {
    fun score(query: String, memory: MemoryRecord, now: Long): Float {
        val queryTokens = tokenize(query)
        if (queryTokens.isEmpty()) return 0f

        val memoryTokens = tokenize(listOfNotNull(memory.key, memory.content).joinToString(" "))
        val overlap = queryTokens.intersect(memoryTokens).size.toFloat() / queryTokens.size.toFloat()
        val ageHours = ((now - memory.updatedAt).coerceAtLeast(0L) / 3_600_000L).toFloat()
        val freshness = 1f / (1f + ageHours / 24f)
        val importance = memory.importance.coerceIn(0f, 1f)
        val pinnedBoost = if (memory.pinned) 0.15f else 0f
        return (overlap * 0.65f + importance * 0.25f + freshness * 0.10f + pinnedBoost).coerceAtMost(1f)
    }

    private fun tokenize(text: String): Set<String> =
        text.lowercase()
            .split(Regex("[^\\p{L}\\p{Nd}]+"))
            .filter { it.length >= 2 }
            .toSet()
}
