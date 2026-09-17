package lol.vynnra.agent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import lol.vynnra.agent.platform.ScreenCaptureConsentController
import lol.vynnra.agent.platform.ScreenCaptureController
import lol.vynnra.agent.ui.VynnraAgentShell
import lol.vynnra.agent.ui.theme.VynnraTheme

class MainActivity : ComponentActivity() {
    private lateinit var screenCaptureConsentController: ScreenCaptureConsentController
    private lateinit var screenCaptureController: ScreenCaptureController

    private var screenCaptureGranted by mutableStateOf(false)

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        screenCaptureConsentController = ScreenCaptureConsentController(this)
        screenCaptureController = ScreenCaptureController(this)
        screenCaptureGranted = screenCaptureConsentController.isGranted()

        val app = application as VynnraApplication
        setContent {
            VynnraTheme {
                VynnraAgentShell(
                    memoryRepository = app.memoryRepository,
                    taskRepository = app.taskRepository,
                    screenCaptureGranted = screenCaptureGranted,
                    onRequestScreenCapture = {
                        screenCaptureLauncher.launch(
                            screenCaptureConsentController.createConsentIntent()
                        )
                    }
                )
            }
        }
    }
}
