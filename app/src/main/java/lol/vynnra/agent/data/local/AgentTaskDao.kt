package lol.vynnra.agent.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentTaskDao {
    @Query("SELECT * FROM agent_tasks ORDER BY updatedAt DESC")
    fun observeTasks(): Flow<List<AgentTaskEntity>>

    @Query("SELECT * FROM agent_tasks WHERE id = :taskId LIMIT 1")
    suspend fun findTask(taskId: String): AgentTaskEntity?

    @Query(
        "SELECT * FROM agent_tasks " +
            "WHERE status IN ('PENDING', 'RUNNING', 'PAUSED', 'BLOCKED', 'FAILED') " +
            "ORDER BY updatedAt DESC"
    )
    suspend fun findResumableTasks(): List<AgentTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: AgentTaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStep(step: AgentTaskStepEntity)

    @Query("SELECT * FROM agent_task_steps WHERE taskId = :taskId ORDER BY stepIndex ASC")
    suspend fun getSteps(taskId: String): List<AgentTaskStepEntity>

    @Query("SELECT * FROM agent_task_steps WHERE taskId = :taskId AND stepIndex = :stepIndex LIMIT 1")
    suspend fun getStep(taskId: String, stepIndex: Int): AgentTaskStepEntity?

    @Query("DELETE FROM agent_tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: String)

    @Query("UPDATE agent_tasks SET status = :status, updatedAt = :timestamp, lastError = :lastError WHERE id = :taskId")
    suspend fun updateStatus(taskId: String, status: String, timestamp: Long, lastError: String?)

    @Query(
        "UPDATE agent_tasks SET currentStep = :currentStep, checkpointJson = :checkpointJson, " +
            "updatedAt = :timestamp, status = :status, lastError = :lastError, " +
            "requiresUserAction = :requiresUserAction WHERE id = :taskId"
    )
    suspend fun checkpoint(
        taskId: String,
        currentStep: Int,
        checkpointJson: String?,
        timestamp: Long,
        status: String,
        lastError: String?,
        requiresUserAction: Boolean
    )

    @Query("UPDATE agent_tasks SET completedAt = :timestamp, updatedAt = :timestamp, status = 'COMPLETED', requiresUserAction = 0 WHERE id = :taskId")
    suspend fun markCompleted(taskId: String, timestamp: Long)

    @Transaction
    suspend fun saveCheckpoint(
        task: AgentTaskEntity,
        step: AgentTaskStepEntity,
        timestamp: Long
    ) {
        upsertStep(step)
        checkpoint(
            taskId = task.id,
            currentStep = task.currentStep,
            checkpointJson = task.checkpointJson,
            timestamp = timestamp,
            status = task.status,
            lastError = task.lastError,
            requiresUserAction = task.requiresUserAction
        )
    }
}
