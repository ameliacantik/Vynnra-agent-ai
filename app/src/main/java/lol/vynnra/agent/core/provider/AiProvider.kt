package lol.vynnra.agent.core.provider

import kotlinx.coroutines.flow.Flow

interface AiProvider {
    val id: String
    val displayName: String

    suspend fun complete(request: AiRequest): AiResponse

    fun stream(request: AiRequest): Flow<AiStreamEvent>
}

data class AiRequest(
    val model: String,
    val messages: List<AiMessage>,
    val temperature: Double? = null,
    val maxTokens: Int? = null
)

data class AiMessage(val role: Role, val content: String) {
    enum class Role { SYSTEM, USER, ASSISTANT, TOOL }
}

data class AiResponse(
    val content: String,
    val model: String,
    val finishReason: String? = null
)

sealed interface AiStreamEvent {
    data class Delta(val text: String) : AiStreamEvent
    data class Completed(val response: AiResponse) : AiStreamEvent
    data class Error(val message: String, val retryable: Boolean = true) : AiStreamEvent
}
