package lol.vynnra.agent.core.orchestrator

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import lol.vynnra.agent.core.agent.ThinkingLevel
import lol.vynnra.agent.core.provider.AiMessage
import lol.vynnra.agent.core.provider.AiProvider
import lol.vynnra.agent.core.provider.AiRequest
import lol.vynnra.agent.core.provider.AiResponse
import lol.vynnra.agent.core.provider.AiStreamEvent
import lol.vynnra.agent.core.tool.ToolDefinition
import lol.vynnra.agent.core.tool.ToolResult
import lol.vynnra.agent.core.tool.RiskLevel
import lol.vynnra.agent.core.security.CapabilityGate
import lol.vynnra.agent.platform.AiAnswerTool
import org.junit.Assert.assertEquals
import org.junit.Test

class AgentOrchestratorLiveIntegrationTest {
    @Test
    fun orchestrator_executes_ai_answer_and_returns_real_response() = kotlinx.coroutines.runBlocking {
        val registry = ToolRegistry()
        val provider = FakeAnswerProvider("The real agent path is active.")
        registry.register(
            ToolRegistry.adapter(AiAnswerTool(provider) { "test-model" }) { input ->
                mapOf(
                    "prompt" to (input["prompt"] as? String ?: error("prompt missing")),
                    "context" to (input["context"] as? String ?: "")
                ).let { lol.vynnra.agent.platform.AiAnswerInput(it["prompt"] as String, it["context"] as String) }
            }
        )
        val planner = object : AgentPlanner {
            override suspend fun createPlan(goal: String, thinkingLevel: ThinkingLevel): AgentPlan = AgentPlan(
                runId = "planner-run",
                goal = goal,
                thinkingLevel = thinkingLevel,
                actions = listOf(AgentAction("a1", "ai.answer", mapOf("prompt" to goal), true))
            )
        }
        val orchestrator = AgentOrchestrator(
            planner = planner,
            registry = registry,
            capabilityGate = CapabilityGate { emptySet() }
        )

        val result = orchestrator.run("Test the agent", ThinkingLevel.HIGH)

        assertEquals("The real agent path is active.", (result as OrchestratorResult.Completed).message)
        assertEquals("The real agent path is active.", orchestrator.state.value.result)
    }
}

private class FakeAnswerProvider(private val text: String) : AiProvider {
    override val id = "fake"
    override val displayName = "Fake"
    override suspend fun complete(request: AiRequest): AiResponse =
        AiResponse(text, request.model, "stop")
    override fun stream(request: AiRequest): Flow<AiStreamEvent> =
        flowOf(AiStreamEvent.Completed(AiResponse(text, request.model, "stop")))
}