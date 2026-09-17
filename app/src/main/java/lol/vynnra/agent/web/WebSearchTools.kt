package lol.vynnra.agent.web

import lol.vynnra.agent.core.tool.Capability
import lol.vynnra.agent.core.tool.RiskLevel
import lol.vynnra.agent.core.tool.ToolDefinition
import lol.vynnra.agent.core.tool.ToolResult
import lol.vynnra.agent.core.tool.ToolResultStatus
import lol.vynnra.agent.core.tool.VynnraTool

class TavilySearchTool(private val client: WebSearchClient) : VynnraTool<SearchToolInput> {
    override val definition = ToolDefinition(
        id = "web.tavily_search",
        name = "Tavily Web Search",
        description = "Search the web through the Vynnra server-side Tavily service.",
        requiredCapabilities = setOf(Capability.WEB_SEARCH),
        riskLevel = RiskLevel.LOW,
        supportsVerification = true
    )

    override suspend fun execute(input: SearchToolInput): ToolResult = try {
        val response = client.search(
            WebSearchRequest(
                query = input.query,
                searchDepth = input.searchDepth,
                topic = input.topic,
                maxResults = input.maxResults,
                includeRawContent = input.includeRawContent,
                includeDomains = input.includeDomains,
                excludeDomains = input.excludeDomains
            )
        )
        ToolResult(
            status = ToolResultStatus.SUCCESS,
            data = mapOf(
                "query" to response.query,
                "answer" to response.answer,
                "sources" to response.sources.map { source ->
                    mapOf(
                        "title" to source.title,
                        "url" to source.url,
                        "snippet" to source.snippet,
                        "score" to source.score,
                        "favicon" to source.favicon
                    )
                }
            ),
            message = "Web search completed"
        )
    } catch (error: WebSearchException) {
        ToolResult(
            status = if (error.retryable) ToolResultStatus.PARTIAL else ToolResultStatus.FAILED,
            message = error.message
        )
    }
}

data class SearchToolInput(
    val query: String,
    val searchDepth: String = "basic",
    val topic: String = "general",
    val maxResults: Int = 5,
    val includeRawContent: Boolean = false,
    val includeDomains: List<String> = emptyList(),
    val excludeDomains: List<String> = emptyList()
)

class WebResearchTool(private val client: WebSearchClient) : VynnraTool<ResearchToolInput> {
    override val definition = ToolDefinition(
        id = "web.research",
        name = "Research with Tavily",
        description = "Run a bounded multi-query research workflow and return deduplicated sources.",
        requiredCapabilities = setOf(Capability.WEB_SEARCH),
        riskLevel = RiskLevel.LOW,
        supportsVerification = true
    )

    override suspend fun execute(input: ResearchToolInput): ToolResult = try {
        val response = client.research(
            ResearchRequest(
                query = input.query,
                followUpQueries = input.followUpQueries,
                searchDepth = input.searchDepth,
                topic = input.topic,
                maxResults = input.maxResults
            )
        )
        ToolResult(
            status = ToolResultStatus.SUCCESS,
            data = mapOf(
                "query" to response.query,
                "answer" to response.answer,
                "queryCount" to response.queryCount,
                "sources" to response.sources.map { source ->
                    mapOf(
                        "title" to source.title,
                        "url" to source.url,
                        "snippet" to source.snippet,
                        "score" to source.score,
                        "favicon" to source.favicon
                    )
                }
            ),
            message = "Research completed"
        )
    } catch (error: WebSearchException) {
        ToolResult(
            status = if (error.retryable) ToolResultStatus.PARTIAL else ToolResultStatus.FAILED,
            message = error.message
        )
    }
}

data class ResearchToolInput(
    val query: String,
    val followUpQueries: List<String> = emptyList(),
    val searchDepth: String = "basic",
    val topic: String = "general",
    val maxResults: Int = 5
)
