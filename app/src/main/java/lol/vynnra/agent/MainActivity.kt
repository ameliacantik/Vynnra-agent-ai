package lol.vynnra.agent

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
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
import lol.vynnra.agent.core.orchestrator.OrchestratorResult
import lol.vynnra.agent.core.provider.OpenAiCompatibleProvider
import lol.vynnra.agent.core.provider.ProviderConfig
import lol.vynnra.agent.core.voice.VoiceAgentResult
import lol.vynnra.agent.core.voice.VoiceAgentSession
import lol.vynnra.agent.core.voice.VoiceAgentUiState
import lol.vynnra.agent.platform.ProviderCredentialStore
import lol.vynnra.agent.platform.ScreenCaptureConsentController
import lol.vynnra.agent.platform.ScreenCaptureController
import lol.vynnra.agent.platform.VoiceController
import lol.vynnra.agent.platform.VynnraAgentRuntime
import lol.vynnra.agent.ui.VynnraAgentShell
import lol.vynnra.agent.ui.theme.VynnraTheme

class MainActivity : ComponentActivity() {
    private lateinit var screenCaptureConsentController: ScreenCaptureConsentController
    private lateinit var screenCaptureController: ScreenCaptureController
    private lateinit var voiceController: VoiceController
    private lateinit var providerCredentialStore: ProviderCredentialStore
    private lateinit var voiceAgentSession: VoiceAgentSession
    private lateinit var agentRuntime: VynnraAgentRuntime

    private var agentJob: Job? = null
    private var screenCaptureGranted by mutableStateOf(false)
    private var microphoneGranted by mutableStateOf(false)
    private var accessibilityGranted by mutableStateOf(false)
    private var filesGranted by mutableStateOf(false)
    private var overlayGranted by mutableStateOf(false)
    private var providerConfig by mutableStateOf(ProviderConfig("", "", ""))
    private var voiceAgentState by mutableStateOf(VoiceAgentUiState())

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
        refreshPermissionState()

        val app = application as VynnraApplication
        val provider = OpenAiCompatibleProvider { providerCredentialStore.load() }
        voiceAgentSession = VoiceAgentSession(
            provider = provider,
            taskRepository = app.taskRepository,
            modelProvider = { providerCredentialStore.load().model }
        )
        agentRuntime = VynnraAgentRuntime(
            context = this,
            provider = provider,
            memoryRepository = app.memoryRepository,
            taskRepository = app.taskRepository,
            modelProvider = { providerCredentialStore.load().model },
            microphoneGranted = { microphoneGranted }
        )

        setContent {
            VynnraTheme {
                val voiceState by voiceController.state.collectAsStateWithLifecycle()
                val orchestratorState by agentRuntime.orchestrator.state.collectAsStateWithLifecycle()
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
                    accessibilityGranted = accessibilityGranted,
                    filesGranted = filesGranted,
                    overlayGranted = overlayGranted,
                    voiceState = voiceState,
                    voiceAgentState = voiceAgentState,
                    orchestratorState = orchestratorState,
                    providerConfig = providerConfig,
                    onRequestMicrophone = {
                        microphoneLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onRequestAccessibility = {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    onRequestFiles = { openFilesSettings() },
                    onRequestOverlay = { openOverlaySettings() },
                    onStartVoice = { voiceController.startListening() },
                    onStopVoice = { voiceController.stopListening() },
                    onStopSpeaking = { voiceController.stopSpeaking() },
                    onStopAgent = { stopAgent() },
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

    override fun onResume() {
        super.onResume()
        if (::voiceController.isInitialized) {
            refreshPermissionState()
        }
    }

    private fun refreshPermissionState() {
        microphoneGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        accessibilityGranted = lol.vynnra.agent.platform.VynnraAccessibilityService.current() != null
        filesGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
        overlayGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)
    }

    private fun openFilesSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            })
        }
    }

    private fun openOverlaySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            })
        }
    }

    private fun submitAgentRequest(
        text: String,
        thinkingLevel: ThinkingLevel,
        speakResponse: Boolean
    ) {
        val normalized = text.trim()
        if (normalized.isEmpty()) return
        agentJob?.cancel()

        agentJob = lifecycleScope.launch {
            voiceAgentState = VoiceAgentUiState(busy = true, activity = "Understanding…")
            when (val result = agentRuntime.orchestrator.run(normalized, thinkingLevel)) {
                is OrchestratorResult.Completed -> {
                    voiceAgentState = VoiceAgentUiState(
                        busy = false,
                        activity = "Response ready",
                        replyId = System.nanoTime(),
                        reply = result.message,
                        error = null
                    )
                    if (speakResponse) voiceController.speak(result.message)
                }
                is OrchestratorResult.Blocked -> {
                    voiceAgentState = VoiceAgentUiState(
                        busy = false,
                        activity = "Action blocked",
                        error = "Vynnra needs an Android capability or user confirmation before continuing."
                    )
                }
                is OrchestratorResult.Cancelled -> {
                    voiceAgentState = VoiceAgentUiState(
                        busy = false,
                        activity = "Stopped",
                        error = null
                    )
                }
                is OrchestratorResult.Failed -> {
                    voiceAgentState = VoiceAgentUiState(
                        busy = false,
                        activity = "Request failed",
                        error = result.message
                    )
                }
            }
        }
    }

    private fun stopAgent() {
        agentRuntime.orchestrator.requestStop()
        agentJob?.cancel()
        agentJob = null
        voiceAgentState = voiceAgentState.copy(busy = false, activity = "Stopped", error = null)
    }

    override fun onDestroy() {
        agentRuntime.orchestrator.requestStop()
        agentJob?.cancel()
        voiceController.shutdown()
        super.onDestroy()
    }
}