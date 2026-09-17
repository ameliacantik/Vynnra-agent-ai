package lol.vynnra.agent.core.memory

import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryRelevanceScorerTest {
    @Test
    fun matching_memory_scores_above_unrelated_memory() {
        val now = 1_000_000L
        val matching = MemoryRecord(
            id = "1",
            kind = MemoryKind.PREFERENCE,
            key = "theme",
            content = "User prefers an AMOLED dark theme",
            sourceSessionId = null,
            importance = 0.8f,
            sensitive = false,
            enabled = true,
            pinned = false,
            createdAt = now,
            updatedAt = now,
            lastAccessedAt = null
        )
        val unrelated = matching.copy(
            id = "2",
            content = "User likes cooking recipes",
            importance = 0.2f
        )

        val matchingScore = MemoryRelevanceScorer.score("dark theme", matching, now)
        val unrelatedScore = MemoryRelevanceScorer.score("dark theme", unrelated, now)
        assertTrue(matchingScore > unrelatedScore)
    }
}
