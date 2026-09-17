package lol.vynnra.agent.data.memory

import kotlinx.coroutines.flow.Flow
import lol.vynnra.agent.core.memory.MemoryRecord

interface MemoryRepository {
    fun observeEnabled(): Flow<List<MemoryRecord>>
    fun observeAll(): Flow<List<MemoryRecord>>
    suspend fun upsert(memory: MemoryRecord)
    suspend fun search(query: String, limit: Int = 20): List<MemoryRecord>
    suspend fun get(id: String): MemoryRecord?
    suspend fun setEnabled(id: String, enabled: Boolean)
    suspend fun setPinned(id: String, pinned: Boolean)
    suspend fun delete(id: String)
    suspend fun markAccessed(id: String)
}
