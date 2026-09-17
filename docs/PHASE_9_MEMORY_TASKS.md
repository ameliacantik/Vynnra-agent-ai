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
- [ ] Chat UI memory controls
- [ ] Task UI and task detail timeline
- [ ] Full agent-orchestrator integration for automatic task checkpointing
- [ ] End-to-end device restart verification

## Memory contract

Vynnra separates recent conversation context from long-term memory. Recent chat messages are bounded per request. Long-term memories are explicit records that can be enabled/disabled, pinned, or permanently forgotten by the user. Sensitive metadata is stored with the record so future policy layers can restrict how it is surfaced.

Memory retrieval is intentionally bounded. A SQL candidate query is followed by a deterministic relevance score using token overlap, importance, freshness, and pinning. This phase does not persist or expose hidden chain-of-thought.

## Task contract

Tasks are durable state machines. Each task stores its goal, lifecycle status, current step, checkpoint payload, error state, and optional user-action requirement. Individual steps store their status, tool identifier, input/output summaries, attempt count, and timestamps.

The repository exposes resumable tasks after process restart. Automatic background execution is intentionally deferred to Phase 11; Phase 9 persists enough state for a future runner to safely resume from a verified checkpoint.

## Verification boundary

CI must compile the Android project and execute the Phase 9 unit tests. Device-level restart testing and UI verification remain runtime work before every Phase 9 item can move to `[x]`.
