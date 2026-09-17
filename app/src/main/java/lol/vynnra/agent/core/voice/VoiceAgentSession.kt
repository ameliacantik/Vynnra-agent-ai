package lol.vynnra.agent.core.voice

import java.util.UUID
import lol.vynnra.agent.core.agent.ThinkingLevel
import lol.vynnra.agent.core.provider.AiMessage
import lol.vynnra.agent.core.provider.AiProvider
import lol.vynnra.agent.core.provider.AiRequest
import lol.vynnra.agent.core.provider.VynnraSystemPrompt
import lol.vynnra.agent.core.task.TaskRecord
import lol.vynnra.agent.core.task.TaskStatus
import lol.vynnra.agent.core.task.TaskStepRecord
import lol.vynnra.agent.core.task.TaskStepStatus
import lol.vynnra.agent.data.task.TaskRepository

class VoiceAgentSession(
    private val provider: AiProvider,
    private val taskRepository: TaskRepository,
    private val modelProvider: () -> String,
    private val clock: () -> Long = System::currentTimeMillis
) {
    suspend fun submit(transcript: String, thinkingLevel: ThinkingLevel): VoiceAgentResult {
        val goal = transcript.trim()
        if (goal.isBlank()) return VoiceAgentResult.Failed("Voice input was empty.")

        val now = clock()
        val taskId = UUID.randomUUID().toString()
        val task = TaskRecord(
            id = taskId,
            title = goal.take(80),
            goal = goal,
            status = TaskStatus.RUNNING,
            currentStep = 0,
            totalSteps = 1,
            checkpointJson = "{\"source\":\"voice\",\"step\":0}",
            lastError = null,
            requiresUserAction = false,
            createdAt = now,
            updatedAt = now,
            completedAt = null
        )
        val step = TaskStepRecord(
            taskId = taskId,
            stepIndex = 0,
            title = "Voice AI response",
            status = TaskStepStatus.RUNNING,
            toolId = "provider.chat.completion",
            inputJson = goal.take(4_000),
            outputSummary = null,
            attempts = 1,
            startedAt = now,
            completedAt = null,
            errorMessage = null
        )
        taskRepository.checkpoint(task, step)

        return try {
            val model = modelProvider().trim()
            require(model.isNotEmpty()) { "AI model is not configured." }
            val response = provider.complete(
                AiRequest(
                    model = model,
                    messages = listOf(
                        AiMessage(AiMessage.Role.SYSTEM, VynnraSystemPrompt.value),
                        AiMessage(AiMessage.Role.USER, goal)
                    )
                )
            )
            val completedAt = clock()
            taskRepository.checkpoint(
                task.copy(
                    status = TaskStatus.COMPLETED,
                    currentStep = 1,
                    updatedAt = completedAt,
                    completedAt = completedAt,
                    checkpointJson = "{\"source\":\"voice\",\"step\":1}"
                ),
                step.copy(
                    status = TaskStepStatus.VERIFIED,
                    outputSummary = response.content.take(2_000),
                    completedAt = completedAt
                )
            )
            VoiceAgentResult.Success(response.content, response.model)
        } catch (error: Throwable) {
            val failedAt = clock()
            val message = error.message ?: "Voice AI request failed."
            taskRepository.checkpoint(
                task.copy(
                    status = TaskStatus.FAILED,
                    updatedAt = failedAt,
                    lastError = message,
                    checkpointJson = "{\"source\":\"voice\",\"step\":0}"
                ),
                step.copy(
                    status = TaskStepStatus.FAILED,
                    completedAt = failedAt,
                    errorMessage = message
                )
            )
            VoiceAgentResult.Failed(message)
        }
    }
}

sealed interface VoiceAgentResult {
    data class Success(val text: String, val model: String) : VoiceAgentResult
    data class Failed(val message: String) : VoiceAgentResult
}
