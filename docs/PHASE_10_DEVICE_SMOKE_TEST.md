# Phase 10 — Device Smoke Test

This checklist is the final manual gate for real Android voice behavior. CI can verify compilation, JVM behavior, APK generation, and emulator restart persistence, but microphone capture and OEM speech services still require a physical Android device.

## Preconditions

- Install the latest `vynnra-agent-debug-apk` from the successful GitHub Actions run.
- Open Vynnra Agent.
- In Settings → AI Provider, configure a valid OpenAI-compatible base URL, API key, and model.
- Grant Microphone permission.

## Smoke test

- [ ] Microphone permission opens and returns to Vynnra.
- [ ] Tapping the microphone enters `LISTENING`.
- [ ] Partial speech text appears while speaking.
- [ ] Final transcript is submitted to the live AI provider.
- [ ] The assistant response appears in chat.
- [ ] The same assistant response is spoken with TTS.
- [ ] Tapping the voice control while speaking stops TTS.
- [ ] The voice request creates a persistent task record.
- [ ] The task record reaches `COMPLETED` and its step reaches `VERIFIED` after a successful provider response.
- [ ] Provider API credentials remain masked in the UI and are not printed into logs.
- [ ] A provider/network failure surfaces as a user-visible error without crashing the app.

## Result

This checklist must be completed on at least one physical Android device before the final `Real-device microphone/STT/TTS verification` item in `docs/PHASE_10_VOICE.md` can be changed to `[x]`.
