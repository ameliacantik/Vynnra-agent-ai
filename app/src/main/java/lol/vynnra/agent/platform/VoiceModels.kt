package lol.vynnra.agent.platform

/** High-level voice state exposed to the UI; no hidden reasoning is included. */
enum class VoiceStatus {
    IDLE,
    LISTENING,
    SPEAKING,
    ERROR,
    UNSUPPORTED
}

data class VoiceUiState(
    val status: VoiceStatus = VoiceStatus.IDLE,
    val partialText: String = "",
    val finalText: String = "",
    val errorMessage: String? = null,
    val speechOutput: String? = null,
    val speechReady: Boolean = false,
    val recognitionSupported: Boolean = true
)

object VoiceSessionStateMachine {
    fun startListening(state: VoiceUiState): VoiceUiState =
        state.copy(
            status = VoiceStatus.LISTENING,
            partialText = "",
            finalText = "",
            errorMessage = null,
            speechOutput = null
        )

    fun partialResult(state: VoiceUiState, text: String): VoiceUiState =
        state.copy(
            status = VoiceStatus.LISTENING,
            partialText = text,
            errorMessage = null
        )

    fun finalResult(state: VoiceUiState, text: String): VoiceUiState =
        state.copy(
            status = VoiceStatus.IDLE,
            partialText = "",
            finalText = text,
            errorMessage = null
        )

    fun startSpeaking(state: VoiceUiState, text: String): VoiceUiState =
        state.copy(
            status = VoiceStatus.SPEAKING,
            speechOutput = text,
            errorMessage = null
        )

    fun finishSpeaking(state: VoiceUiState): VoiceUiState =
        state.copy(status = VoiceStatus.IDLE, speechOutput = null)

    fun error(state: VoiceUiState, message: String): VoiceUiState =
        state.copy(status = VoiceStatus.ERROR, errorMessage = message)
}
