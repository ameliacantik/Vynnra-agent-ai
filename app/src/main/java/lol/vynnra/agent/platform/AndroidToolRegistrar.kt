package lol.vynnra.agent.platform

import lol.vynnra.agent.core.orchestrator.ToolRegistry

/** Registers the Android tools in the central Vynnra tool registry. */
object AndroidToolRegistrar {
    fun registerAll(registry: ToolRegistry, controller: AndroidController) {
        registry.register(ToolRegistry.adapter(AndroidBackTool(controller)) { Unit })
        registry.register(ToolRegistry.adapter(AndroidHomeTool(controller)) { Unit })
        registry.register(ToolRegistry.adapter(AndroidRecentsTool(controller)) { Unit })
        registry.register(ToolRegistry.adapter(AndroidNotificationsTool(controller)) { Unit })
        registry.register(ToolRegistry.adapter(AndroidLaunchAppTool(controller)) { input ->
            LaunchAppInput(packageName = input.requiredString("packageName"))
        })
        registry.register(ToolRegistry.adapter(AndroidTapTool(controller)) { input ->
            TapInput(
                x = input.requiredFloat("x"),
                y = input.requiredFloat("y"),
                expectedText = input.optionalString("expectedText")
            )
        })
        registry.register(ToolRegistry.adapter(AndroidSwipeTool(controller)) { input ->
            SwipeInput(
                startX = input.requiredFloat("startX"),
                startY = input.requiredFloat("startY"),
                endX = input.requiredFloat("endX"),
                endY = input.requiredFloat("endY"),
                durationMs = input.optionalLong("durationMs") ?: 400L,
                expectedText = input.optionalString("expectedText")
            )
        })
        registry.register(ToolRegistry.adapter(AndroidTypeTextTool(controller)) { input ->
            TypeTextInput(
                text = input.requiredString("text"),
                expectedText = input.optionalString("expectedText")
            )
        })
        registry.register(ToolRegistry.adapter(AndroidClickTextTool(controller)) { input ->
            ClickTextInput(
                text = input.requiredString("text"),
                expectedText = input.optionalString("expectedText")
            )
        })
        registry.register(ToolRegistry.adapter(AndroidFindTextTool(controller)) { input ->
            FindTextInput(
                text = input.requiredString("text"),
                ignoreCase = input["ignoreCase"] as? Boolean ?: true
            )
        })
        registry.register(ToolRegistry.adapter(AndroidInspectScreenTool(controller)) { input ->
            InspectScreenInput(
                maxNodes = (input["maxNodes"] as? Number)?.toInt()?.coerceIn(1, 500) ?: 300
            )
        })
    }

    private fun Map<String, Any?>.requiredString(key: String): String =
        (this[key] as? String)?.takeIf { it.isNotBlank() }
            ?: error("Missing or blank input: $key")

    private fun Map<String, Any?>.optionalString(key: String): String? =
        this[key] as? String

    private fun Map<String, Any?>.requiredFloat(key: String): Float = when (val value = this[key]) {
        is Number -> value.toFloat()
        is String -> value.toFloatOrNull() ?: error("Invalid float input: $key")
        else -> error("Missing numeric input: $key")
    }

    private fun Map<String, Any?>.optionalLong(key: String): Long? = when (val value = this[key]) {
        is Number -> value.toLong()
        is String -> value.toLongOrNull()
        else -> null
    }
}
