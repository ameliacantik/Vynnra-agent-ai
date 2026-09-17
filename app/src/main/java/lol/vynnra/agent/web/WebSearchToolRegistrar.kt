package lol.vynnra.agent.web

import lol.vynnra.agent.core.orchestrator.ToolRegistry

/** Registers Phase 8 web-search tools after the server endpoint is configured. */
object WebSearchToolRegistrar {
    fun registerAll(
        registry: ToolRegistry,
        baseUrlProvider: () -> String,
        authTokenProvider: () -> String? = { null }
    ) {
        val client = WebSearchClient(baseUrlProvider, authTokenProvider)
        registry.register(ToolRegistry.adapter(TavilySearchTool(client)) { input ->
            SearchToolInput(
                query = input.requiredString("query"),
                searchDepth = input.optionalString("searchDepth") ?: "basic",
                topic = input.optionalString("topic") ?: "general",
                maxResults = (input["maxResults"] as? Number)?.toInt()?.coerceIn(1, 10) ?: 5,
                includeRawContent = input["includeRawContent"] as? Boolean ?: false,
                includeDomains = input["includeDomains"].stringList(),
                excludeDomains = input["excludeDomains"].stringList()
            )
        })
        registry.register(ToolRegistry.adapter(WebResearchTool(client)) { input ->
            ResearchToolInput(
                query = input.requiredString("query"),
                followUpQueries = input["followUpQueries"].stringList().take(3),
                searchDepth = input.optionalString("searchDepth") ?: "basic",
                topic = input.optionalString("topic") ?: "general",
                maxResults = (input["maxResults"] as? Number)?.toInt()?.coerceIn(1, 10) ?: 5
            )
        })
    }

    private fun Map<String, Any?>.requiredString(key: String): String =
        (this[key] as? String)?.trim()?.takeIf { it.isNotEmpty() }
            ?: error("Missing or blank input: $key")

    private fun Map<String, Any?>.optionalString(key: String): String? =
        (this[key] as? String)?.trim()?.takeIf { it.isNotEmpty() }

    private fun Any?.stringList(): List<String> =
        (this as? List<*>)?.filterIsInstance<String>()?.map(String::trim)?.filter(String::isNotEmpty).orEmpty()
}
