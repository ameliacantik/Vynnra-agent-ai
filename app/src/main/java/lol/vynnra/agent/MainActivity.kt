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
import lol.vynnra.agent.platform.ScreenCaptureConsentController
import lol.vynnra.agent.platform.ScreenCaptureController
import lol.vynnra.agent.platform.VoiceController
import lol.vynnra.agent.ui.VynnraAgentShell
import lol.vynnra.agent.ui.theme.VynnraTheme

class MainActivity : ComponentActivity() {
    private lateinit var screenCaptureConsentController: ScreenCaptureConsentController
    private lateinit var screenCaptureController: ScreenCaptureController
    private lateinit var voiceController: VoiceController

    private var screenCaptureGranted by mutableStateOf(false)
    private var microphoneGranted by mutableStateOf(false)

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
        screenCaptureGranted = screenCaptureConsentController.isGranted()
        microphoneGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val app = application as VynnraApplication
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
                    onRequestMicrophone = {
                        microphoneLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onStartVoice = { voiceController.startListening() },
                    onStopVoice = { voiceController.stopListening() },
                    onStopSpeaking = { voiceController.stopSpeaking() }
                )
            }
        }
    }

    override fun onDestroy() {
        voiceController.shutdown()
        super.onDestroy()
    }
}
