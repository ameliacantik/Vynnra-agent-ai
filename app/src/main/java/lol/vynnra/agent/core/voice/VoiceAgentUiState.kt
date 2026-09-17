package lol.vynnra.agent.core.voice

data class VoiceAgentUiState(
    val busy: Boolean = false,
    val activity: String? = null,
    val replyId: Long = 0L,
    val reply: String? = null,
    val error: String? = null
)
