package lol.vynnra.agent.core.task

import lol.vynnra.agent.data.local.AgentTaskEntity
import lol.vynnra.agent.data.local.AgentTaskStepEntity

enum class TaskStatus {
    PENDING,
    RUNNING,
    PAUSED,
    BLOCKED,
    FAILED,
    COMPLETED,
    CANCELLED
}

enum class TaskStepStatus {
    PENDING,
    RUNNING,
    VERIFIED,
    FAILED,
    BLOCKED,
    CANCELLED
}

data class TaskRecord(
    val id: String,
    val title: String,
    val goal: String,
    val status: TaskStatus,
    val currentStep: Int,
    val totalSteps: Int,
    val checkpointJson: String?,
    val lastError: String?,
    val requiresUserAction: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long?
)

data class TaskStepRecord(
    val taskId: String,
    val stepIndex: Int,
    val title: String,
    val status: TaskStepStatus,
    val toolId: String?,
    val inputJson: String?,
    val outputSummary: String?,
    val attempts: Int,
    val startedAt: Long?,
    val completedAt: Long?,
    val errorMessage: String?
)

fun TaskRecord.toEntity(): AgentTaskEntity = AgentTaskEntity(
    id = id,
    title = title,
    goal = goal,
    status = status.name,
    currentStep = currentStep,
    totalSteps = totalSteps,
    checkpointJson = checkpointJson,
    lastError = lastError,
    requiresUserAction = requiresUserAction,
    createdAt = createdAt,
    updatedAt = updatedAt,
    completedAt = completedAt
)

fun AgentTaskEntity.toRecord(): TaskRecord = TaskRecord(
    id = id,
    title = title,
    goal = goal,
    status = runCatching { TaskStatus.valueOf(status) }.getOrDefault(TaskStatus.FAILED),
    currentStep = currentStep,
    totalSteps = totalSteps,
    checkpointJson = checkpointJson,
    lastError = lastError,
    requiresUserAction = requiresUserAction,
    createdAt = createdAt,
    updatedAt = updatedAt,
    completedAt = completedAt
)

fun TaskStepRecord.toEntity(): AgentTaskStepEntity = AgentTaskStepEntity(
    taskId = taskId,
    stepIndex = stepIndex,
    title = title,
    status = status.name,
    toolId = toolId,
    inputJson = inputJson,
    outputSummary = outputSummary,
    attempts = attempts,
    startedAt = startedAt,
    completedAt = completedAt,
    errorMessage = errorMessage
)

fun AgentTaskStepEntity.toRecord(): TaskStepRecord = TaskStepRecord(
    taskId = taskId,
    stepIndex = stepIndex,
    title = title,
    status = runCatching { TaskStepStatus.valueOf(status) }.getOrDefault(TaskStepStatus.FAILED),
    toolId = toolId,
    inputJson = inputJson,
    outputSummary = outputSummary,
    attempts = attempts,
    startedAt = startedAt,
    completedAt = completedAt,
    errorMessage = errorMessage
)
