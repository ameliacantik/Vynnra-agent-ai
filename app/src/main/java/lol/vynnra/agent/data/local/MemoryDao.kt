package lol.vynnra.agent.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories WHERE enabled = 1 ORDER BY pinned DESC, importance DESC, updatedAt DESC")
    fun observeEnabled(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories ORDER BY pinned DESC, enabled DESC, importance DESC, updatedAt DESC")
    fun observeAll(): Flow<List<MemoryEntity>>

    @Query(
        "SELECT * FROM memories " +
            "WHERE enabled = 1 AND (content LIKE '%' || :query || '%' OR key LIKE '%' || :query || '%') " +
            "ORDER BY pinned DESC, importance DESC, updatedAt DESC LIMIT :limit"
    )
    suspend fun search(query: String, limit: Int): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(memory: MemoryEntity)

    @Query("UPDATE memories SET lastAccessedAt = :timestamp WHERE id = :id")
    suspend fun markAccessed(id: String, timestamp: Long)

    @Query("UPDATE memories SET enabled = :enabled, updatedAt = :timestamp WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean, timestamp: Long)

    @Query("UPDATE memories SET pinned = :pinned, updatedAt = :timestamp WHERE id = :id")
    suspend fun setPinned(id: String, pinned: Boolean, timestamp: Long)

    @Delete
    suspend fun delete(memory: MemoryEntity)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM memories WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): MemoryEntity?
}
