# Phase 4 — Permission Center

## Goal
Provide a single capability inventory that makes Vynnra's Android permissions explicit, user-controlled, and observable.

## Implemented foundation
- Capability-to-permission state model.
- Live status refresh for Accessibility, microphone, and overlay capabilities.
- Accessibility service registration with window-content and gesture capability metadata.
- Settings deep links for Accessibility and overlay flows.
- Document picker entry point for file access.
- Permission Center UI component with refresh and enable actions.
- Dangerous capabilities are marked explicitly in the model.

## Security rules
- No capability is treated as granted merely because the UI exists.
- Tool execution remains protected by the Phase 3 capability gate.
- Screen capture remains an explicit MediaProjection user-consent flow; it is not silently granted.
- Destructive/security-sensitive actions will require separate confirmation policies in the orchestrator.

## Remaining runtime work
- Wire MediaProjection consent into the activity/task layer.
- Add dedicated notification-listener service only when notification reading is implemented.
- Wire microphone runtime permission request.
- Add foreground-service/background execution policy.
- Connect Permission Center UI to a shared lifecycle-aware permission manager instance.
