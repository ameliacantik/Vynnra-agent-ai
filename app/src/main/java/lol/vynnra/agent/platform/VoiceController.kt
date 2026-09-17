package lol.vynnra.agent.platform

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VoiceController(context: Context) : RecognitionListener, TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private val _state = MutableStateFlow(
        VoiceUiState(recognitionSupported = SpeechRecognizer.isRecognitionAvailable(appContext))
    )
    val state: StateFlow<VoiceUiState> = _state.asStateFlow()

    private val recognizer: SpeechRecognizer? =
        if (SpeechRecognizer.isRecognitionAvailable(appContext)) {
            SpeechRecognizer.createSpeechRecognizer(appContext).also { it.setRecognitionListener(this) }
        } else {
            null
        }

    private val textToSpeech = TextToSpeech(appContext, this).apply {
        setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _state.value = VoiceSessionStateMachine.startSpeaking(_state.value, _state.value.speechOutput.orEmpty())
            }

            override fun onDone(utteranceId: String?) {
                _state.value = VoiceSessionStateMachine.finishSpeaking(_state.value)
            }

            override fun onError(utteranceId: String?) {
                _state.value = VoiceSessionStateMachine.error(_state.value, "Text-to-speech failed.")
            }
        })
    }

    fun startListening(locale: Locale = Locale.getDefault()) {
        val speechRecognizer = recognizer
        if (speechRecognizer == null) {
            _state.value = _state.value.copy(
                status = VoiceStatus.UNSUPPORTED,
                recognitionSupported = false,
                errorMessage = "Speech recognition is unavailable on this device."
            )
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        _state.value = VoiceSessionStateMachine.startListening(_state.value)
        try {
            speechRecognizer.startListening(intent)
        } catch (error: Throwable) {
            _state.value = VoiceSessionStateMachine.error(
                _state.value,
                error.message ?: "Unable to start speech recognition."
            )
        }
    }

    fun stopListening() {
        recognizer?.stopListening()
        if (_state.value.status == VoiceStatus.LISTENING) {
            _state.value = _state.value.copy(status = VoiceStatus.IDLE)
        }
    }

    fun speak(text: String, locale: Locale = Locale.getDefault()) {
        val normalized = text.trim()
        if (normalized.isEmpty()) return
        if (!_state.value.speechReady) {
            _state.value = VoiceSessionStateMachine.error(_state.value, "Text-to-speech is not ready yet.")
            return
        }

        val languageResult = textToSpeech.setLanguage(locale)
        if (languageResult == TextToSpeech.LANG_MISSING_DATA || languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            _state.value = VoiceSessionStateMachine.error(
                _state.value,
                "The selected text-to-speech language is unavailable."
            )
            return
        }

        _state.value = VoiceSessionStateMachine.startSpeaking(_state.value, normalized)
        textToSpeech.speak(normalized, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    fun stopSpeaking() {
        textToSpeech.stop()
        _state.value = VoiceSessionStateMachine.finishSpeaking(_state.value)
    }

    fun shutdown() {
        recognizer?.destroy()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.setLanguage(Locale.getDefault())
            _state.value = _state.value.copy(speechReady = true, errorMessage = null)
        } else {
            _state.value = VoiceSessionStateMachine.error(
                _state.value,
                "Text-to-speech initialization failed."
            )
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {
        _state.value = _state.value.copy(status = VoiceStatus.LISTENING, errorMessage = null)
    }

    override fun onBeginningOfSpeech() {
        _state.value = _state.value.copy(status = VoiceStatus.LISTENING, errorMessage = null)
    }

    override fun onRmsChanged(rmsdB: Float) = Unit

    override fun onBufferReceived(buffer: ByteArray?) = Unit

    override fun onEndOfSpeech() {
        if (_state.value.status == VoiceStatus.LISTENING) {
            _state.value = _state.value.copy(status = VoiceStatus.IDLE)
        }
    }

    override fun onError(error: Int) {
        _state.value = VoiceSessionStateMachine.error(_state.value, recognitionErrorMessage(error))
    }

    override fun onResults(results: Bundle?) {
        val text = results
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.trim()
            .orEmpty()
        _state.value = if (text.isEmpty()) {
            _state.value.copy(status = VoiceStatus.IDLE, partialText = "")
        } else {
            VoiceSessionStateMachine.finalResult(_state.value, text)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val text = partialResults
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.trim()
            .orEmpty()
        if (text.isNotEmpty()) {
            _state.value = VoiceSessionStateMachine.partialResult(_state.value, text)
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) = Unit

    private fun recognitionErrorMessage(errorCode: Int): String = when (errorCode) {
        SpeechRecognizer.ERROR_AUDIO -> "Microphone audio error."
        SpeechRecognizer.ERROR_CLIENT -> "Speech recognizer client error."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
        SpeechRecognizer.ERROR_NETWORK -> "Speech recognition network error."
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition timed out."
        SpeechRecognizer.ERROR_NO_MATCH -> "No speech was recognized."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy."
        SpeechRecognizer.ERROR_SERVER -> "Speech recognition server error."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech was detected before timeout."
        else -> "Speech recognition failed (code $errorCode)."
    }

    private companion object {
        const val UTTERANCE_ID = "vynnra-agent-voice"
    }
}
