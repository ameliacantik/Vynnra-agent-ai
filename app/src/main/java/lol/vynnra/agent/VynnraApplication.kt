package lol.vynnra.agent

import android.app.Application
import androidx.room.Room
import lol.vynnra.agent.data.local.VynnraDatabase
import lol.vynnra.agent.data.local.VynnraDatabaseMigrations
import lol.vynnra.agent.data.memory.MemoryRepository
import lol.vynnra.agent.data.memory.RoomMemoryRepository
import lol.vynnra.agent.data.task.RoomTaskRepository
import lol.vynnra.agent.data.task.TaskRepository

class VynnraApplication : Application() {
    val database: VynnraDatabase by lazy {
        Room.databaseBuilder(this, VynnraDatabase::class.java, "vynnra-agent.db")
            .addMigrations(VynnraDatabaseMigrations.FROM_1_TO_2)
            .build()
    }

    val memoryRepository: MemoryRepository by lazy {
        RoomMemoryRepository(database.memoryDao())
    }

    val taskRepository: TaskRepository by lazy {
        RoomTaskRepository(database.agentTaskDao())
    }
}
