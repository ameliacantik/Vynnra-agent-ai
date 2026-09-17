package lol.vynnra.agent.core.task

import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import lol.vynnra.agent.data.task.TaskRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TaskManagerTest {
    @Test
    fun checkpoint_persists_progress_and_resume_state() = runBlocking {
        val repository = FakeTaskRepository()
        val clock = AtomicLong(100L)
        val manager = TaskManager(repository) { clock.incrementAndGet() }

        val task = manager.createTask("Demo", "Run three steps", 3)
        manager.start(task.id)
        manager.checkpoint(
            taskId = task.id,
            nextStep = 1,
            checkpointJson = "{\"cursor\":1}"
        )

        val persisted = repository.get(task.id)
        assertNotNull(persisted)
        assertEquals(TaskStatus.RUNNING, persisted!!.status)
        assertEquals(1, persisted.currentStep)
        assertEquals("{\"cursor\":1}", persisted.checkpointJson)
        assertEquals(1, manager.resumable().single().currentStep)
    }

    private class FakeTaskRepository : TaskRepository {
        private val tasks = linkedMapOf<String, TaskRecord>()
        private val steps = linkedMapOf<Pair<String, Int>, TaskStepRecord>()
        private val flow = MutableStateFlow<List<TaskRecord>>(emptyList())

        override fun observeTasks(): Flow<List<TaskRecord>> = flow

        override suspend fun create(task: TaskRecord) {
            tasks[task.id] = task
            publish()
        }

        override suspend fun get(taskId: String): TaskRecord? = tasks[taskId]

        override suspend fun getSteps(taskId: String): List<TaskStepRecord> =
            steps.values.filter { it.taskId == taskId }.sortedBy { it.stepIndex }

        override suspend fun upsertStep(step: TaskStepRecord) {
            steps[step.taskId to step.stepIndex] = step
        }

        override suspend fun checkpoint(task: TaskRecord, step: TaskStepRecord?) {
            tasks[task.id] = task
            if (step != null) steps[step.taskId to step.stepIndex] = step
            publish()
        }

        override suspend fun markCompleted(taskId: String, timestamp: Long) {
            tasks[taskId] = tasks.getValue(taskId).copy(
                status = TaskStatus.COMPLETED,
                updatedAt = timestamp,
                completedAt = timestamp
            )
            publish()
        }

        override suspend fun updateStatus(taskId: String, status: String, error: String?) {
            tasks[taskId] = tasks.getValue(taskId).copy(
                status = TaskStatus.valueOf(status),
                lastError = error
            )
            publish()
        }

        override suspend fun resumableTasks(): List<TaskRecord> = tasks.values.filter {
            it.status in setOf(
                TaskStatus.PENDING,
                TaskStatus.RUNNING,
                TaskStatus.PAUSED,
                TaskStatus.BLOCKED,
                TaskStatus.FAILED
            )
        }

        override suspend fun delete(taskId: String) {
            tasks.remove(taskId)
            publish()
        }

        private fun publish() {
            flow.value = tasks.values.sortedByDescending { it.updatedAt }
        }
    }
}
