# Phase 11 — Background Agent

## Implementation checklist

- [~] Foreground/background task execution foundation
- [ ] Notifications
- [ ] Scheduled workflows
- [ ] Recovery after interruption
- [ ] Persistent task runner

## Contract

Phase 11 extends the persistent task model from Phase 9 into Android-managed execution. Background work must remain capability-aware, cancellable, observable, and resumable. Long-running execution uses Android-supported foreground execution when required; scheduled work uses WorkManager where appropriate.

The background runner must never bypass confirmation or capability gates. It records task/checkpoint state before and after execution and reports interruption/recovery explicitly.

## Verification boundary

CI must compile the Android project and pass JVM coverage for scheduling/state transitions. Real-device verification is required for notification behavior, foreground-service restrictions, Doze/background limits, reboot/interruption recovery, and OEM-specific behavior.
