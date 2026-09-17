package lol.vynnra.agent.core.voice

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import lol.vynnra.agent.core.agent.ThinkingLevel
import lol.vynnra.agent.core.provider.AiMessage
import lol.vynnra.agent.core.provider.AiProvider
import lol.vynnra.agent.core.provider.AiRequest
import lol.vynnra.agent.core.provider.AiResponse
import lol.vynnra.agent.core.provider.AiStreamEvent
import lol.vynnra.agent.core.task.TaskRecord
import lol.vynnra.agent.core.task.TaskStepRecord
import lol.vynnra.agent.data.task.TaskRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceAgentSessionTest {
    @Test
    fun submit_persists_and_completes_voice_task() = kotlinx.coroutines.runBlocking {
        val repository = RecordingTaskRepository()
        val provider = FakeProvider("Hello from Vynnra")
        val session = VoiceAgentSession(
            provider = provider,
            taskRepository = repository,
            modelProvider = { "test-model" },
            clock = { 1_000L }
        )

        val result = session.submit("Say hello", ThinkingLevel.HIGH)

        assertTrue(result is VoiceAgentResult.Success)
        assertEquals("Say hello", repository.lastTask?.goal)
        assertEquals("COMPLETED", repository.lastTask?.status?.name)
        assertEquals("VERIFIED", repository.lastStep?.status?.name)
        assertEquals("Hello from Vynnra", (result as VoiceAgentResult.Success).text)
    }
}

private class FakeProvider(private val text: String) : AiProvider {
    override val id = "fake"
    override val displayName = "Fake"

    override suspend fun complete(request: AiRequest): AiResponse =
        AiResponse(content = text, model = request.model, finishReason = "stop")

    override fun stream(request: AiRequest): Flow<AiStreamEvent> = MutableStateFlow(
        AiStreamEvent.Completed(AiResponse(text, request.model, "stop"))
    )
}

private class RecordingTaskRepository : TaskRepository {
    private val tasks = mutableMapOf<String, TaskRecord>()
    private val steps = mutableMapOf<Pair<String, Int>, TaskStepRecord>()
    private val observed = MutableStateFlow(emptyList<TaskRecord>())

    var lastTask: TaskRecord? = null
        private set
    var lastStep: TaskStepRecord? = null
        private set

    override fun observeTasks() = observed

    override suspend fun create(task: TaskRecord) {
        tasks[task.id] = task
        observed.value = tasks.values.toList()
    }

    override suspend fun get(taskId: String) = tasks[taskId]

    override suspend fun getSteps(taskId: String) =
        steps.values.filter { it.taskId == taskId }.sortedBy { it.stepIndex }

    override suspend fun upsertStep(step: TaskStepRecord) {
        steps[step.taskId to step.stepIndex] = step
        lastStep = step
    }

    override suspend fun checkpoint(task: TaskRecord, step: TaskStepRecord?) {
        tasks[task.id] = task
        lastTask = task
        if (step != null) upsertStep(step)
        observed.value = tasks.values.toList()
    }

    override suspend fun markCompleted(taskId: String, timestamp: Long) {
        tasks[taskId]?.let { tasks[taskId] = it.copy(completedAt = timestamp) }
        lastTask = tasks[taskId]
        observed.value = tasks.values.toList()
    }

    override suspend fun updateStatus(taskId: String, status: String, error: String?) {
        tasks[taskId]?.let { tasks[taskId] = it.copy(lastError = error) }
        lastTask = tasks[taskId]
        observed.value = tasks.values.toList()
    }

    override suspend fun resumableTasks() = emptyList<TaskRecord>()

    override suspend fun delete(taskId: String) {
        tasks.remove(taskId)
        steps.keys.removeAll { it.first == taskId }
        observed.value = tasks.values.toList()
    }
}
