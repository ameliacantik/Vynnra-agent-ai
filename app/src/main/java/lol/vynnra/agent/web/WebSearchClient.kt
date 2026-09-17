package lol.vynnra.agent.web

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Client for the Vynnra server-side web-search service. The Tavily secret never enters the APK.
 */
class WebSearchClient(
    private val baseUrlProvider: () -> String,
    private val authTokenProvider: () -> String? = { null }
) {
    suspend fun search(request: WebSearchRequest): WebSearchResponse = withContext(Dispatchers.IO) {
        postJson("/v1/web/search", request.toJson()) { body -> WebSearchResponse.fromJson(body) }
    }

    suspend fun research(request: ResearchRequest): ResearchResponse = withContext(Dispatchers.IO) {
        postJson("/v1/web/research", request.toJson()) { body -> ResearchResponse.fromJson(body) }
    }

    suspend fun extract(urls: List<String>): ExtractResponse = withContext(Dispatchers.IO) {
        postJson("/v1/web/extract", JSONObject().put("urls", JSONArray(urls.take(10)))) { body ->
            ExtractResponse.fromJson(body)
        }
    }

    private fun <T> postJson(path: String, payload: JSONObject, parse: (JSONObject) -> T): T {
        val baseUrl = baseUrlProvider().trim().trimEnd('/')
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            throw WebSearchException("Web-search server URL is not configured.")
        }
        val connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            authTokenProvider()?.takeIf { it.isNotBlank() }?.let {
                setRequestProperty("Authorization", "Bearer $it")
            }
        }
        try {
            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = if (body.isBlank()) JSONObject() else JSONObject(body)
            if (status !in 200..299) {
                throw WebSearchException(
                    message = json.optString("error", "Web-search request failed ($status)"),
                    retryable = json.optBoolean("retryable", status == 429 || status >= 500)
                )
            }
            if (!json.optBoolean("ok", false)) throw WebSearchException("Web-search server returned an invalid response.")
            return parse(json.optJSONObject("data") ?: JSONObject())
        } catch (error: WebSearchException) {
            throw error
        } catch (error: IOException) {
            throw WebSearchException("Web-search connection failed: ${error.message ?: "unknown error"}", true)
        } finally {
            connection.disconnect()
        }
    }
}

data class WebSearchRequest(
    val query: String,
    val searchDepth: String = "basic",
    val topic: String = "general",
    val maxResults: Int = 5,
    val includeRawContent: Boolean = false,
    val includeDomains: List<String> = emptyList(),
    val excludeDomains: List<String> = emptyList()
) {
    fun toJson(): JSONObject = JSONObject()
        .put("query", query)
        .put("searchDepth", searchDepth)
        .put("topic", topic)
        .put("maxResults", maxResults)
        .put("includeRawContent", includeRawContent)
        .put("includeDomains", JSONArray(includeDomains.take(20)))
        .put("excludeDomains", JSONArray(excludeDomains.take(20)))
}

data class ResearchRequest(
    val query: String,
    val followUpQueries: List<String> = emptyList(),
    val searchDepth: String = "basic",
    val topic: String = "general",
    val maxResults: Int = 5
) {
    fun toJson(): JSONObject = JSONObject()
        .put("query", query)
        .put("followUpQueries", JSONArray(followUpQueries.take(3)))
        .put("searchDepth", searchDepth)
        .put("topic", topic)
        .put("maxResults", maxResults)
}

data class WebSource(
    val title: String,
    val url: String,
    val snippet: String,
    val score: Double?,
    val favicon: String?
)

data class WebSearchResponse(
    val query: String,
    val answer: String?,
    val sources: List<WebSource>,
    val usage: JSONObject?
) {
    companion object {
        fun fromJson(json: JSONObject): WebSearchResponse = WebSearchResponse(
            query = json.optString("query"),
            answer = json.optString("answer").takeIf { it.isNotBlank() },
            sources = parseSources(json.optJSONArray("results")),
            usage = json.optJSONObject("usage")
        )
    }
}

data class ResearchResponse(
    val query: String,
    val answer: String?,
    val sources: List<WebSource>,
    val queryCount: Int
) {
    companion object {
        fun fromJson(json: JSONObject): ResearchResponse = ResearchResponse(
            query = json.optString("query"),
            answer = json.optString("answer").takeIf { it.isNotBlank() },
            sources = parseSources(json.optJSONArray("sources")),
            queryCount = json.optInt("queryCount", 1)
        )
    }
}

data class ExtractResponse(val results: JSONArray) {
    companion object {
        fun fromJson(json: JSONObject): ExtractResponse =
            ExtractResponse(json.optJSONArray("results") ?: JSONArray())
    }
}

private fun parseSources(array: JSONArray?): List<WebSource> {
    if (array == null) return emptyList()
    return buildList {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val url = item.optString("url").takeIf { it.isNotBlank() } ?: continue
            add(
                WebSource(
                    title = item.optString("title", url),
                    url = url,
                    snippet = item.optString("snippet", item.optString("content")),
                    score = if (item.has("score")) item.optDouble("score") else null,
                    favicon = item.optString("favicon").takeIf { it.isNotBlank() }
                )
            )
        }
    }
}

class WebSearchException(message: String, val retryable: Boolean = false) : RuntimeException(message)
