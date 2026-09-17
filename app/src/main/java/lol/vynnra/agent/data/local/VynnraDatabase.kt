package lol.vynnra.agent.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        MemoryEntity::class,
        AgentTaskEntity::class,
        AgentTaskStepEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class VynnraDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun memoryDao(): MemoryDao
    abstract fun agentTaskDao(): AgentTaskDao
}
