package lol.vynnra.agent.data.memory

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import lol.vynnra.agent.core.memory.MemoryRecord
import lol.vynnra.agent.core.memory.MemoryRelevanceScorer
import lol.vynnra.agent.core.memory.toRecord
import lol.vynnra.agent.core.memory.toEntity
import lol.vynnra.agent.data.local.MemoryDao

class RoomMemoryRepository(
    private val dao: MemoryDao,
    private val clock: () -> Long = System::currentTimeMillis
) : MemoryRepository {
    override fun observeEnabled(): Flow<List<MemoryRecord>> =
        dao.observeEnabled().map { memories -> memories.map { it.toRecord() } }

    override suspend fun upsert(memory: MemoryRecord) {
        dao.upsert(memory.copy(
            importance = memory.importance.coerceIn(0f, 1f),
            updatedAt = clock()
        ).toEntity())
    }

    override suspend fun search(query: String, limit: Int): List<MemoryRecord> {
        val boundedLimit = limit.coerceIn(1, 100)
        val candidates = dao.search(query.trim(), boundedLimit * 2)
            .map { it.toRecord() }
        return candidates
            .sortedByDescending { MemoryRelevanceScorer.score(query, it, clock()) }
            .take(boundedLimit)
    }

    override suspend fun get(id: String): MemoryRecord? = dao.findById(id)?.toRecord()

    override suspend fun setEnabled(id: String, enabled: Boolean) {
        dao.setEnabled(id, enabled, clock())
    }

    override suspend fun setPinned(id: String, pinned: Boolean) {
        dao.setPinned(id, pinned, clock())
    }

    override suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    override suspend fun markAccessed(id: String) {
        dao.markAccessed(id, clock())
    }
}
