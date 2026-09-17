package lol.vynnra.agent.data.task

import kotlinx.coroutines.flow.Flow
import lol.vynnra.agent.core.task.TaskRecord
import lol.vynnra.agent.core.task.TaskStepRecord

interface TaskRepository {
    fun observeTasks(): Flow<List<TaskRecord>>
    suspend fun create(task: TaskRecord)
    suspend fun get(taskId: String): TaskRecord?
    suspend fun getSteps(taskId: String): List<TaskStepRecord>
    suspend fun upsertStep(step: TaskStepRecord)
    suspend fun checkpoint(task: TaskRecord, step: TaskStepRecord?)
    suspend fun markCompleted(taskId: String, timestamp: Long)
    suspend fun updateStatus(taskId: String, status: String, error: String? = null)
    suspend fun resumableTasks(): List<TaskRecord>
    suspend fun delete(taskId: String)
}
