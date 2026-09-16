package lol.vynnra.agent.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ChatSessionEntity::class, ChatMessageEntity::class],
    version = 1,
    exportSchema = true
)
abstract class VynnraDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
}
