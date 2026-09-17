# Phase 10 — Voice

## Implementation checklist

- [x] Speech-to-text controller using Android `SpeechRecognizer`
- [x] Text-to-speech controller using Android `TextToSpeech`
- [x] Voice state model with high-level listening/speaking/error states
- [x] Microphone permission flow in the Permission Center
- [x] Voice input control in the main composer
- [x] Voice agent mode connected to the live AI/provider response loop
- [x] Spoken assistant responses from live agent output
- [x] Voice-triggered persistent tasks
- [~] Real-device microphone/STT/TTS verification

## Voice contract

Voice is a transport layer for the agent, not a separate reasoning system. Speech input becomes normal Vynnra text input. Agent output is returned to the chat and can be spoken through the same controller. The voice UI exposes only high-level activity states and never private chain-of-thought.

The controller keeps recognition and synthesis state bounded and cancellable. Recognition supports partial and final transcripts, while text-to-speech supports explicit stop/cancel. Android runtime permission remains user-controlled.

## Live AI integration

Voice transcripts now use the same configurable OpenAI-compatible provider path as the main composer. Provider configuration is stored locally with an Android Keystore-backed AES/GCM credential store. Successful voice requests are persisted as Room tasks with a running checkpoint and a verified/completed terminal state; failed provider requests are persisted as failed tasks.

The main chat also uses this provider path, so the Phase 10 voice transport is connected to live AI output rather than remaining a UI-only foundation.

## Verification boundary

GitHub Actions run `35260438777` completed successfully for the Phase 10 implementation commit, including:
- server tests
- debug APK build
- JVM unit tests, including the persistent voice-task test
- KVM setup
- Android restart instrumentation test
- APK artifact upload

The remaining `[~]` item is intentionally a physical-device gate. The repository includes `docs/PHASE_10_DEVICE_SMOKE_TEST.md` with the exact microphone, STT, TTS, provider, and persistent-task checks. CI cannot substitute for OEM microphone input, speech services, installed language packs, and physical audio output.
