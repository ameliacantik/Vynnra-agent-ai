package lol.vynnra.agent.core.orchestrator

import lol.vynnra.agent.core.agent.ThinkingLevel
import java.util.UUID

interface AgentPlanner {
    suspend fun createPlan(goal: String, thinkingLevel: ThinkingLevel): AgentPlan
}

/**
 * Safe foundation planner. It does not invent executable tools; actual planning
 * will be provided by an AI-backed planner in a later provider integration.
 */
class FoundationPlanner : AgentPlanner {
    override suspend fun createPlan(goal: String, thinkingLevel: ThinkingLevel): AgentPlan =
        AgentPlan(
            runId = UUID.randomUUID().toString(),
            goal = goal,
            actions = emptyList(),
            thinkingLevel = thinkingLevel
        )
}
