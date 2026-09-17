package lol.vynnra.agent.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceModelsTest {
    @Test
    fun listening_and_transcript_states_are_bounded() {
        val started = VoiceSessionStateMachine.startListening(VoiceUiState())
        assertEquals(VoiceStatus.LISTENING, started.status)
        assertEquals("", started.partialText)

        val partial = VoiceSessionStateMachine.partialResult(started, "buka")
        assertEquals("buka", partial.partialText)
        assertEquals(VoiceStatus.LISTENING, partial.status)

        val final = VoiceSessionStateMachine.finalResult(partial, "Buka Chrome")
        assertEquals("Buka Chrome", final.finalText)
        assertEquals("", final.partialText)
        assertEquals(VoiceStatus.IDLE, final.status)
    }

    @Test
    fun speaking_state_has_explicit_stop_transition() {
        val speaking = VoiceSessionStateMachine.startSpeaking(VoiceUiState(), "Halo")
        assertEquals(VoiceStatus.SPEAKING, speaking.status)
        assertEquals("Halo", speaking.speechOutput)

        val finished = VoiceSessionStateMachine.finishSpeaking(speaking)
        assertEquals(VoiceStatus.IDLE, finished.status)
        assertTrue(finished.speechOutput == null)
    }
}
