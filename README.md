# Vynnra Agent AI

A serious, modular Android AI agent project focused on personal assistance, device interaction, browser automation, files, vision, web research, memory, and task execution.

## Project principles

- Build from scratch; this repository is independent of previous prototypes.
- Observe before acting and verify important actions after execution.
- Never expose private chain-of-thought; the UI shows only high-level activity states.
- Respect Android permissions, user consent, security boundaries, and platform limitations.
- Keep secrets such as AI/Tavily API keys out of the APK and source repository.

## Architecture

`OBSERVE → UNDERSTAND → PLAN → ACT → VERIFY → RECOVER → RESPOND`

## Roadmap

- Phase 0 — Architecture specification
- Phase 1 — Android foundation, UI, state, data, provider/tool contracts
- Phase 2 — Agent orchestrator
- Phase 3 — Accessibility and screen control
- Phase 4 — Vision engine
- Phase 5 — Browser and file agent
- Phase 6 — Web research
- Phase 7 — Memory and task persistence
- Phase 8 — Voice
- Phase 9 — Background agent
- Phase 10 — Power-user/device administration capabilities

See `docs/ARCHITECTURE.md` for the current design.