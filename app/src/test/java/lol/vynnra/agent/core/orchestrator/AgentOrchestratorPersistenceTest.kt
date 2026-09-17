package lol.vynnra.agent.core.orchestrator

import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import lol.vynnra.agent.core.agent.ThinkingLevel
import lol.vynnra.agent.core.memory.MemoryKind
import lol.vynnra.agent.core.memory.MemoryRecord
import lol.vynnra.agent.core.security.CapabilityGate
import lol.vynnra.agent.core.task.TaskRecord
import lol.vynnra.agent.core.task.TaskStepRecord
import lol.vynnra.agent.core.task.TaskStatus
import lol.vynnra.agent.data.memory.MemoryRepository
import lol.vynnra.agent.data.task.TaskRepository
import lol.vynnra.agent.core.tool.RiskLevel
import lol.vynnra.agent.core.tool.ToolDefinition
import lol.vynnra.agent.core.tool.ToolResult
import lol.vynnra.agent.core.tool.ToolResultStatus

class AgentOrchestratorPersistenceTest {
    @Test
    fun run_persists_task_checkpoints_and_uses_relevant_memory() = runBlocking {
        val memory = FakeMemoryRepository(
            MemoryRecord(
                id = "m1",
                kind = MemoryKind.PREFERENCE,
                key = "theme",
                content = "Use AMOLED dark UI",
                sourceSessionId = null,
                importance = 1f,
                sensitive = false,
                enabled = true,
                pinned = true,
                createdAt = 1L,
                updatedAt = 1L,
                lastAccessedAt = null
            )
        )
        val tasks = FakeTaskRepository()
        var plannedGoal = ""
        val planner = object : AgentPlanner {
            override suspend fun createPlan(goal: String, thinkingLevel: ThinkingLevel): AgentPlan {
                plannedGoal = goal
                return AgentPlan(
                    runId = "planner",
                    goal = goal,
                    actions = listOf(
                        AgentAction("a1", "test.tool", mapOf("value" to "ok"), requiresVerification = false)
                    ),
                    thinkingLevel = thinkingLevel
                )
            }
        }
        val registry = ToolRegistry().apply {
            register(object : RegisteredTool {
                override val definition = ToolDefinition(
                    id = "test.tool",
                    name = "Test tool",
                    description = "test",
                    requiredCapabilities = emptySet(),
                    riskLevel = RiskLevel.LOW,
                    supportsVerification = false
                )

                override suspend fun execute(input: Map<String, Any?>): ToolResult =
                    ToolResult(ToolResultStatus.SUCCESS, message = "done")
            })
        }

        val result = AgentOrchestrator(
            planner = planner,
            registry = registry,
            capabilityGate = CapabilityGate { emptySet() },
            memoryRepository = memory,
            taskRepository = tasks
        ).run("Set up the theme")

        assertTrue(result is OrchestratorResult.Completed)
        assertTrue(plannedGoal.contains("Use AMOLED dark UI"))
        assertEquals(1, tasks.created.size)
        assertEquals(TaskStatus.COMPLETED.name, tasks.created.single().status.name)
        assertTrue(tasks.steps.any { it.status.name == "RUNNING" })
        assertTrue(tasks.steps.any { it.status.name == "VERIFIED" })
        assertEquals(1, tasks.completedTaskIds.size)
    }

    @Test
    fun resume_pending_tasks_reuses_task_and_skips_verified_checkpoint() = runBlocking {
        val tasks = FakeTaskRepository()
        val persisted = TaskRecord(
            id = "persisted-task",
            title = "Resume me",
            goal = "Continue persisted work",
            status = TaskStatus.PAUSED,
            currentStep = 1,
            totalSteps = 2,
            checkpointJson = "{\"runId\":\"old\",\"actionIndex\":1}",
            lastError = null,
            requiresUserAction = false,
            createdAt = 1L,
            updatedAt = 2L,
            completedAt = null
        )
        tasks.created += persisted

        val executed = CopyOnWriteArrayList<String>()
        val planner = object : AgentPlanner {
            override suspend fun createPlan(goal: String, thinkingLevel: ThinkingLevel): AgentPlan =
                AgentPlan(
                    runId = "planner",
                    goal = goal,
                    actions = listOf(
                        AgentAction("first", "resume.tool", mapOf("value" to "first")),
                        AgentAction("second", "resume.tool", mapOf("value" to "second"))
                    ),
                    thinkingLevel = thinkingLevel
                )
        }
        val registry = ToolRegistry().apply {
            register(object : RegisteredTool {
                override val definition = ToolDefinition(
                    id = "resume.tool",
                    name = "Resume tool",
                    description = "resume test",
                    requiredCapabilities = emptySet(),
                    riskLevel = RiskLevel.LOW,
                    supportsVerification = false
                )

                override suspend fun execute(input: Map<String, Any?>): ToolResult {
                    executed += input["value"].toString()
                    return ToolResult(ToolResultStatus.SUCCESS, message = input["value"].toString())
                }
            })
        }

        val results = AgentOrchestrator(
            planner = planner,
            registry = registry,
            capabilityGate = CapabilityGate { emptySet() },
            taskRepository = tasks
        ).resumePendingTasks()

        assertEquals(1, results.size)
        assertTrue(results.single() is OrchestratorResult.Completed)
        assertEquals(listOf("second"), executed.toList())
        assertEquals(1, tasks.created.size)
        assertEquals(persisted.id, tasks.created.single().id)
        assertEquals(TaskStatus.COMPLETED, tasks.created.single().status)
        assertEquals(1, tasks.completedTaskIds.size)
        assertTrue(tasks.created.single().checkpointJson?.contains("actionIndex") == true)
    }
}

private class FakeMemoryRepository(private val memory: MemoryRecord) : MemoryRepository {
    override fun observeEnabled(): Flow<List<MemoryRecord>> = emptyFlow()
    override fun observeAll(): Flow<List<MemoryRecord>> = emptyFlow()
    override suspend fun upsert(memory: MemoryRecord) = Unit
    override suspend fun search(query: String, limit: Int): List<MemoryRecord> = listOf(memory)
    override suspend fun get(id: String): MemoryRecord? = memory.takeIf { it.id == id }
    override suspend fun setEnabled(id: String, enabled: Boolean) = Unit
    override suspend fun setPinned(id: String, pinned: Boolean) = Unit
    override suspend fun delete(id: String) = Unit
    override suspend fun markAccessed(id: String) = Unit
}

private class FakeTaskRepository : TaskRepository {
    val created = CopyOnWriteArrayList<TaskRecord>()
    val steps = CopyOnWriteArrayList<TaskStepRecord>()
    val completedTaskIds = CopyOnWriteArrayList<String>()

    override fun observeTasks(): Flow<List<TaskRecord>> = emptyFlow()
    override suspend fun create(task: TaskRecord) { created += task }
    override suspend fun get(taskId: String): TaskRecord? = created.lastOrNull { it.id == taskId }
    override suspend fun getSteps(taskId: String): List<TaskStepRecord> = steps.filter { it.taskId == taskId }
    override suspend fun upsertStep(step: TaskStepRecord) { steps += step }
    override suspend fun checkpoint(task: TaskRecord, step: TaskStepRecord?) {
        val previous = created.indexOfLast { it.id == task.id }
        if (previous >= 0) created[previous] = task else created += task
        if (step != null) steps += step
    }
    override suspend fun markCompleted(taskId: String, timestamp: Long) {
        completedTaskIds += taskId
        val index = created.indexOfLast { it.id == taskId }
        if (index >= 0) created[index] = created[index].copy(status = TaskStatus.COMPLETED, completedAt = timestamp)
    }
    override suspend fun updateStatus(taskId: String, status: String, error: String?) {
        val index = created.indexOfLast { it.id == taskId }
        if (index >= 0) created[index] = created[index].copy(status = TaskStatus.valueOf(status), lastError = error)
    }
    override suspend fun resumableTasks(): List<TaskRecord> = created.filter { it.status != TaskStatus.COMPLETED }
    override suspend fun delete(taskId: String) { created.removeIf { it.id == taskId } }
}
