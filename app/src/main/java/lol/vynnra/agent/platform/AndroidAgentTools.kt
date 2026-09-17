package lol.vynnra.agent.platform

import android.content.Context
import lol.vynnra.agent.core.orchestrator.AgentPlanner
import lol.vynnra.agent.core.orchestrator.AgentOrchestrator
import lol.vynnra.agent.core.orchestrator.BoundedRecoveryEngine
import lol.vynnra.agent.core.orchestrator.FoundationPlanner
import lol.vynnra.agent.core.orchestrator.ToolRegistry
import lol.vynnra.agent.core.security.CapabilityGate
import lol.vynnra.agent.core.tool.Capability

/**
 * Phase 5 wiring for the Android tool layer. Callers provide the currently granted
 * capabilities so the orchestrator never assumes that Accessibility is enabled.
 */
class AndroidAgentTools(
    context: Context,
    grantedCapabilities: () -> Set<Capability>,
    planner: AgentPlanner = FoundationPlanner()
) {
    val controller = AndroidController(context.applicationContext)
    val registry = ToolRegistry()
    val capabilityGate = CapabilityGate(grantedCapabilities)
    val verifier = AndroidVerificationEngine(controller)
    val orchestrator = AgentOrchestrator(
        planner = planner,
        registry = registry,
        capabilityGate = capabilityGate,
        verifier = verifier,
        recovery = BoundedRecoveryEngine()
    )

    init {
        AndroidToolRegistrar.registerAll(registry, controller)
    }
}
