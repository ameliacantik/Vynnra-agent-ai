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
- Phase 1 — Foundation
- Phase 2 — Vynnra UI/UX
- Phase 3 — Agent Orchestrator
- Phase 4 — Permission Center
- Phase 5 — Android Control
- Phase 6 — Vision
- Phase 7 — Browser + Files
- Phase 8 — Tavily Web Search
- Phase 9 — Memory + Tasks
- Phase 10 — Voice
- Phase 11 — Background Agent
- Phase 12 — Power User / Device Owner / ADB

`Vynnra Agent 1.0` is the milestone after Phase 12.

See `docs/ARCHITECTURE.md` and `docs/ROADMAP.md` for the detailed design.