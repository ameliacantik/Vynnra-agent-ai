# Phase 6 — Vision

## Implementation checklist

- [x] User-consented MediaProjection screenshot capture
- [x] Android 14+ compatible mediaProjection foreground service declaration
- [x] Foreground notification with explicit Stop action
- [x] In-memory latest-frame store; projection frames are not persisted
- [x] Frame throttling and bounded output width
- [x] On-device OCR visual understanding with ML Kit
- [x] Accessibility + visual detection fusion
- [x] Confidence-ranked target candidates
- [x] Duplicate target suppression
- [x] Target visibility verification
- [x] Post-action target/state-change verification primitive
- [x] Safe no-op visual engine remains available when OCR/model execution is unavailable

## Runtime contract

1. The user grants screen-capture consent through Android's system dialog.
2. Vynnra starts `VynnraScreenCaptureService` as a mediaProjection foreground service.
3. The service owns one active `MediaProjection` session and publishes the newest bounded frame to `ScreenCaptureStore`.
4. Vision analysis converts the frame into bounded detections. OCR is the first real on-device detector; additional vision models can implement `VisualUnderstandingEngine` later without changing the orchestrator contract.
5. `FusedTargetResolver` combines Accessibility tree targets and visual detections.
6. `TargetVerifier` can validate that a target remains visible and that an expected target changed after an action.

## Verification boundary

GitHub Actions verifies compilation and APK generation. Real-device testing is still required to validate the Android consent dialog, OEM-specific MediaProjection behavior, OCR model availability/download, and gesture/target behavior on an actual device.

The Phase 6 implementation therefore does not claim universal visual recognition: it provides real screenshot capture, real OCR text understanding, accessibility/vision fusion, and verification primitives, with a provider interface for richer vision models.
