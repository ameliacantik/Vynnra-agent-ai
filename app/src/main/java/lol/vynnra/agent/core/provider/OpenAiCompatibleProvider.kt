package lol.vynnra.agent.core.provider

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class OpenAiCompatibleProvider(
    private val configProvider: () -> ProviderConfig
) : AiProvider {
    override val id: String = "openai-compatible"
    override val displayName: String = "OpenAI-compatible"

    override suspend fun complete(request: AiRequest): AiResponse {
        val config = configProvider()
        require(config.isConfigured()) { "AI provider is not configured. Set base URL, API key, and model in Settings." }

        val connection = (URL("${config.normalizedBaseUrl()}/chat/completions").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 120_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer ${config.apiKey.trim()}")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }

        try {
            val body = JSONObject().apply {
                put("model", request.model.ifBlank { config.model.trim() })
                put("messages", request.messages.toJsonArray())
                request.temperature?.let { put("temperature", it) }
                request.maxTokens?.let { put("max_tokens", it) }
            }.toString()

            connection.outputStream.use { output ->
                output.write(body.toByteArray(Charsets.UTF_8))
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val payload = stream?.let { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { it.readText() }
            }.orEmpty()

            if (status !in 200..299) {
                throw IllegalStateException("AI provider HTTP $status: ${sanitizeError(payload)}")
            }
            return parseResponse(payload, request.model.ifBlank { config.model.trim() })
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
            emit(AiStreamEvent.Error(error.message ?: "AI provider request failed", retryable = true))
        }
    }

    private fun parseResponse(payload: String, requestedModel: String): AiResponse {
        val root = JSONObject(payload)
        val choices = root.optJSONArray("choices")
            ?: throw IllegalStateException("AI provider response did not contain choices")
        if (choices.length() == 0) throw IllegalStateException("AI provider returned no choices")

        val choice = choices.optJSONObject(0)
            ?: throw IllegalStateException("AI provider returned an invalid choice")
        val message = choice.optJSONObject("message")
            ?: throw IllegalStateException("AI provider response did not contain a message")
        val content = message.optString("content").trim()
        if (content.isBlank()) throw IllegalStateException("AI provider returned an empty response")

        return AiResponse(
            content = content,
            model = root.optString("model", requestedModel).ifBlank { requestedModel },
            finishReason = choice.optString("finish_reason").ifBlank { null }
        )
    }

    private fun List<AiMessage>.toJsonArray(): org.json.JSONArray =
        org.json.JSONArray().apply {
            forEach { message ->
                put(
                    JSONObject().apply {
                        put("role", message.role.name.lowercase())
                        put("content", message.content)
                    }
                )
            }
        }

    private fun sanitizeError(payload: String): String =
        payload.replace(Regex("\\s+"), " ").trim().take(500)
}
