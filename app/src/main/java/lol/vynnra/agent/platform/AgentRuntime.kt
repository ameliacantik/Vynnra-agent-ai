package lol.vynnra.agent.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import lol.vynnra.agent.core.orchestrator.AgentChatSession
import lol.vynnra.agent.core.orchestrator.AgentOrchestrator
import lol.vynnra.agent.core.orchestrator.BoundedRecoveryEngine
import lol.vynnra.agent.core.orchestrator.ProviderBackedPlanner
import lol.vynnra.agent.core.orchestrator.ToolRegistry
import lol.vynnra.agent.core.security.CapabilityGate
import lol.vynnra.agent.data.memory.MemoryRepository
import lol.vynnra.agent.data.task.TaskRepository
import lol.vynnra.agent.core.tool.Capability
import lol.vynnra.agent.core.provider.AiProvider
import lol.vynnra.agent.web.WebSearchToolRegistrar

/**
 * Single production wiring point for the agent runtime.
 * Capability detection is evaluated at execution/planning time so newly granted
 * Android permissions become available without rebuilding the runtime.
 */
class AgentRuntime(
    context: Context,
    provider: AiProvider,
    modelProvider: () -> String,
    webBaseUrlProvider: () -> String = { "" },
    webAuthTokenProvider: () -> String? = { null },
    memoryRepository: MemoryRepository? = null,
    taskRepository: TaskRepository? = null
) {
    private val appContext = context.applicationContext

    val registry = ToolRegistry()

    private val capabilityProvider: () -> Set<Capability> = {
        mutableSetOf<Capability>().apply {
            add(Capability.FILE_READ)
            add(Capability.FILE_WRITE)
            add(Capability.BROWSER_CONTROL)

            val accessibility = VynnraAccessibilityService.current() != null
            if (accessibility) {
                add(Capability.ACCESSIBILITY_CONTROL)
                add(Capability.SCREEN_READ)
                add(Capability.SCREEN_INTERACT)
            }

            if (ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                add(Capability.MICROPHONE)
            }

            if (Settings.canDrawOverlays(appContext)) {
                add(Capability.OVERLAY)
            }

            if (webBaseUrlProvider().isNotBlank()) {
                add(Capability.WEB_SEARCH)
            }
        }
    }

    private val planner = ProviderBackedPlanner(
        provider = provider,
        modelProvider = modelProvider,
        toolDefinitionsProvider = { registry.definitions() },
        availableCapabilitiesProvider = capabilityProvider
    )

    private val capabilityGate = CapabilityGate(capabilityProvider)

    val orchestrator = AgentOrchestrator(
        planner = planner,
        registry = registry,
        capabilityGate = capabilityGate,
        verifier = AndroidVerificationEngine(AndroidController(appContext)),
        recovery = BoundedRecoveryEngine(),
        memoryRepository = memoryRepository,
        taskRepository = taskRepository
    )

    val chatSession = AgentChatSession(
        orchestrator = orchestrator,
        provider = provider,
        modelProvider = modelProvider
    )

    init {
        AndroidToolRegistrar.registerAll(registry, AndroidController(appContext))
        Phase7ToolRegistrar.registerAll(registry, appContext)
        WebSearchToolRegistrar.registerAll(
            registry = registry,
            baseUrlProvider = webBaseUrlProvider,
            authTokenProvider = webAuthTokenProvider
        )
    }

    fun capabilities(): Set<Capability> = capabilityProvider()
}
