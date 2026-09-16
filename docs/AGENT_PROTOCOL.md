# Agent Protocol

## AgentRun

An agent run has:

- `runId`
- `userRequest`
- `thinkingLevel`
- `status`
- `startedAt`
- `updatedAt`
- `currentActivity`
- `toolCalls`
- `result`
- `error`

## Status

```text
IDLE
UNDERSTANDING
PLANNING
EXECUTING
VERIFYING
RECOVERING
COMPLETED
FAILED
CANCELLED
BLOCKED
```

## Thinking levels

```text
LOW
MEDIUM
HIGH
MAX
```

Thinking level controls planning/reasoning effort exposed to the application, not disclosure of private chain-of-thought.

## Tool execution

Before a tool executes:

1. Validate arguments.
2. Check required capability.
3. Check confirmation requirements.
4. Record an action-journal entry.
5. Execute with cancellation support.
6. Normalize the result.
7. Verify when required.
8. Feed the result back to the orchestrator.

## Recovery

Temporary failures should use bounded retries and alternative strategies. Recovery must not silently bypass capability or confirmation gates.
