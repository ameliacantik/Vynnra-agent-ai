package lol.vynnra.agent.platform

import lol.vynnra.agent.core.tool.Capability
import lol.vynnra.agent.core.tool.RiskLevel
import lol.vynnra.agent.core.tool.ToolResult
import lol.vynnra.agent.core.tool.ToolResultStatus
import lol.vynnra.agent.core.tool.VynnraTool

private val ACCESSIBILITY_AND_INTERACTION = setOf(
    Capability.ACCESSIBILITY_CONTROL,
    Capability.SCREEN_INTERACT
)

private fun AndroidControlResult.toToolResult(): ToolResult = ToolResult(
    status = if (success) ToolResultStatus.SUCCESS else ToolResultStatus.FAILED,
    message = message,
    data = details
)

class AndroidBackTool(private val controller: AndroidController) : VynnraTool<Unit> {
    override val definition = toolDefinition("android.back", "Android Back", "Press the Android Back action.")
    override suspend fun execute(input: Unit): ToolResult = controller.back().toToolResult()
}

class AndroidHomeTool(private val controller: AndroidController) : VynnraTool<Unit> {
    override val definition = toolDefinition("android.home", "Android Home", "Go to the Android Home screen.")
    override suspend fun execute(input: Unit): ToolResult = controller.home().toToolResult()
}

class AndroidRecentsTool(private val controller: AndroidController) : VynnraTool<Unit> {
    override val definition = toolDefinition("android.recents", "Android Recents", "Open the Android recent-apps screen.")
    override suspend fun execute(input: Unit): ToolResult = controller.recents().toToolResult()
}

class AndroidNotificationsTool(private val controller: AndroidController) : VynnraTool<Unit> {
    override val definition = toolDefinition("android.notifications", "Android Notifications", "Open the Android notification shade.")
    override suspend fun execute(input: Unit): ToolResult = controller.notifications().toToolResult()
}

data class LaunchAppInput(val packageName: String)

class AndroidLaunchAppTool(private val controller: AndroidController) : VynnraTool<LaunchAppInput> {
    override val definition = toolDefinition(
        id = "android.launch_app",
        name = "Launch Android App",
        description = "Launch an installed Android app by package name.",
        riskLevel = RiskLevel.MEDIUM,
        supportsVerification = true
    )
    override suspend fun execute(input: LaunchAppInput): ToolResult = controller.launchPackage(input.packageName).toToolResult()
}

data class TapInput(val x: Float, val y: Float, val expectedText: String? = null)

class AndroidTapTool(private val controller: AndroidController) : VynnraTool<TapInput> {
    override val definition = toolDefinition(
        id = "android.tap",
        name = "Tap Screen",
        description = "Tap a screen coordinate using the user-enabled Accessibility service.",
        riskLevel = RiskLevel.MEDIUM,
        supportsVerification = true
    )
    override suspend fun execute(input: TapInput): ToolResult = controller.tap(input.x, input.y).toToolResult().withExpectedText(input.expectedText)
}

data class SwipeInput(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val durationMs: Long = 400L,
    val expectedText: String? = null
)

class AndroidSwipeTool(private val controller: AndroidController) : VynnraTool<SwipeInput> {
    override val definition = toolDefinition(
        id = "android.swipe",
        name = "Swipe Screen",
        description = "Swipe between screen coordinates using Accessibility gestures.",
        riskLevel = RiskLevel.MEDIUM,
        supportsVerification = true
    )
    override suspend fun execute(input: SwipeInput): ToolResult = controller.swipe(
        input.startX, input.startY, input.endX, input.endY, input.durationMs
    ).toToolResult().withExpectedText(input.expectedText)
}

data class TypeTextInput(val text: String, val expectedText: String? = null)

class AndroidTypeTextTool(private val controller: AndroidController) : VynnraTool<TypeTextInput> {
    override val definition = toolDefinition(
        id = "android.type_text",
        name = "Type Text",
        description = "Enter text into the focused or editable Accessibility node.",
        requiredCapabilities = ACCESSIBILITY_AND_INTERACTION,
        riskLevel = RiskLevel.MEDIUM,
        supportsVerification = true
    )
    override suspend fun execute(input: TypeTextInput): ToolResult =
        controller.typeText(input.text).toToolResult().withExpectedText(input.expectedText ?: input.text)
}

data class ClickTextInput(val text: String, val expectedText: String? = null)

class AndroidClickTextTool(private val controller: AndroidController) : VynnraTool<ClickTextInput> {
    override val definition = toolDefinition(
        id = "android.click_text",
        name = "Click Text",
        description = "Find visible text in the Accessibility tree and click its nearest clickable parent.",
        requiredCapabilities = ACCESSIBILITY_AND_INTERACTION,
        riskLevel = RiskLevel.MEDIUM,
        supportsVerification = true
    )
    override suspend fun execute(input: ClickTextInput): ToolResult =
        controller.clickText(input.text).toToolResult().withExpectedText(input.expectedText)
}

data class FindTextInput(val text: String, val ignoreCase: Boolean = true)

class AndroidFindTextTool(private val controller: AndroidController) : VynnraTool<FindTextInput> {
    override val definition = toolDefinition(
        id = "android.find_text",
        name = "Find Text",
        description = "Inspect the active Accessibility tree for visible matching text.",
        requiredCapabilities = setOf(Capability.ACCESSIBILITY_CONTROL),
        riskLevel = RiskLevel.LOW,
        supportsVerification = true
    )
    override suspend fun execute(input: FindTextInput): ToolResult {
        val matches = controller.findText(input.text)
        return ToolResult(
            status = if (matches.isNotEmpty()) ToolResultStatus.SUCCESS else ToolResultStatus.FAILED,
            message = if (matches.isNotEmpty()) "Found ${matches.size} matching element(s)" else "Text not found: ${input.text}",
            data = mapOf(
                "query" to input.text,
                "matches" to matches.map {
                    mapOf("text" to it.text, "className" to it.className, "clickable" to it.clickable, "editable" to it.editable)
                }
            )
        )
    }
}

private fun toolDefinition(
    id: String,
    name: String,
    description: String,
    requiredCapabilities: Set<Capability> = setOf(Capability.ACCESSIBILITY_CONTROL),
    riskLevel: RiskLevel,
    supportsVerification: Boolean = false
) = lol.vynnra.agent.core.tool.ToolDefinition(
    id = id,
    name = name,
    description = description,
    requiredCapabilities = requiredCapabilities,
    riskLevel = riskLevel,
    supportsVerification = supportsVerification
)

private fun ToolResult.withExpectedText(expectedText: String?): ToolResult =
    if (expectedText.isNullOrBlank()) this else copy(data = data + ("expectedText" to expectedText))
