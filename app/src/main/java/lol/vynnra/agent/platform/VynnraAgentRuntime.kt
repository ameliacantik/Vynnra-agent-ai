package lol.vynnra.agent.platform

import android.content.Context
import android.os.Build
import android.provider.Settings
import lol.vynnra.agent.core.orchestrator.ActionJournal
import lol.vynnra.agent.core.orchestrator.AgentOrchestrator
import lol.vynnra.agent.core.orchestrator.AiAgentPlanner
import lol.vynnra.agent.core.orchestrator.BoundedRecoveryEngine
import lol.vynnra.agent.core.orchestrator.DefaultVerificationEngine
import lol.vynnra.agent.core.orchestrator.ToolRegistry
import lol.vynnra.agent.core.security.CapabilityGate
import lol.vynnra.agent.core.provider.AiProvider
import lol.vynnra.agent.core.tool.Capability
import lol.vynnra.agent.data.memory.MemoryRepository
import lol.vynnra.agent.data.task.TaskRepository

class VynnraAgentRuntime(
    context: Context,
    provider: AiProvider,
    memoryRepository: MemoryRepository,
    taskRepository: TaskRepository,
    private val modelProvider: () -> String,
    private val microphoneGranted: () -> Boolean,
    private val webSearchBaseUrlProvider: () -> String = { "" }
) {
    private val appContext = context.applicationContext
    private val controller = AndroidController(appContext)

    val registry = ToolRegistry()

    val orchestrator: AgentOrchestrator

    init {
        AndroidToolRegistrar.registerAll(registry, controller)
        Phase7ToolRegistrar.registerAll(registry, appContext)
        lol.vynnra.agent.web.WebSearchToolRegistrar.registerAll(
            registry = registry,
            baseUrlProvider = webSearchBaseUrlProvider
        )
        registry.register(AiAnswerTool(provider, modelProvider))

        orchestrator = AgentOrchestrator(
            planner = AiAgentPlanner(
                provider = provider,
                modelProvider = modelProvider,
                toolDefinitionsProvider = { registry.definitions() }
            ),
            registry = registry,
            capabilityGate = CapabilityGate(::currentCapabilities),
            verifier = DefaultVerificationEngine(),
            recovery = BoundedRecoveryEngine(),
            journal = ActionJournal(),
            memoryRepository = memoryRepository,
            taskRepository = taskRepository
        )
    }

    fun currentCapabilities(): Set<Capability> = buildSet {
        add(Capability.FILE_READ)
        add(Capability.FILE_WRITE)
        if (microphoneGranted()) add(Capability.MICROPHONE)

        if (VynnraAccessibilityService.current() != null) {
            add(Capability.ACCESSIBILITY_CONTROL)
            add(Capability.SCREEN_READ)
            add(Capability.SCREEN_INTERACT)
            add(Capability.BROWSER_CONTROL)
        }
        if (ScreenCaptureStore.isActive()) add(Capability.SCREEN_READ)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(appContext)) {
            add(Capability.OVERLAY)
        }
        if (webSearchBaseUrlProvider().trim().isNotEmpty()) add(Capability.WEB_SEARCH)
    }
}