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
import lol.vynnra.agent.core.security.CapabilityGate
import lol.vynnra.agent.core.security.GateResult
import lol.vynnra.agent.core.tool.ToolResultStatus

class AgentOrchestrator(
    private val planner: AgentPlanner,
    private val registry: ToolRegistry,
    private val capabilityGate: CapabilityGate,
    private val verifier: VerificationEngine = DefaultVerificationEngine(),
    private val recovery: RecoveryEngine = BoundedRecoveryEngine(),
    private val journal: ActionJournal = ActionJournal()
) {
    private val _state = MutableStateFlow(OrchestratorState())
    val state: StateFlow<OrchestratorState> = _state.asStateFlow()

    @Volatile
    private var stopRequested = false

    suspend fun run(goal: String, thinkingLevel: ThinkingLevel = ThinkingLevel.MAX): OrchestratorResult {
        stopRequested = false
        val runId = UUID.randomUUID().toString()
        update(runId, AgentStatus.UNDERSTANDING, "Understanding request")

        return try {
            update(runId, AgentStatus.PLANNING, "Creating execution plan")
            val plan = planner.createPlan(goal, thinkingLevel)

            if (plan.actions.isEmpty()) {
                update(runId, AgentStatus.COMPLETED, "No executable actions were produced")
                OrchestratorResult.Completed(runId, "Plan created; no tool actions required")
            } else {
                executePlan(plan.copy(runId = runId))
            }
        } catch (_: CancellationException) {
            update(runId, AgentStatus.CANCELLED, "Execution cancelled")
            OrchestratorResult.Cancelled(runId)
        } catch (t: Throwable) {
            update(runId, AgentStatus.FAILED, "Execution failed")
            _state.value = _state.value.copy(error = t.message)
            OrchestratorResult.Failed(runId, t.message ?: "Unknown error")
        }
    }

    fun requestStop() {
        stopRequested = true
    }

    fun journal(runId: String): List<ActionJournalEntry> = journal.forRun(runId)

    private suspend fun executePlan(plan: AgentPlan): OrchestratorResult {
        for (action in plan.actions) {
            currentCoroutineContext().ensureActive()
            if (stopRequested) {
                update(plan.runId, AgentStatus.CANCELLED, "Emergency stop requested")
                return OrchestratorResult.Cancelled(plan.runId)
            }

            val tool = registry.get(action.toolId)
                ?: return failBlocked(plan.runId, "Tool not registered: ${action.toolId}")

            when (val gate = capabilityGate.check(tool.definition.requiredCapabilities)) {
                GateResult.Allowed -> Unit
                is GateResult.Blocked -> {
                    update(plan.runId, AgentStatus.BLOCKED, "Missing capability: ${gate.missing.joinToString()}")
                    journal.record(plan.runId, action, "BLOCKED", "Missing capability")
                    return OrchestratorResult.Blocked(plan.runId, gate.missing)
                }
            }

            update(plan.runId, AgentStatus.EXECUTING, "Executing ${tool.definition.name}")
            var attempt = 0
            while (true) {
                currentCoroutineContext().ensureActive()
                journal.record(plan.runId, action, "STARTED")
                val result = tool.execute(action.input)
                journal.record(plan.runId, action, result.status.name, result.message)

                if (result.status == ToolResultStatus.CANCELLED || stopRequested) {
                    update(plan.runId, AgentStatus.CANCELLED, "Execution cancelled")
                    return OrchestratorResult.Cancelled(plan.runId)
                }

                if (action.requiresVerification || tool.definition.supportsVerification) {
                    update(plan.runId, AgentStatus.VERIFYING, "Verifying ${tool.definition.name}")
                    val verification = verifier.verify(action, result)
                    if (verification.verified) {
                        break
                    }
                } else if (result.status == ToolResultStatus.SUCCESS) {
                    break
                }

                update(plan.runId, AgentStatus.RECOVERING, "Recovering from ${result.status.name}")
                val decision = recovery.decide(action, result.status, attempt)
                if (!decision.retry) {
                    return OrchestratorResult.Failed(plan.runId, decision.message)
                }
                attempt++
            }
        }

        update(plan.runId, AgentStatus.COMPLETED, "Task completed and verified")
        return OrchestratorResult.Completed(plan.runId, "Task completed")
    }

    private fun failBlocked(runId: String, message: String): OrchestratorResult {
        update(runId, AgentStatus.BLOCKED, message)
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
