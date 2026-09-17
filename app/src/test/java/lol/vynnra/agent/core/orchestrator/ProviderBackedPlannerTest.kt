package lol.vynnra.agent.core.orchestrator

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import lol.vynnra.agent.core.agent.ThinkingLevel
import lol.vynnra.agent.core.provider.AiMessage
import lol.vynnra.agent.core.provider.AiProvider
import lol.vynnra.agent.core.provider.AiRequest
import lol.vynnra.agent.core.provider.AiResponse
import lol.vynnra.agent.core.provider.AiStreamEvent
import lol.vynnra.agent.core.tool.Capability
import lol.vynnra.agent.core.tool.ToolDefinition
import lol.vynnra.agent.core.tool.RiskLevel

class ProviderBackedPlannerTest {
    @Test
    fun parses_json_plan_and_filters_unavailable_capabilities() = runBlocking {
        val provider = FakePlannerProvider(
            """
            {"actions":[
              {"id":"a1","toolId":"file.list","input":{"path":"/storage/emulated/0"},"requiresVerification":false},
              {"id":"a2","toolId":"android.tap","input":{"x":20,"y":30},"requiresVerification":true},
              {"id":"bad","toolId":"missing.tool","input":{}}
            ]}
            """.trimIndent()
        )
        val planner = ProviderBackedPlanner(
            provider = provider,
            modelProvider = { "test-model" },
            toolDefinitionsProvider = {
                listOf(
                    ToolDefinition(
                        id = "file.list",
                        name = "List",
                        description = "List files",
                        requiredCapabilities = setOf(Capability.FILE_READ),
                        riskLevel = RiskLevel.LOW
                    ),
                    ToolDefinition(
                        id = "android.tap",
                        name = "Tap",
                        description = "Tap",
                        requiredCapabilities = setOf(Capability.ACCESSIBILITY_CONTROL),
                        riskLevel = RiskLevel.MEDIUM
                    )
                )
            },
            availableCapabilitiesProvider = { setOf(Capability.FILE_READ) }
        )

        val plan = planner.createPlan("list files", ThinkingLevel.MAX)

        assertEquals(1, plan.actions.size)
        assertEquals("file.list", plan.actions.single().toolId)
        assertEquals("/storage/emulated/0", plan.actions.single().input["path"])
        assertTrue(provider.lastRequest!!.messages.first().content.contains("file.list"))
        assertTrue(!provider.lastRequest!!.messages.first().content.contains("android.tap"))
    }
}

private class FakePlannerProvider(
    private val responseText: String
) : AiProvider {
    var lastRequest: AiRequest? = null

    override val id: String = "test"
    override val displayName: String = "Test"

    override suspend fun complete(request: AiRequest): AiResponse {
        lastRequest = request
        return AiResponse(responseText, request.model)
    }

    override fun stream(request: AiRequest): Flow<AiStreamEvent> = emptyFlow()
}
