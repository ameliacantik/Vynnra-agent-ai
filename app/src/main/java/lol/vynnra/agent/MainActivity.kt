package lol.vynnra.agent

import android.Manifest
import android.content.ComponentName
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
import lol.vynnra.agent.core.provider.OpenAiCompatibleProvider
import lol.vynnra.agent.core.provider.ProviderConfig
import lol.vynnra.agent.core.voice.VoiceAgentUiState
import lol.vynnra.agent.platform.AgentRuntime
import lol.vynnra.agent.platform.AudioRecorderController
import lol.vynnra.agent.platform.AudioRecorderState
import lol.vynnra.agent.platform.AudioRecording
import lol.vynnra.agent.platform.ProviderCredentialStore
import lol.vynnra.agent.platform.ScreenCaptureConsentController
import lol.vynnra.agent.platform.ScreenCaptureController
import lol.vynnra.agent.platform.VoiceController
import lol.vynnra.agent.platform.VoiceStatus
import lol.vynnra.agent.ui.VynnraAgentShell
import lol.vynnra.agent.ui.theme.VynnraTheme

class MainActivity : ComponentActivity() {
    private lateinit var screenCaptureConsentController: ScreenCaptureConsentController
    private lateinit var screenCaptureController: ScreenCaptureController
    private lateinit var voiceController: VoiceController
    private lateinit var audioRecorderController: AudioRecorderController
    private lateinit var providerCredentialStore: ProviderCredentialStore
    private lateinit var aiProvider: OpenAiCompatibleProvider
    private lateinit var agentRuntime: AgentRuntime

    private var screenCaptureGranted by mutableStateOf(false)
    private var microphoneGranted by mutableStateOf(false)
    private var accessibilityGranted by mutableStateOf(false)
    private var fullStorageGranted by mutableStateOf(false)
    private var overlayGranted by mutableStateOf(false)
    private var notificationGranted by mutableStateOf(true)
    private var providerConfig by mutableStateOf(ProviderConfig("", "", ""))
    private var availableModels by mutableStateOf(emptyList<String>())
    private var providerStatus by mutableStateOf("Provider not connected")
    private var providerBusy by mutableStateOf(false)
    private var audioRecorderState by mutableStateOf(AudioRecorderState())
    private var recordings by mutableStateOf(emptyList<AudioRecording>())
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

    private val notificationLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            notificationGranted = granted
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        screenCaptureConsentController = ScreenCaptureConsentController(this)
        screenCaptureController = ScreenCaptureController(this)
        voiceController = VoiceController(this)
        audioRecorderController = AudioRecorderController(this)
        providerCredentialStore = ProviderCredentialStore(this)
        aiProvider = OpenAiCompatibleProvider { providerCredentialStore.load() }
        providerConfig = providerCredentialStore.load()
        refreshPermissions()
        recordings = audioRecorderController.listRecordings()

        screenCaptureGranted = screenCaptureConsentController.isGranted()
        val app = application as VynnraApplication
        agentRuntime = AgentRuntime(
            context = this,
            provider = aiProvider,
            modelProvider = { providerCredentialStore.load().model },
            memoryRepository = app.memoryRepository,
            taskRepository = app.taskRepository
        )

        lifecycleScope.launch {
            audioRecorderController.state.collect { audioRecorderState = it }
        }

        if (providerConfig.hasEndpoint()) {
            discoverProviderModels(autoSelect = providerConfig.model.isBlank())
        }

        setContent {
            VynnraTheme {
                val voiceState by voiceController.state.collectAsStateWithLifecycle()
                VynnraAgentShell(
                    memoryRepository = app.memoryRepository,
                    taskRepository = app.taskRepository,
                    screenCaptureGranted = screenCaptureGranted,
                    accessibilityGranted = accessibilityGranted,
                    fullStorageGranted = fullStorageGranted,
                    overlayGranted = overlayGranted,
                    microphoneGranted = microphoneGranted,
                    notificationGranted = notificationGranted,
                    onRequestScreenCapture = {
                        screenCaptureLauncher.launch(
                            screenCaptureConsentController.createConsentIntent()
                        )
                    },
                    onOpenAccessibilitySettings = {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    onOpenStorageSettings = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                    Uri.parse("package:$packageName")
                                )
                            )
                        }
                    },
                    onOpenOverlaySettings = {
                        startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName")
                            )
                        )
                    },
                    onRequestNotifications = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    voiceState = voiceState,
                    voiceAgentState = voiceAgentState,
                    recordingState = audioRecorderState,
                    recordings = recordings,
                    providerConfig = providerConfig,
                    availableModels = availableModels,
                    providerStatus = providerStatus,
                    providerBusy = providerBusy,
                    onRequestMicrophone = {
                        microphoneLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onStartVoice = { voiceController.startListening() },
                    onStopVoice = { voiceController.stopListening() },
                    onStopSpeaking = { voiceController.stopSpeaking() },
                    onStartRecording = {
                        if (microphoneGranted) audioRecorderController.start()
                    },
                    onStopRecording = {
                        audioRecorderController.stop()
                        recordings = audioRecorderController.listRecordings()
                    },
                    onPlayRecording = { recording -> audioRecorderController.play(recording) },
                    onStopPlayback = { audioRecorderController.stopPlayback() },
                    onDeleteRecording = { recording ->
                        if (audioRecorderController.delete(recording)) {
                            recordings = audioRecorderController.listRecordings()
                        }
                    },
                    onRefreshPermissions = { refreshPermissions() },
                    onRefreshModels = { discoverProviderModels(autoSelect = true) },
                    onTestProvider = { testProviderConnection() },
                    onStopAgent = {
                        agentRuntime.orchestrator.requestStop()
                        agentJob?.cancel()
                    },
                    onSendMessage = { text, thinkingLevel, speakResponse ->
                        submitAgentRequest(text, thinkingLevel, speakResponse)
                    },
                    onSaveProviderConfig = { config ->
                        providerCredentialStore.save(config)
                        providerConfig = providerCredentialStore.load()
                        discoverProviderModels(autoSelect = providerConfig.model.isBlank())
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissions()
    }

    private fun refreshPermissions() {
        screenCaptureGranted = screenCaptureConsentController.currentGrant() != null
        microphoneGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        accessibilityGranted = isAccessibilityEnabled()
        fullStorageGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
        overlayGranted = Settings.canDrawOverlays(this)
        notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        recordings = audioRecorderController.listRecordings()
    }

    private fun isAccessibilityEnabled(): Boolean {
        val component = ComponentName(this, lol.vynnra.agent.platform.VynnraAccessibilityService::class.java)
            .flattenToString()
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
        return enabled.split(':').any { it.equals(component, ignoreCase = true) }
    }

    private fun discoverProviderModels(autoSelect: Boolean) {
        val config = providerCredentialStore.load()
        if (!config.hasEndpoint()) {
            providerStatus = "Enter a provider URL"
            availableModels = emptyList()
            return
        }

        lifecycleScope.launch {
            providerBusy = true
            providerStatus = "Fetching models…"
            val result = runCatching { aiProvider.testConnection(config) }
            providerBusy = false
            result.onSuccess { probe ->
                availableModels = probe.models
                providerStatus = probe.message
                if (autoSelect && config.model.isBlank() && probe.models.isNotEmpty()) {
                    val updated = config.copy(model = probe.models.first())
                    providerCredentialStore.save(updated)
                    providerConfig = updated
                } else {
                    providerConfig = providerCredentialStore.load()
                }
            }.onFailure {
                providerStatus = it.message ?: "Provider discovery failed"
            }
        }
    }

    private fun testProviderConnection() {
        discoverProviderModels(autoSelect = false)
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
        audioRecorderController.shutdown()
        voiceController.shutdown()
        super.onDestroy()
    }
}
