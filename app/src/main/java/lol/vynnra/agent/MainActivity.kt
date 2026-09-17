package lol.vynnra.agent

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import lol.vynnra.agent.core.agent.ThinkingLevel
import lol.vynnra.agent.core.provider.OpenAiCompatibleProvider
import lol.vynnra.agent.core.provider.ProviderConfig
import lol.vynnra.agent.core.voice.VoiceAgentUiState
import lol.vynnra.agent.platform.AgentRuntime
import lol.vynnra.agent.platform.ProviderCredentialStore
import lol.vynnra.agent.platform.ScreenCaptureConsentController
import lol.vynnra.agent.platform.ScreenCaptureController
import lol.vynnra.agent.platform.VoiceController
import lol.vynnra.agent.ui.VynnraAgentShell
import lol.vynnra.agent.ui.theme.VynnraTheme

class MainActivity : ComponentActivity() {
    private lateinit var screenCaptureConsentController: ScreenCaptureConsentController
    private lateinit var screenCaptureController: ScreenCaptureController
    private lateinit var voiceController: VoiceController
    private lateinit var providerCredentialStore: ProviderCredentialStore
    private lateinit var agentRuntime: AgentRuntime

    private var screenCaptureGranted by mutableStateOf(false)
    private var microphoneGranted by mutableStateOf(false)
    private var providerConfig by mutableStateOf(ProviderConfig("", "", ""))
    private var voiceAgentState by mutableStateOf(VoiceAgentUiState())
    private var agentJob: Job? = null

    private val screenCaptureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            screenCaptureGranted = screenCaptureConsentController.handleActivityResult(
                result.resultCode,
                result.data
            )
            screenCaptureConsentController.currentGrant()?.let { grant ->
                screenCaptureController.start(grant)
            }
        }

    private val microphoneLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            microphoneGranted = granted
            if (granted) voiceController.startListening()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        screenCaptureConsentController = ScreenCaptureConsentController(this)
        screenCaptureController = ScreenCaptureController(this)
        voiceController = VoiceController(this)
        providerCredentialStore = ProviderCredentialStore(this)
        providerConfig = providerCredentialStore.load()

        screenCaptureGranted = screenCaptureConsentController.isGranted()
        microphoneGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val app = application as VynnraApplication
        val provider = OpenAiCompatibleProvider { providerCredentialStore.load() }
        agentRuntime = AgentRuntime(
            context = this,
            provider = provider,
            modelProvider = { providerCredentialStore.load().model },
            memoryRepository = app.memoryRepository,
            taskRepository = app.taskRepository
        )

        setContent {
            VynnraTheme {
                val voiceState by voiceController.state.collectAsStateWithLifecycle()
                VynnraAgentShell(
                    memoryRepository = app.memoryRepository,
                    taskRepository = app.taskRepository,
                    screenCaptureGranted = screenCaptureGranted,
                    onRequestScreenCapture = {
                        screenCaptureLauncher.launch(
                            screenCaptureConsentController.createConsentIntent()
                        )
                    },
                    microphoneGranted = microphoneGranted,
                    voiceState = voiceState,
                    voiceAgentState = voiceAgentState,
                    providerConfig = providerConfig,
                    onRequestMicrophone = {
                        microphoneLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onStartVoice = { voiceController.startListening() },
                    onStopVoice = { voiceController.stopListening() },
                    onStopSpeaking = { voiceController.stopSpeaking() },
                    onStopAgent = {
                        agentRuntime.orchestrator.requestStop()
                        agentJob?.cancel()
                    },
                    onSendMessage = { text, thinkingLevel, speakResponse ->
                        submitAgentRequest(text, thinkingLevel, speakResponse)
                    },
                    onSaveProviderConfig = { config ->
                        providerCredentialStore.save(config)
                        providerConfig = config
                    }
                )
            }
        }
    }

    private fun submitAgentRequest(
        text: String,
        thinkingLevel: ThinkingLevel,
        speakResponse: Boolean
    ) {
        val normalized = text.trim()
        if (normalized.isEmpty() || voiceAgentState.busy) return

        agentJob?.cancel()
        agentJob = lifecycleScope.launch {
            voiceAgentState = VoiceAgentUiState(busy = true, activity = "Thinking…")
            when (val result = agentRuntime.chatSession.submit(normalized, thinkingLevel)) {
                is lol.vynnra.agent.core.orchestrator.AgentChatResult.Success -> {
                    voiceAgentState = voiceAgentState.copy(
                        busy = false,
                        activity = if (result.usedTools) "Agent completed verified actions" else "Response ready",
                        replyId = System.nanoTime(),
                        reply = result.text,
                        error = null
                    )
                    if (speakResponse) voiceController.speak(result.text)
                }
                is lol.vynnra.agent.core.orchestrator.AgentChatResult.Blocked -> {
                    voiceAgentState = voiceAgentState.copy(
                        busy = false,
                        activity = "Waiting for capability",
                        error = result.message
                    )
                }
                is lol.vynnra.agent.core.orchestrator.AgentChatResult.Failed -> {
                    voiceAgentState = voiceAgentState.copy(
                        busy = false,
                        activity = "Agent failed",
                        error = result.message
                    )
                }
                is lol.vynnra.agent.core.orchestrator.AgentChatResult.Cancelled -> {
                    voiceAgentState = voiceAgentState.copy(
                        busy = false,
                        activity = "Agent cancelled",
                        error = null
                    )
                }
            }
        }.also { job ->
            job.invokeOnCompletion { if (agentJob === job) agentJob = null }
        }
    }

    override fun onDestroy() {
        agentJob?.cancel()

        voiceController.shutdown()
        super.onDestroy()
    }
}
