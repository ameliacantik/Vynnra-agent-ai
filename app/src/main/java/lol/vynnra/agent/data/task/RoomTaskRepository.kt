package lol.vynnra.agent.data.task

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import lol.vynnra.agent.core.task.TaskRecord
import lol.vynnra.agent.core.task.TaskStatus
import lol.vynnra.agent.core.task.TaskStepRecord
import lol.vynnra.agent.core.task.toEntity
import lol.vynnra.agent.core.task.toRecord
import lol.vynnra.agent.data.local.AgentTaskDao

class RoomTaskRepository(
    private val dao: AgentTaskDao,
    private val clock: () -> Long = System::currentTimeMillis
) : TaskRepository {
    override fun observeTasks(): Flow<List<TaskRecord>> =
        dao.observeTasks().map { tasks -> tasks.map { it.toRecord() } }

    override suspend fun create(task: TaskRecord) {
        dao.upsertTask(task.toEntity())
    }

    override suspend fun get(taskId: String): TaskRecord? = dao.findTask(taskId)?.toRecord()

    override suspend fun getSteps(taskId: String): List<TaskStepRecord> =
        dao.getSteps(taskId).map { it.toRecord() }

    override suspend fun upsertStep(step: TaskStepRecord) {
        dao.upsertStep(step.toEntity())
    }

    override suspend fun checkpoint(task: TaskRecord, step: TaskStepRecord?) {
        val now = clock()
        if (step != null) dao.upsertStep(step.toEntity())
        dao.checkpoint(
            taskId = task.id,
            currentStep = task.currentStep,
            checkpointJson = task.checkpointJson,
            timestamp = now,
            status = task.status.name,
            lastError = task.lastError,
            requiresUserAction = task.requiresUserAction
        )
    }

    override suspend fun markCompleted(taskId: String, timestamp: Long) {
        dao.markCompleted(taskId, timestamp)
    }

    override suspend fun updateStatus(taskId: String, status: String, error: String?) {
        val normalized = runCatching { TaskStatus.valueOf(status) }
            .getOrDefault(TaskStatus.FAILED)
            .name
        dao.updateStatus(taskId, normalized, clock(), error)
    }

    override suspend fun resumableTasks(): List<TaskRecord> =
        dao.findResumableTasks().map { it.toRecord() }

    override suspend fun delete(taskId: String) {
        dao.deleteTask(taskId)
    }
}
