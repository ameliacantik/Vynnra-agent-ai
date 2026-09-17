package lol.vynnra.agent.core.orchestrator

import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject
import lol.vynnra.agent.core.agent.ThinkingLevel
import lol.vynnra.agent.core.provider.AiMessage
import lol.vynnra.agent.core.provider.AiProvider
import lol.vynnra.agent.core.provider.AiRequest
import lol.vynnra.agent.core.tool.ToolDefinition

interface AgentPlanner {
    suspend fun createPlan(goal: String, thinkingLevel: ThinkingLevel): AgentPlan
}

/**
 * Provider-backed planner. The model emits only executable tool plans;
 * hidden chain-of-thought is never requested or stored.
 */
class ProviderBackedPlanner(
    private val provider: AiProvider,
    private val modelProvider: () -> String,
    private val toolDefinitionsProvider: () -> List<ToolDefinition>,
    private val availableCapabilitiesProvider: () -> Set<lol.vynnra.agent.core.tool.Capability> = { emptySet() }
) : AgentPlanner {

    override suspend fun createPlan(goal: String, thinkingLevel: ThinkingLevel): AgentPlan {
        val model = modelProvider().trim()
        require(model.isNotEmpty()) { "AI model is not configured." }

        val available = availableCapabilitiesProvider()
        val tools = toolDefinitionsProvider()
            .filter { it.requiredCapabilities.all(available::contains) }

        val response = provider.complete(
            AiRequest(
                model = model,
                messages = listOf(
                    AiMessage(
                        AiMessage.Role.SYSTEM,
                        buildPlannerPrompt(tools, thinkingLevel)
                    ),
                    AiMessage(AiMessage.Role.USER, goal.trim())
                ),
                temperature = 0.1,
                maxTokens = 4_000
            )
        )
        return parsePlan(response.content, goal, thinkingLevel, tools)
    }

    private fun buildPlannerPrompt(
        tools: List<ToolDefinition>,
        thinkingLevel: ThinkingLevel
    ): String {
        val maxActions = maxActions(thinkingLevel)
        val catalog = tools.joinToString("\n") { definition ->
            "- ${definition.id}: ${definition.description}; capabilities=${definition.requiredCapabilities.joinToString(",")}; risk=${definition.riskLevel}; confirmation=${definition.requiresConfirmation}; verification=${definition.supportsVerification}"
        }
        return """
You are Vynnra's execution planner.
Turn the user's goal into a short executable tool plan.

Rules:
- Return JSON only. No markdown fences and no prose outside JSON.
- JSON schema:
  {"actions":[{"id":"a1","toolId":"tool.id","input":{},"requiresVerification":true}]}
- Use only tool IDs from the catalog below.
- Maximum $maxActions actions.
- Prefer observation/read actions before interaction.
- Set requiresVerification=true for state-changing or browser actions when supported.
- Never invent tool IDs or unsupported input fields.
- Do not select tools with confirmation=true. A future approval layer will handle those actions.
- If the request only needs a normal answer, return {"actions":[]}.

Thinking level: ${thinkingLevel.name}

Tool catalog:
$catalog
        """.trimIndent()
    }

    private fun parsePlan(
        raw: String,
        goal: String,
        thinkingLevel: ThinkingLevel,
        tools: List<ToolDefinition>
    ): AgentPlan {
        val jsonText = extractJsonObject(raw)
        val root = JSONObject(jsonText)
        val actionsJson = root.optJSONArray("actions") ?: JSONArray()
        val definitions = tools.associateBy { it.id }
        val maxActions = maxActions(thinkingLevel)

        val actions = buildList {
            for (index in 0 until minOf(actionsJson.length(), maxActions)) {
                val item = actionsJson.optJSONObject(index) ?: continue
                val toolId = item.optString("toolId").trim()
                val definition = definitions[toolId] ?: continue
                if (definition.requiresConfirmation) continue

                val input = item.optJSONObject("input")?.toMap().orEmpty()
                val requestedVerification = item.optBoolean(
                    "requiresVerification",
                    definition.supportsVerification
                )
                add(
                    AgentAction(
                        id = item.optString("id").trim()
                            .ifBlank { "a${index + 1}-${UUID.randomUUID()}" },
                        toolId = toolId,
                        input = input,
                        requiresVerification = requestedVerification
                    )
                )
            }
        }

        return AgentPlan(
            runId = UUID.randomUUID().toString(),
            goal = goal.trim(),
            actions = actions,
            thinkingLevel = thinkingLevel
        )
    }

    private fun maxActions(thinkingLevel: ThinkingLevel): Int = when (thinkingLevel) {
        ThinkingLevel.LOW -> 3
        ThinkingLevel.MEDIUM -> 6
        ThinkingLevel.HIGH -> 10
        ThinkingLevel.MAX -> 15
    }

    private fun extractJsonObject(raw: String): String {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        require(start >= 0 && end > start) { "Planner returned invalid JSON." }
        return raw.substring(start, end + 1)
    }
}

private fun JSONObject.toMap(): Map<String, Any?> {
    val result = linkedMapOf<String, Any?>()
    val keys = keys()
    while (keys.hasNext()) {
        val key = keys.next()
        result[key] = opt(key).toKotlinValue()
    }
    return result
}

private fun Any?.toKotlinValue(): Any? = when (this) {
    is JSONObject -> toMap()
    is JSONArray -> buildList {
        for (index in 0 until length()) {
            add(opt(index).toKotlinValue())
        }
    }
    JSONObject.NULL -> null
    else -> this
}

/** Safe fallback used only by tests and bootstrapping. */
class FoundationPlanner : AgentPlanner {
    override suspend fun createPlan(goal: String, thinkingLevel: ThinkingLevel): AgentPlan =
        AgentPlan(
            runId = UUID.randomUUID().toString(),
            goal = goal,
            actions = emptyList(),
            thinkingLevel = thinkingLevel
        )
}
