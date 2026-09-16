package lol.vynnra.agent.core.orchestrator

import java.util.UUID

class ActionJournal {
    private val entries = mutableListOf<ActionJournalEntry>()

    @Synchronized
    fun record(
        runId: String,
        action: AgentAction,
        status: String,
        message: String? = null
    ): ActionJournalEntry {
        val entry = ActionJournalEntry(
            id = UUID.randomUUID().toString(),
            runId = runId,
            actionId = action.id,
            toolId = action.toolId,
            status = status,
            message = message,
            timestamp = System.currentTimeMillis()
        )
        entries += entry
        return entry
    }

    @Synchronized
    fun forRun(runId: String): List<ActionJournalEntry> = entries.filter { it.runId == runId }.toList()

    @Synchronized
    fun clear() = entries.clear()
}

data class ActionJournalEntry(
    val id: String,
    val runId: String,
    val actionId: String,
    val toolId: String,
    val status: String,
    val message: String?,
    val timestamp: Long
)
