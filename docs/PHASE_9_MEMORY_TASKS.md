# Phase 9 — Memory + Tasks

## Implementation checklist

- [x] Room schema v2 for memory and persistent tasks
- [x] Room migration from schema v1 to v2
- [x] Conversation memory: bounded recent-message retrieval
- [x] Long-term memory records with kind, importance, pinning, enabled state, and sensitivity metadata
- [x] Memory relevance scoring and bounded retrieval
- [x] User-controlled memory management: enable, disable, pin, unpin, forget
- [x] Persistent task records and task steps
- [x] Task lifecycle: pending, running, paused, blocked, failed, completed, cancelled
- [x] Task checkpoints with resumable state
- [x] Resumable-task discovery after process/app restart
- [x] Application-scoped database/repository wiring
- [x] Unit coverage for memory relevance and task checkpoint/resume behavior
- [x] Chat UI memory controls
- [x] Task UI and task detail timeline
- [x] Full agent-orchestrator integration for automatic task checkpointing
- [~] End-to-end device restart verification

## Memory contract

Vynnra separates recent conversation context from long-term memory. Recent chat messages are bounded per request. Long-term memories are explicit records that can be enabled/disabled, pinned, or permanently forgotten by the user. Sensitive metadata is stored with the record so future policy layers can restrict how it is surfaced.

Memory retrieval is intentionally bounded. A SQL candidate query is followed by a deterministic relevance score using token overlap, importance, freshness, and pinning. The planning layer injects only a bounded set of relevant enabled memories. This phase does not persist or expose hidden chain-of-thought.

## Task contract

Tasks are durable state machines. Each task stores its goal, lifecycle status, current step, checkpoint payload, error state, and optional user-action requirement. Individual steps store their status, tool identifier, input/output summaries, attempt count, and timestamps.

The orchestrator creates and checkpoints tasks around tool execution, records verified step completion, persists blocked/cancelled/failed states, and exposes resumable-task discovery. Resume starts from the persisted current step rather than replaying already verified steps. Automatic background execution remains deferred to Phase 11.

## UI contract

The Memory & Tasks center is exposed from the main Vynnra sidebar. Memory controls cover add, enable/disable, pin/unpin, and forget. Task controls expose persisted task status, progress, checkpoint payload, and step-level execution history.

## Verification boundary

The Android build and JVM unit-test stages pass on the latest Phase 9 CI run. The Android emulator restart stage did not complete within the hosted runner timeout because the emulator/ADB environment was slow to become usable; no application assertion failure was reported. Therefore the restart verification remains `[~]` until it passes on a reliable emulator/device environment.
