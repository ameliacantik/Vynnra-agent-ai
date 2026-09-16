package lol.vynnra.agent.core.orchestrator

import lol.vynnra.agent.core.tool.ToolResultStatus

interface RecoveryEngine {
    suspend fun decide(action: AgentAction, resultStatus: ToolResultStatus, attempt: Int): RecoveryDecision
}

class BoundedRecoveryEngine(
    private val maxRetries: Int = 2
) : RecoveryEngine {
    override suspend fun decide(
        action: AgentAction,
        resultStatus: ToolResultStatus,
        attempt: Int
    ): RecoveryDecision {
        if (resultStatus == ToolResultStatus.FAILED && attempt < maxRetries) {
            return RecoveryDecision(
                retry = true,
                message = "Retrying ${action.toolId} (attempt ${attempt + 1}/$maxRetries)"
            )
        }
        return RecoveryDecision(
            retry = false,
            message = "No automatic recovery available"
        )
    }
}
