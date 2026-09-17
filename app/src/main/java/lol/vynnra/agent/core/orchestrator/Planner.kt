package lol.vynnra.agent.core.orchestrator

import java.util.UUID
import lol.vynnra.agent.core.agent.ThinkingLevel
import lol.vynnra.agent.core.provider.AiMessage
import lol.vynnra.agent.core.provider.AiProvider
import lol.vynnra.agent.core.provider.AiRequest
import lol.vynnra.agent.core.provider.VynnraSystemPrompt
import lol.vynnra.agent.core.tool.ToolDefinition
import org.json.JSONArray
import org.json.JSONObject

interface AgentPlanner {
    suspend fun createPlan(goal: String, thinkingLevel: ThinkingLevel): AgentPlan
}

/**
 * Provider-backed planner. The model returns only executable action JSON; hidden reasoning is
 * never requested or persisted.
 */
class AiAgentPlanner(
    private val provider: AiProvider,
    private val modelProvider: () -> String,
    private val toolDefinitionsProvider: () -> List<ToolDefinition>,
    private val maxActions: Int = 12
) : AgentPlanner {

    override suspend fun createPlan(goal: String, thinkingLevel: ThinkingLevel): AgentPlan {
        val model = modelProvider().trim()
        require(model.isNotEmpty()) { "AI model is not configured." }

        val response = provider.complete(
            AiRequest(
                model = model,
                temperature = 0.0,
                messages = listOf(
                    AiMessage(
                        AiMessage.Role.SYSTEM,
                        """
                        ${VynnraSystemPrompt.value}

                        You are the Vynnra execution planner.
                        Produce a short executable plan as JSON only. Do not output markdown,
                        chain-of-thought, hidden reasoning, explanations, or prose outside JSON.

                        Planning rules:
                        - Use only tools from the supplied catalog.
                        - Prefer the smallest safe number of actions.
                        - Observe before interacting with the UI when useful.
                        - Mark actions that need deterministic post-condition checking with "verify": true.
                        - Never invent package names, URLs, paths, coordinates, or other facts that
                          are not justified by the user's request or current tool context.
                        - Do not set confirmation flags for destructive actions unless the user
                          explicitly confirmed that action in this request.
                        - For ordinary questions or requests that need a natural-language response,
                          finish with "ai.answer".
                        - Use thinking level: ${thinkingLevel.name}.

                        JSON schema:
                        {"actions":[
                          {"id":"a1","tool":"tool.id","input":{},"verify":true}
                        ]}
                        """.trimIndent() + "

Tool catalog:
" + toolCatalog()
                    ),
                    AiMessage(AiMessage.Role.USER, goal.trim())
                )
            )
        )

        return parsePlan(response.content, goal, thinkingLevel)
    }

    private fun parsePlan(raw: String, goal: String, thinkingLevel: ThinkingLevel): AgentPlan {
        val jsonText = extractJson(raw)
        val root = runCatching { JSONObject(jsonText) }.getOrElse {
            return fallbackPlan(goal, thinkingLevel)
        }
        val jsonActions = root.optJSONArray("actions") ?: return fallbackPlan(goal, thinkingLevel)
        val known = toolDefinitionsProvider().map { it.id }.toSet()
        val actions = buildList {
            for (index in 0 until minOf(jsonActions.length(), maxActions.coerceAtLeast(1))) {
                val item = jsonActions.optJSONObject(index) ?: continue
                val toolId = item.optString("tool").trim()
                if (toolId.isEmpty() || toolId !in known) continue
                val input = item.optJSONObject("input")?.toMap().orEmpty()
                val id = item.optString("id").trim().ifEmpty { "a$index" }
                val verify = item.optBoolean("verify", false)
                add(AgentAction(id = id, toolId = toolId, input = input, requiresVerification = verify))
            }
        }
        if (actions.isEmpty()) return fallbackPlan(goal, thinkingLevel)
        return AgentPlan(
            runId = UUID.randomUUID().toString(),
            goal = goal,
            actions = actions,
            thinkingLevel = thinkingLevel
        )
    }

    private fun fallbackPlan(goal: String, thinkingLevel: ThinkingLevel): AgentPlan =
        AgentPlan(
            runId = UUID.randomUUID().toString(),
            goal = goal,
            actions = listOf(
                AgentAction(
                    id = "answer-1",
                    toolId = "ai.answer",
                    input = mapOf("prompt" to goal),
                    requiresVerification = true
                )
            ),
            thinkingLevel = thinkingLevel
        )

    private fun toolCatalog(): String = buildString {
        toolDefinitionsProvider().forEach { tool ->
            append(
                JSONObject()
                    .put("id", tool.id)
                    .put("name", tool.name)
                    .put("description", tool.description)
                    .put("requiredCapabilities", tool.requiredCapabilities.map { it.name })
                    .put("riskLevel", tool.riskLevel.name)
                    .put("requiresConfirmation", tool.requiresConfirmation)
                    .put("supportsVerification", tool.supportsVerification)
                    .toString()
            )
            append('\n')
        }
    }

    private fun extractJson(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) return trimmed
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        if (start >= 0 && end > start) return trimmed.substring(start, end + 1)
        return trimmed
    }
}

class FoundationPlanner : AgentPlanner {
    override suspend fun createPlan(goal: String, thinkingLevel: ThinkingLevel): AgentPlan =
        AgentPlan(
            runId = UUID.randomUUID().toString(),
            goal = goal,
            actions = emptyList(),
            thinkingLevel = thinkingLevel
        )
}

private fun JSONObject.toMap(): Map<String, Any?> =
    keys().asSequence().associateWith { key ->
        when (val value = opt(key)) {
            JSONObject.NULL -> null
            is JSONObject -> value.toMap()
            is JSONArray -> value.toList()
            else -> value
        }
    }

private fun JSONArray.toList(): List<Any?> =
    (0 until length()).map { index ->
        when (val value = opt(index)) {
            JSONObject.NULL -> null
            is JSONObject -> value.toMap()
            is JSONArray -> value.toList()
            else -> value
        }
    }
