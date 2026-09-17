package lol.vynnra.agent.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object VynnraDatabaseMigrations {
    val FROM_1_TO_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS memories (
                    id TEXT NOT NULL PRIMARY KEY,
                    kind TEXT NOT NULL,
                    `key` TEXT,
                    content TEXT NOT NULL,
                    sourceSessionId TEXT,
                    importance REAL NOT NULL,
                    sensitive INTEGER NOT NULL,
                    enabled INTEGER NOT NULL,
                    pinned INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    lastAccessedAt INTEGER
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_memories_kind ON memories(kind)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_memories_updatedAt ON memories(updatedAt)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_memories_enabled_pinned ON memories(enabled, pinned)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS agent_tasks (
                    id TEXT NOT NULL PRIMARY KEY,
                    title TEXT NOT NULL,
                    goal TEXT NOT NULL,
                    status TEXT NOT NULL,
                    currentStep INTEGER NOT NULL,
                    totalSteps INTEGER NOT NULL,
                    checkpointJson TEXT,
                    lastError TEXT,
                    requiresUserAction INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    completedAt INTEGER
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_agent_tasks_status ON agent_tasks(status)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_agent_tasks_updatedAt ON agent_tasks(updatedAt)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS agent_task_steps (
                    taskId TEXT NOT NULL,
                    stepIndex INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    status TEXT NOT NULL,
                    toolId TEXT,
                    inputJson TEXT,
                    outputSummary TEXT,
                    attempts INTEGER NOT NULL,
                    startedAt INTEGER,
                    completedAt INTEGER,
                    errorMessage TEXT,
                    PRIMARY KEY(taskId, stepIndex),
                    FOREIGN KEY(taskId) REFERENCES agent_tasks(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_agent_task_steps_taskId_status ON agent_task_steps(taskId, status)")
        }
    }
}
