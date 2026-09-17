# Phase 10 — Voice

## Implementation checklist

- [~] Speech-to-text controller using Android `SpeechRecognizer`
- [~] Text-to-speech controller using Android `TextToSpeech`
- [~] Voice state model with high-level listening/speaking/error states
- [~] Microphone permission flow in the Permission Center
- [~] Voice input control in the main composer
- [ ] Voice agent mode connected to the real AI provider/orchestrator response loop
- [ ] Spoken assistant responses from live agent output
- [ ] Voice-triggered persistent tasks
- [ ] Real-device microphone/STT/TTS verification

## Voice contract

Voice is a transport layer for the agent, not a separate reasoning system. Speech input becomes normal Vynnra text input. Agent output can later be spoken through the same controller. The voice UI exposes only high-level activity states and never private chain-of-thought.

The controller keeps recognition and synthesis state bounded and cancellable. Recognition supports partial and final transcripts, while text-to-speech supports explicit stop/cancel. Android runtime permission remains user-controlled.

## Integration boundary

The current Phase 10 implementation establishes the Android voice transport and UI foundation. The main chat still uses the existing Phase 2/3 shell rather than a production AI provider loop, so live spoken responses and voice-triggered task execution remain intentionally deferred until the provider/orchestrator chat path is connected.

## Verification boundary

CI must compile the Android project and run JVM tests for the voice state machine. Real-device testing remains required for microphone permissions, recognition availability, OEM speech services, language packs, TTS voices, and end-to-end voice commands.
