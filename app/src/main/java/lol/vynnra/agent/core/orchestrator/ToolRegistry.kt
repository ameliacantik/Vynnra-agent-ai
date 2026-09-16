package lol.vynnra.agent.core.orchestrator

import lol.vynnra.agent.core.tool.ToolDefinition
import lol.vynnra.agent.core.tool.ToolResult
import lol.vynnra.agent.core.tool.VynnraTool

/**
 * Runtime registry. Concrete input decoding belongs to each registered adapter so
 * the orchestrator can remain independent of individual tool implementations.
 */
interface RegisteredTool {
    val definition: ToolDefinition
    suspend fun execute(input: Map<String, Any?>): ToolResult
}

class ToolRegistry {
    private val tools = LinkedHashMap<String, RegisteredTool>()

    fun register(tool: RegisteredTool) {
        require(tool.definition.id.isNotBlank()) { "Tool id cannot be blank" }
        tools[tool.definition.id] = tool
    }

    fun unregister(toolId: String) {
        tools.remove(toolId)
    }

    fun get(toolId: String): RegisteredTool? = tools[toolId]

    fun definitions(): List<ToolDefinition> = tools.values.map { it.definition }

    fun clear() = tools.clear()

    companion object {
        fun <I> adapter(tool: VynnraTool<I>, decoder: (Map<String, Any?>) -> I): RegisteredTool =
            object : RegisteredTool {
                override val definition: ToolDefinition = tool.definition
                override suspend fun execute(input: Map<String, Any?>): ToolResult =
                    tool.execute(decoder(input))
            }
    }
}
