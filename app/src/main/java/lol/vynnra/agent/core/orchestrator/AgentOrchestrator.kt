package lol.vynnra.agent.core.orchestrator

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.ensureActive
import java.util.UUID
import lol.vynnra.agent.core.agent.AgentStatus
import lol.vynnra.agent.core.agent.ThinkingLevel
import lol.vynnra.agent.core.memory.MemoryRepositoryBridge
import lol.vynnra.agent.core.security.CapabilityGate
import lol.vynnra.agent.core.security.GateResult
import lol.vynnra.agent.core.task.TaskRecord
import lol.vynnra.agent.core.task.TaskStatus
import lol.vynnra.agent.core.task.TaskStepRecord
import lol.vynnra.agent.core.task.TaskStepStatus
import lol.vynnra.agent.core.tool.ToolResultStatus
import lol.vynnra.agent.data.memory.MemoryRepository
import lol.vynnra.agent.data.task.TaskRepository

class AgentOrchestrator(
    private val planner: AgentPlanner,
    private val registry: ToolRegistry,
    private val capabilityGate: CapabilityGate,
    private val verifier: VerificationEngine = DefaultVerificationEngine(),
    private val recovery: RecoveryEngine = BoundedRecoveryEngine(),
    private val journal: ActionJournal = ActionJournal(),
    private val memoryRepository: MemoryRepository? = null,
    private val taskRepository: TaskRepository? = null,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val _state = MutableStateFlow(OrchestratorState())
    val state: StateFlow<OrchestratorState> = _state.asStateFlow()

    @Volatile
    private var stopRequested = false

    suspend fun run(goal: String, thinkingLevel: ThinkingLevel = ThinkingLevel.MAX): OrchestratorResult {
        stopRequested = false
        val runId = UUID.randomUUID().toString()
        var task = createTask(goal, runId)
        taskRepository?.create(task)
        update(runId, AgentStatus.UNDERSTANDING, "Understanding request")

        return try {
            val plan = createPlan(goal, thinkingLevel, runId)
            task = task.copy(
                status = TaskStatus.RUNNING,
                totalSteps = plan.actions.size,
                updatedAt = clock()
            )
            taskRepository?.checkpoint(task, null)

            if (plan.actions.isEmpty()) {
                update(runId, AgentStatus.COMPLETED, "No executable actions were produced")
                taskRepository?.markCompleted(task.id, clock())
                OrchestratorResult.Completed(runId, "Plan created; no tool actions required")
            } else {
                executePlan(plan, task, startIndex = 0)
            }
        } catch (_: CancellationException) {
            update(runId, AgentStatus.CANCELLED, "Execution cancelled")
            taskRepository?.updateStatus(task.id, TaskStatus.CANCELLED.name, "Execution cancelled")
            OrchestratorResult.Cancelled(runId)
        } catch (t: Throwable) {
            update(runId, AgentStatus.FAILED, "Execution failed")
            _state.value = _state.value.copy(error = t.message)
            taskRepository?.updateStatus(task.id, TaskStatus.FAILED.name, t.message ?: "Unknown error")
            OrchestratorResult.Failed(runId, t.message ?: "Unknown error")
        }
    }

    suspend fun resumePendingTasks(thinkingLevel: ThinkingLevel = ThinkingLevel.MAX): List<OrchestratorResult> =
        taskRepository?.resumableTasks()?.map { task -> resumeTask(task, thinkingLevel) }.orEmpty()

    fun requestStop() {
        stopRequested = true
    }

    fun journal(runId: String): List<ActionJournalEntry> = journal.forRun(runId)

    private suspend fun resumeTask(task: TaskRecord, thinkingLevel: ThinkingLevel): OrchestratorResult {
        stopRequested = false
        val runId = UUID.randomUUID().toString()
        update(runId, AgentStatus.UNDERSTANDING, "Resuming persisted task")
        return try {
            val plan = createPlan(task.goal, thinkingLevel, runId)
            val startIndex = task.currentStep.coerceIn(0, plan.actions.size)
            val resumed = task.copy(
                status = TaskStatus.RUNNING,
                totalSteps = plan.actions.size,
                currentStep = startIndex,
                updatedAt = clock(),
                lastError = null,
                checkpointJson = checkpointJson(runId, startIndex)
            )
            taskRepository?.checkpoint(resumed, null)

            if (startIndex >= plan.actions.size) {
                update(runId, AgentStatus.COMPLETED, "Persisted task was already at its final checkpoint")
                taskRepository?.markCompleted(task.id, clock())
                OrchestratorResult.Completed(runId, "Task resumed and completed from checkpoint")
            } else {
                executePlan(plan, resumed, startIndex)
            }
        } catch (_: CancellationException) {
            update(runId, AgentStatus.CANCELLED, "Resume cancelled")
            taskRepository?.updateStatus(task.id, TaskStatus.CANCELLED.name, "Resume cancelled")
            OrchestratorResult.Cancelled(runId)
        } catch (t: Throwable) {
            update(runId, AgentStatus.FAILED, "Resume failed")
            _state.value = _state.value.copy(error = t.message)
            taskRepository?.updateStatus(task.id, TaskStatus.FAILED.name, t.message ?: "Unknown error")
            OrchestratorResult.Failed(runId, t.message ?: "Unknown error")
        }
    }

    private suspend fun createPlan(goal: String, thinkingLevel: ThinkingLevel, runId: String): AgentPlan {
        val relevantMemory = memoryRepository?.search(goal, limit = 8).orEmpty()
        val planningGoal = MemoryRepositoryBridge.withContext(goal, relevantMemory)
        update(runId, AgentStatus.PLANNING, "Creating execution plan")
        return planner.createPlan(planningGoal, thinkingLevel).copy(runId = runId)
    }

    private fun createTask(goal: String, runId: String): TaskRecord {
        val createdAt = clock()
        return TaskRecord(
            id = UUID.randomUUID().toString(),
            title = goal.trim().take(80).ifBlank { "Vynnra task" },
            goal = goal,
            status = TaskStatus.PENDING,
            currentStep = 0,
            totalSteps = 0,
            checkpointJson = checkpointJson(runId, 0),
            lastError = null,
            requiresUserAction = false,
            createdAt = createdAt,
            updatedAt = createdAt,
            completedAt = null
        )
    }

    private suspend fun executePlan(
        plan: AgentPlan,
        initialTask: TaskRecord,
        startIndex: Int
    ): OrchestratorResult {
        var task = initialTask
        for ((index, action) in plan.actions.withIndex()) {
            if (index < startIndex) continue
            currentCoroutineContext().ensureActive()
            if (stopRequested) {
                update(plan.runId, AgentStatus.CANCELLED, "Emergency stop requested")
                taskRepository?.updateStatus(task.id, TaskStatus.CANCELLED.name, "Emergency stop requested")
                return OrchestratorResult.Cancelled(plan.runId)
            }

            val tool = registry.get(action.toolId)
                ?: return failTask(task, plan.runId, "Tool not registered: ${action.toolId}")

            when (val gate = capabilityGate.check(tool.definition.requiredCapabilities)) {
                GateResult.Allowed -> Unit
                is GateResult.Blocked -> {
                    update(plan.runId, AgentStatus.BLOCKED, "Missing capability: ${gate.missing.joinToString()}")
                    journal.record(plan.runId, action, "BLOCKED", "Missing capability")
                    taskRepository?.checkpoint(
                        task.copy(
                            status = TaskStatus.BLOCKED,
                            currentStep = index,
                            updatedAt = clock(),
                            requiresUserAction = true,
                            checkpointJson = checkpointJson(plan.runId, index)
                        ),
                        TaskStepRecord(
                            taskId = task.id,
                            stepIndex = index,
                            title = tool.definition.name,
                            status = TaskStepStatus.BLOCKED,
                            toolId = action.toolId,
                            inputJson = action.input.toString(),
                            outputSummary = null,
                            attempts = 0,
                            startedAt = null,
                            completedAt = null,
                            errorMessage = "Missing capability: ${gate.missing.joinToString()}"
                        )
                    )
                    return OrchestratorResult.Blocked(plan.runId, gate.missing)
                }
            }

            update(plan.runId, AgentStatus.EXECUTING, "Executing ${tool.definition.name}")
            val startedAt = clock()
            var attempt = 0
            var lastResult = ToolResultStatus.FAILED
            while (true) {
                currentCoroutineContext().ensureActive()
                val runningTask = task.copy(
                    status = TaskStatus.RUNNING,
                    currentStep = index,
                    updatedAt = clock(),
                    checkpointJson = checkpointJson(plan.runId, index)
                )
                val runningStep = TaskStepRecord(
                    taskId = task.id,
                    stepIndex = index,
                    title = tool.definition.name,
                    status = TaskStepStatus.RUNNING,
                    toolId = action.toolId,
                    inputJson = action.input.toString(),
                    outputSummary = null,
                    attempts = attempt,
                    startedAt = startedAt,
                    completedAt = null,
                    errorMessage = null
                )
                taskRepository?.checkpoint(runningTask, runningStep)
                journal.record(plan.runId, action, "STARTED")

                val result = tool.execute(action.input)
                lastResult = result.status
                journal.record(plan.runId, action, result.status.name, result.message)

                if (result.status == ToolResultStatus.CANCELLED || stopRequested) {
                    update(plan.runId, AgentStatus.CANCELLED, "Execution cancelled")
                    taskRepository?.checkpoint(
                        runningTask.copy(status = TaskStatus.CANCELLED, updatedAt = clock(), lastError = "Execution cancelled"),
                        runningStep.copy(
                            status = TaskStepStatus.CANCELLED,
                            completedAt = clock(),
                            attempts = attempt + 1,
                            errorMessage = "Execution cancelled"
                        )
                    )
                    return OrchestratorResult.Cancelled(plan.runId)
                }

                if (action.requiresVerification || tool.definition.supportsVerification) {
                    update(plan.runId, AgentStatus.VERIFYING, "Verifying ${tool.definition.name}")
                    val verification = verifier.verify(action, result)
                    if (verification.verified) {
                        val verifiedAt = clock()
                        task = runningTask.copy(currentStep = index + 1, updatedAt = verifiedAt, lastError = null)
                        taskRepository?.checkpoint(
                            task,
                            runningStep.copy(
                                status = TaskStepStatus.VERIFIED,
                                completedAt = verifiedAt,
                                attempts = attempt + 1,
                                outputSummary = boundedSummary(result.message ?: verification.message)
                            )
                        )
                        break
                    }
                } else if (result.status == ToolResultStatus.SUCCESS) {
                    val verifiedAt = clock()
                    task = runningTask.copy(currentStep = index + 1, updatedAt = verifiedAt, lastError = null)
                    taskRepository?.checkpoint(
                        task,
                        runningStep.copy(
                            status = TaskStepStatus.VERIFIED,
                            completedAt = verifiedAt,
                            attempts = attempt + 1,
                            outputSummary = boundedSummary(result.message)
                        )
                    )
                    break
                }

                update(plan.runId, AgentStatus.RECOVERING, "Recovering from $lastResult")
                val decision = recovery.decide(action, result.status, attempt)
                if (!decision.retry) {
                    val failedAt = clock()
                    task = runningTask.copy(status = TaskStatus.FAILED, updatedAt = failedAt, lastError = decision.message)
                    taskRepository?.checkpoint(
                        task,
                        runningStep.copy(
                            status = TaskStepStatus.FAILED,
                            completedAt = failedAt,
                            attempts = attempt + 1,
                            errorMessage = decision.message,
                            outputSummary = boundedSummary(result.message)
                        )
                    )
                    return OrchestratorResult.Failed(plan.runId, decision.message)
                }
                attempt++
            }
        }

        update(plan.runId, AgentStatus.COMPLETED, "Task completed and verified")
        taskRepository?.markCompleted(task.id, clock())
        return OrchestratorResult.Completed(plan.runId, "Task completed")
    }

    private suspend fun failTask(task: TaskRecord, runId: String, message: String): OrchestratorResult {
        update(runId, AgentStatus.FAILED, message)
        taskRepository?.updateStatus(task.id, TaskStatus.FAILED.name, message)
        return OrchestratorResult.Failed(runId, message)
    }

    private fun update(runId: String, status: AgentStatus, activity: String) {
        _state.value = OrchestratorState(
            runId = runId,
            status = status,
            currentActivity = activity,
            error = null
        )
    }

    private fun checkpointJson(runId: String, actionIndex: Int): String =
        "{\"runId\":\"$runId\",\"actionIndex\":$actionIndex}"

    private fun boundedSummary(message: String?): String? = message?.take(2_000)
}

data class OrchestratorState(
    val runId: String? = null,
    val status: AgentStatus = AgentStatus.IDLE,
    val currentActivity: String? = null,
    val error: String? = null
)

sealed interface OrchestratorResult {
    data class Completed(val runId: String, val message: String) : OrchestratorResult
    data class Failed(val runId: String, val message: String) : OrchestratorResult
    data class Blocked(val runId: String, val missing: Set<lol.vynnra.agent.core.tool.Capability>) : OrchestratorResult
    data class Cancelled(val runId: String) : OrchestratorResult
}
