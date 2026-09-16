package lol.vynnra.agent.core.orchestrator

import lol.vynnra.agent.core.tool.ToolResult
import lol.vynnra.agent.core.tool.ToolResultStatus

interface VerificationEngine {
    suspend fun verify(action: AgentAction, result: ToolResult): VerificationResult
}

class DefaultVerificationEngine : VerificationEngine {
    override suspend fun verify(action: AgentAction, result: ToolResult): VerificationResult =
        when (result.status) {
            ToolResultStatus.SUCCESS -> VerificationResult(true, result.message ?: "Action completed")
            ToolResultStatus.PARTIAL -> VerificationResult(false, result.message ?: "Action completed partially")
            ToolResultStatus.FAILED -> VerificationResult(false, result.message ?: "Action failed")
            ToolResultStatus.CANCELLED -> VerificationResult(false, "Action cancelled")
            ToolResultStatus.BLOCKED -> VerificationResult(false, result.message ?: "Action blocked")
        }
}
