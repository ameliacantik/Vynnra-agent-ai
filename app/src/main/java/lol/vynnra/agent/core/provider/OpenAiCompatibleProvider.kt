package lol.vynnra.agent.core.provider

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class OpenAiCompatibleProvider(
    private val configProvider: () -> ProviderConfig
) : AiProvider {
    override val id: String = "openai-compatible"
    override val displayName: String = "OpenAI-compatible"

    suspend fun fetchModels(config: ProviderConfig = configProvider()): List<String> =
        withContext(Dispatchers.IO) {
            require(config.hasEndpoint()) { "Provider URL is empty." }
            val connection = openConnection(
                url = "${config.normalizedBaseUrl()}/models",
                method = "GET",
                config = config
            )
            try {
                val (status, payload) = readResponse(connection)
                if (status !in 200..299) {
                    throw IllegalStateException(
                        "Models endpoint HTTP $status: ${sanitizeError(payload)}"
                    )
                }
                parseModels(payload)
            } finally {
                connection.disconnect()
            }
        }

    suspend fun testConnection(config: ProviderConfig = configProvider()): ProviderConnectionResult =
        withContext(Dispatchers.IO) {
            require(config.hasEndpoint()) { "Provider URL is empty." }
            runCatching {
                val models = runCatching { fetchModels(config) }.getOrElse { error ->
                    if (error is IllegalStateException && error.message?.contains("HTTP 404") == true) {
                        emptyList()
                    } else {
                        throw error
                    }
                }

                if (models.isNotEmpty()) {
                    ProviderConnectionResult(
                        connected = true,
                        message = "Connected · ${models.size} model${if (models.size == 1) "" else "s"} detected",
                        models = models
                    )
                } else {
                    val selectedModel = config.model.trim()
                    if (selectedModel.isEmpty()) {
                        ProviderConnectionResult(
                            connected = false,
                            message = "Provider is reachable, but /models is unavailable. Enter a model ID manually.",
                            models = emptyList()
                        )
                    } else {
                        lightweightChatProbe(config, selectedModel)
                        ProviderConnectionResult(
                            connected = true,
                            message = "Connected · manual model '$selectedModel'",
                            models = listOf(selectedModel)
                        )
                    }
                }
            }.getOrElse { error ->
                ProviderConnectionResult(
                    connected = false,
                    message = providerErrorMessage(error),
                    models = emptyList()
                )
            }
        }

    override suspend fun complete(request: AiRequest): AiResponse =
        withContext(Dispatchers.IO) {
            val config = configProvider()
            require(config.hasEndpoint()) {
                "AI provider is not configured. Set a provider URL in Settings."
            }

            val model = request.model.ifBlank { config.model.trim() }
            require(model.isNotBlank()) {
                "AI model is not configured. Tap Refresh models or enter a model ID."
            }

            val connection = openConnection(
                url = "${config.normalizedBaseUrl()}/chat/completions",
                method = "POST",
                config = config
            )

            try {
                val body = JSONObject().apply {
                    put("model", model)
                    put("messages", request.messages.toJsonArray())
                    request.temperature?.let { put("temperature", it) }
                    request.maxTokens?.let { put("max_tokens", it) }
                }.toString()

                connection.outputStream.use { output ->
                    output.write(body.toByteArray(Charsets.UTF_8))
                }

                val (status, payload) = readResponse(connection)
                if (status !in 200..299) {
                    throw IllegalStateException(
                        "AI provider HTTP $status: ${sanitizeError(payload)}"
                    )
                }
                parseResponse(payload, model)
            } finally {
                connection.disconnect()
            }
        }

    override fun stream(request: AiRequest): Flow<AiStreamEvent> = flow {
        try {
            val response = complete(request)
            emit(AiStreamEvent.Delta(response.content))
            emit(AiStreamEvent.Completed(response))
        } catch (error: Throwable) {
            emit(
                AiStreamEvent.Error(
                    error.message ?: "AI provider request failed",
                    retryable = true
                )
            )
        }
    }

    private fun lightweightChatProbe(config: ProviderConfig, model: String) {
        val connection = openConnection(
            url = "${config.normalizedBaseUrl()}/chat/completions",
            method = "POST",
            config = config
        )
        try {
            val body = JSONObject().apply {
                put("model", model)
                put(
                    "messages",
                    JSONArray().put(
                        JSONObject().apply {
                            put("role", "user")
                            put("content", "Reply with OK")
                        }
                    )
                )
                put("temperature", 0.0)
                put("max_tokens", 1)
            }.toString()
            connection.outputStream.use { output ->
                output.write(body.toByteArray(Charsets.UTF_8))
            }
            val (status, payload) = readResponse(connection)
            if (status !in 200..299) {
                throw IllegalStateException(
                    "Chat probe HTTP $status: ${sanitizeError(payload)}"
                )
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseModels(payload: String): List<String> {
        val root = JSONObject(payload)
        val data = root.optJSONArray("data")
            ?: root.optJSONArray("models")
            ?: root.optJSONArray("items")
            ?: JSONArray()

        return buildList {
            for (index in 0 until data.length()) {
                val item = data.opt(index)
                val id = when (item) {
                    is JSONObject -> item.optString("id")
                        .ifBlank { item.optString("name") }
                        .ifBlank { item.optString("model") }
                    is String -> item
                    else -> ""
                }.trim()
                if (id.isNotEmpty()) add(id)
            }
        }.distinct().sorted()
    }

    private fun parseResponse(payload: String, requestedModel: String): AiResponse {
        val root = JSONObject(payload)
        val choices = root.optJSONArray("choices")
            ?: throw IllegalStateException("AI provider response did not contain choices")
        if (choices.length() == 0) {
            throw IllegalStateException("AI provider returned no choices")
        }

        val choice = choices.optJSONObject(0)
            ?: throw IllegalStateException("AI provider returned an invalid choice")
        val message = choice.optJSONObject("message")
            ?: throw IllegalStateException("AI provider response did not contain a message")
        val content = message.optString("content").trim()
        if (content.isBlank()) {
            throw IllegalStateException("AI provider returned an empty response")
        }

        return AiResponse(
            content = content,
            model = root.optString("model", requestedModel).ifBlank { requestedModel },
            finishReason = choice.optString("finish_reason").ifBlank { null }
        )
    }

    private fun List<AiMessage>.toJsonArray(): JSONArray =
        JSONArray().apply {
            forEach { message ->
                put(
                    JSONObject().apply {
                        put("role", message.role.name.lowercase())
                        put("content", message.content)
                    }
                )
            }
        }

    private fun openConnection(
        url: String,
        method: String,
        config: ProviderConfig
    ): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 120_000
            useCaches = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Vynnra-Agent/0.2")
            config.authorizationHeader()?.let { setRequestProperty("Authorization", it) }
            if (method == "POST") {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }

    private fun readResponse(connection: HttpURLConnection): Pair<Int, String> {
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val payload = stream?.let { input ->
            BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { it.readText() }
        }.orEmpty()
        return status to payload
    }

    private fun providerErrorMessage(error: Throwable): String {
        val raw = error.message?.trim().orEmpty()
        if (raw.contains("Unable to resolve host", ignoreCase = true)) {
            return "Provider host could not be resolved. Check the URL and internet connection."
        }
        if (raw.contains("Cleartext HTTP traffic", ignoreCase = true)) {
            return "HTTP provider blocked by Android. Use HTTPS for the custom provider."
        }
        if (raw.contains("timeout", ignoreCase = true)) {
            return "Provider timed out. Check server availability and response latency."
        }
        if (raw.isNotEmpty()) return raw.take(700)
        return "Unable to connect to the custom provider."
    }

    private fun sanitizeError(payload: String): String =
        payload.replace(Regex("\\s+"), " ").trim().take(700)
}

data class ProviderConnectionResult(
    val connected: Boolean,
    val message: String,
    val models: List<String>
)
