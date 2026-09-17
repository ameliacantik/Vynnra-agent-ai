package lol.vynnra.agent.platform

import kotlinx.coroutines.delay
import lol.vynnra.agent.core.orchestrator.AgentAction
import lol.vynnra.agent.core.orchestrator.VerificationEngine
import lol.vynnra.agent.core.orchestrator.VerificationResult
import lol.vynnra.agent.core.tool.ToolResult
import lol.vynnra.agent.core.tool.ToolResultStatus

/**
 * Phase 5 verification layer. It prefers an explicit post-condition such as visible text,
 * while falling back to the tool result when no deterministic UI assertion was requested.
 */
class AndroidVerificationEngine(
    private val controller: AndroidController,
    private val settleDelayMs: Long = 250L
) : VerificationEngine {

    override suspend fun verify(action: AgentAction, result: ToolResult): VerificationResult {
        if (result.status != ToolResultStatus.SUCCESS) {
            return VerificationResult(false, result.message ?: "Tool execution did not succeed")
        }

        val expectedText = (action.input["expectedText"] as? String)
            ?: (result.data["expectedText"] as? String)

        if (expectedText.isNullOrBlank()) {
            return VerificationResult(true, result.message ?: "Action reported success")
        }

        delay(settleDelayMs.coerceIn(50L, 2_000L))
        val matches = controller.findText(expectedText)
        return if (matches.isNotEmpty()) {
            VerificationResult(true, "Verified visible text: $expectedText")
        } else {
            VerificationResult(false, "Post-condition not observed: $expectedText")
        }
    }
}
