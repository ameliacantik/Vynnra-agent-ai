package lol.vynnra.agent.core.orchestrator

import lol.vynnra.agent.core.agent.ThinkingLevel

/** A deliberately high-level action description. Private chain-of-thought is never stored here. */
data class AgentAction(
    val id: String,
    val toolId: String,
    val input: Map<String, Any?> = emptyMap(),
    val requiresVerification: Boolean = false
)

data class AgentPlan(
    val runId: String,
    val goal: String,
    val actions: List<AgentAction>,
    val thinkingLevel: ThinkingLevel
)

data class VerificationResult(
    val verified: Boolean,
    val message: String
)

data class RecoveryDecision(
    val retry: Boolean,
    val nextAction: AgentAction? = null,
    val message: String
)
