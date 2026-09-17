package lol.vynnra.agent.core.memory

/** Keeps memory injection bounded and excludes disabled records by contract. */
object MemoryRepositoryBridge {
    fun withContext(goal: String, memories: List<MemoryRecord>): String {
        if (memories.isEmpty()) return goal

        val context = memories
            .filter { it.enabled }
            .take(8)
            .joinToString("\n") { memory ->
                val label = memory.key?.takeIf { it.isNotBlank() } ?: memory.kind.name.lowercase()
                "- $label: ${memory.content.take(800)}"
            }

        if (context.isBlank()) return goal
        return buildString {
            append(goal.trim())
            append("\n\nRelevant saved context (use only when applicable):\n")
            append(context)
        }
    }
}
