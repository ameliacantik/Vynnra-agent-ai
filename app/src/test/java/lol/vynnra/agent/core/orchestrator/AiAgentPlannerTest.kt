package lol.vynnra.agent.core.orchestrator

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import lol.vynnra.agent.core.agent.ThinkingLevel
import lol.vynnra.agent.core.provider.AiMessage
import lol.vynnra.agent.core.provider.AiProvider
import lol.vynnra.agent.core.provider.AiRequest
import lol.vynnra.agent.core.provider.AiResponse
import lol.vynnra.agent.core.provider.AiStreamEvent
import lol.vynnra.agent.core.tool.RiskLevel
import lol.vynnra.agent.core.tool.ToolDefinition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiAgentPlannerTest {
    @Test
    fun parses_model_plan_and_restricts_to_registered_tools() = kotlinx.coroutines.runBlocking {
        val planner = AiAgentPlanner(
            provider = FakePlannerProvider("""{"actions":[{"id":"a1","tool":"android.home","input":{},"verify":true},{"id":"a2","tool":"not.registered","input":{}}]}"""),
            modelProvider = { "test-model" },
            toolDefinitionsProvider = { listOf(
                ToolDefinition("android.home", "Home", "Go home", riskLevel = RiskLevel.LOW),
                ToolDefinition("ai.answer", "Answer", "Answer user", riskLevel = RiskLevel.LOW)
            ) }
        )

        val plan = planner.createPlan("Go home", ThinkingLevel.HIGH)

        assertEquals(1, plan.actions.size)
        assertEquals("android.home", plan.actions.single().toolId)
        assertTrue(plan.actions.single().requiresVerification)
        assertEquals(ThinkingLevel.HIGH, plan.thinkingLevel)
    }

    @Test
    fun falls_back_to_ai_answer_when_model_output_is_invalid() = kotlinx.coroutines.runBlocking {
        val planner = AiAgentPlanner(
            provider = FakePlannerProvider("not-json"),
            modelProvider = { "test-model" },
            toolDefinitionsProvider = { listOf(
                ToolDefinition("ai.answer", "Answer", "Answer user", riskLevel = RiskLevel.LOW)
            ) }
        )

        val plan = planner.createPlan("Explain Vynnra", ThinkingLevel.MAX)

        assertEquals(1, plan.actions.size)
        assertEquals("ai.answer", plan.actions.single().toolId)
        assertEquals("Explain Vynnra", plan.actions.single().input["prompt"])
    }
}

private class FakePlannerProvider(private val response: String) : AiProvider {
    override val id = "fake"
    override val displayName = "Fake"

    override suspend fun complete(request: AiRequest): AiResponse =
        AiResponse(content = response, model = request.model, finishReason = "stop")

    override fun stream(request: AiRequest): Flow<AiStreamEvent> =
        flowOf(AiStreamEvent.Completed(AiResponse(response, request.model, "stop")))
}