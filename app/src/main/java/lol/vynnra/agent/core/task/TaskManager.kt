package lol.vynnra.agent.core.task

import java.util.UUID
import lol.vynnra.agent.data.task.TaskRepository

class TaskManager(
    private val repository: TaskRepository,
    private val clock: () -> Long = System::currentTimeMillis
) {
    suspend fun createTask(
        title: String,
        goal: String,
        totalSteps: Int,
        checkpointJson: String? = null
    ): TaskRecord {
        val now = clock()
        val task = TaskRecord(
            id = UUID.randomUUID().toString(),
            title = title.trim().ifEmpty { "Vynnra task" },
            goal = goal.trim(),
            status = TaskStatus.PENDING,
            currentStep = 0,
            totalSteps = totalSteps.coerceAtLeast(0),
            checkpointJson = checkpointJson,
            lastError = null,
            requiresUserAction = false,
            createdAt = now,
            updatedAt = now,
            completedAt = null
        )
        repository.create(task)
        return task
    }

    suspend fun start(taskId: String) {
        repository.updateStatus(taskId, TaskStatus.RUNNING.name)
    }

    suspend fun pause(taskId: String, reason: String? = null) {
        repository.updateStatus(taskId, TaskStatus.PAUSED.name, reason)
    }

    suspend fun block(taskId: String, reason: String) {
        repository.updateStatus(taskId, TaskStatus.BLOCKED.name, reason)
    }

    suspend fun fail(taskId: String, error: String) {
        repository.updateStatus(taskId, TaskStatus.FAILED.name, error)
    }

    suspend fun cancel(taskId: String, reason: String? = null) {
        repository.updateStatus(taskId, TaskStatus.CANCELLED.name, reason)
    }

    suspend fun checkpoint(
        taskId: String,
        nextStep: Int,
        checkpointJson: String?,
        requiresUserAction: Boolean = false,
        status: TaskStatus = TaskStatus.RUNNING,
        lastError: String? = null,
        step: TaskStepRecord? = null
    ) {
        val task = repository.get(taskId) ?: error("Task not found: $taskId")
        repository.checkpoint(
            task.copy(
                status = status,
                currentStep = nextStep.coerceIn(0, task.totalSteps),
                checkpointJson = checkpointJson,
                requiresUserAction = requiresUserAction,
                lastError = lastError,
                updatedAt = clock()
            ),
            step
        )
    }

    suspend fun complete(taskId: String) {
        repository.markCompleted(taskId, clock())
    }

    suspend fun resumable(): List<TaskRecord> = repository.resumableTasks()
}
