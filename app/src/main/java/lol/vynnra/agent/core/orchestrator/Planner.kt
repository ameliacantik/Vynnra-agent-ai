package lol.vynnra.agent.core.orchestrator

import java.util.UUID
import lol.vynnra.agent.core.agent.ThinkingLevel
import lol.vynnra.agent.core.provider.AiMessage
import lol.vynnra.agent.core.provider.AiProvider
import lol.vynnra.agent.core.provider.AiRequest
import lol.vynnra.agent.core.provider.VynnraSystemPrompt
import lol.vynnra.agent.core.tool.ToolDefinition

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
                        \${VynnraSystemPrompt.value}

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
                        - Use thinking level: \${thinkingLevel.name}.

                        JSON schema:
                        {"actions":[{"id":"a1","tool":"tool.id","input":{},"verify":true}]}
                        """.trimIndent() + "\n\nTool catalog:\n" + toolCatalog()
                    ),
                    AiMessage(AiMessage.Role.USER, goal.trim())
                )
            )
        )

        return parsePlan(response.content, goal, thinkingLevel)
    }

    private fun parsePlan(raw: String, goal: String, thinkingLevel: ThinkingLevel): AgentPlan {
        val jsonText = extractJson(raw)
        val root = runCatching { JsonValueParser(jsonText).parseObject() }.getOrElse {
            return fallbackPlan(goal, thinkingLevel)
        }
        val rawActions = root["actions"] as? List<*> ?: return fallbackPlan(goal, thinkingLevel)
        val known = toolDefinitionsProvider().map { it.id }.toSet()
        val actions = buildList {
            for ((index, rawAction) in rawActions.withIndex()) {
                if (size >= maxActions.coerceAtLeast(1)) break
                val item = rawAction as? Map<*, *> ?: continue
                val toolId = (item["tool"] as? String)?.trim().orEmpty()
                if (toolId.isEmpty() || toolId !in known) continue
                val input = (item["input"] as? Map<*, *>)
                    ?.entries
                    ?.associate { (key, value) -> key.toString() to value }
                    .orEmpty()
                val id = (item["id"] as? String)?.trim().takeUnless { it.isNullOrEmpty() } ?: "a$index"
                val verify = item["verify"] as? Boolean ?: false
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
            append("id=").append(tool.id)
                .append("; name=").append(tool.name)
                .append("; description=").append(tool.description)
                .append("; capabilities=").append(tool.requiredCapabilities.joinToString(","))
                .append("; risk=").append(tool.riskLevel.name)
                .append("; confirmation=").append(tool.requiresConfirmation)
                .append("; verification=").append(tool.supportsVerification)
                .append("\n")
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

private class JsonValueParser(private val source: String) {
    private var index = 0

    fun parseObject(): Map<String, Any?> {
        skipWhitespace()
        expect('{')
        val result = linkedMapOf<String, Any?>()
        skipWhitespace()
        if (peek('}')) {
            index++
            return result
        }
        while (true) {
            skipWhitespace()
            val key = parseString()
            skipWhitespace()
            expect(':')
            val value = parseValue()
            result[key] = value
            skipWhitespace()
            when (peek()) {
                ',' -> index++
                '}' -> { index++; return result }
                else -> error("Expected comma or object end at index $index")
            }
        }
    }

    private fun parseValue(): Any? {
        skipWhitespace()
        return when (peek()) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> parseString()
            't' -> parseLiteral("true", true)
            'f' -> parseLiteral("false", false)
            'n' -> parseLiteral("null", null)
            else -> parseNumber()
        }
    }

    private fun parseArray(): List<Any?> {
        expect('[')
        val result = mutableListOf<Any?>()
        skipWhitespace()
        if (peek(']')) {
            index++
            return result
        }
        while (true) {
            result += parseValue()
            skipWhitespace()
            when (peek()) {
                ',' -> index++
                ']' -> { index++; return result }
                else -> error("Expected comma or array end at index $index")
            }
        }
    }

    private fun parseString(): String {
        expect('"')
        val out = StringBuilder()
        while (index < source.length) {
            when (val ch = source[index++]) {
                '"' -> return out.toString()
                '\\' -> {
                    if (index >= source.length) error("Unterminated escape")
                    when (val escaped = source[index++]) {
                        '"' -> out.append('"')
                        '\\' -> out.append('\\')
                        '/' -> out.append('/')
                        'b' -> out.append('\b')
                        'f' -> out.append('\u000C')
                        'n' -> out.append('\n')
                        'r' -> out.append('\r')
                        't' -> out.append('\t')
                        'u' -> {
                            if (index + 4 > source.length) error("Incomplete unicode escape")
                            val hex = source.substring(index, index + 4)
                            out.append(hex.toInt(16).toChar())
                            index += 4
                        }
                        else -> error("Unsupported escape: $escaped")
                    }
                }
                else -> out.append(ch)
            }
        }
        error("Unterminated string")
    }

    private fun parseLiteral(literal: String, value: Any?): Any? {
        if (!source.regionMatches(index, literal, 0, literal.length)) {
            error("Expected $literal at index $index")
        }
        index += literal.length
        return value
    }

    private fun parseNumber(): Number {
        val start = index
        while (index < source.length && source[index] in "-+0123456789.eE") index++
        val raw = source.substring(start, index)
        return raw.toLongOrNull() ?: raw.toDoubleOrNull() ?: error("Invalid number: $raw")
    }

    private fun skipWhitespace() {
        while (index < source.length && source[index].isWhitespace()) index++
    }

    private fun expect(expected: Char) {
        skipWhitespace()
        if (peek() != expected) error("Expected $expected at index $index")
        index++
    }

    private fun peek(expected: Char): Boolean = peek() == expected

    private fun peek(): Char {
        skipWhitespace()
        if (index >= source.length) error("Unexpected end of JSON")
        return source[index]
    }
}
