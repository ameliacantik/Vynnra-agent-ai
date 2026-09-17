# Vynnra Agent AI Roadmap

Status legend: `[x]` implemented and CI/build verified, `[~]` foundation/partial implementation, `[ ]` not implemented.

## Phase 0 — Architecture specification
- [x] Define system layers
- [x] Define agent lifecycle
- [x] Define tool protocol
- [x] Define capability model
- [x] Finalize database schema foundation
- [x] Finalize API/provider contracts foundation

## Phase 1 — Foundation
- [x] Repository initialized from scratch
- [x] Architecture documentation
- [x] Agent protocol
- [x] Tool protocol
- [x] Android Gradle project
- [x] Compose application shell
- [x] AMOLED visual system foundation
- [x] Chat/session data foundation
- [x] Local persistence foundation (Room schema + repository layer)
- [x] Provider abstraction
- [x] Agent state model
- [x] Permission/capability gate foundation
- [x] CI build validation workflow
- [x] Debug APK build verified by GitHub Actions

## Phase 2 — Vynnra UI/UX
- [ ] Production chat experience
- [x] Sidebar and chat sessions UI foundation
- [x] Thinking level selector: Low / Medium / High / Max
- [x] High-level activity/status UI foundation
- [x] Permission Center UI foundation
- [x] Settings UI foundation
- [ ] Tool/action journal UI
- [ ] Vynnra branding, logo, splash, empty states refinement
- [ ] Error/reconnect UX

## Phase 3 — Agent Orchestrator
- [x] Orchestrator
- [x] Tool registry
- [x] Planner/executor loop
- [x] Observe → Understand → Plan → Act → Verify → Recover → Respond
- [x] Verification engine
- [x] Recovery engine
- [x] Action journal
- [x] Cancellation / emergency stop

## Phase 4 — Permission Center
- [x] Capability inventory
- [x] Accessibility permission flow
- [x] Screen-capture consent flow
- [x] Storage/file capability flow
- [x] Storage access levels: Level 1 / Level 2 / Level 3
- [x] Level 1 — Standard / Scoped Storage: SAF + app-owned and user-selected files/directories
- [x] Level 2 — Full Storage: broad shared-storage access through the appropriate Android all-files capability where eligible
- [x] Level 3 — Power User: optional ADB / device-owner / elevated device-management integrations where supported
- [x] Explicit storage capability status: granted / partial / denied / not supported
- [ ] Notification access
- [x] Overlay permission
- [x] Microphone permission
- [ ] Background execution capability
- [x] Permission status monitoring

## Phase 5 — Android Control
- [x] Accessibility service
- [x] Screen inspection
- [x] Gesture/input execution
- [x] App launching
- [x] Global navigation actions
- [x] Permission-aware device actions

## Phase 6 — Vision
- [x] Screenshot pipeline
- [x] Visual understanding
- [x] Accessibility + vision fusion
- [x] Robust UI target resolution
- [x] Target verification

## Phase 7 — Browser + Files
- [x] Chrome automation foundation
- [~] AI-native browser option
- [x] File agent
- [x] File search/read/write/move/copy/delete
- [~] Download/upload workflows
- [x] Storage-aware routing across Level 1 / Level 2 / Level 3
- [x] Safe handling of protected Android paths and unavailable system areas
- [x] Bulk file operations with confirmation gates

## Phase 8 — Tavily Web Search
- [x] Server-side web search foundation
- [x] Tavily integration
- [x] Bounded research workflows
- [x] Source/citation handling foundation
- [x] Secret-safe API configuration
- [~] Android-to-backend runtime integration
- [~] Citation rendering in production chat UI
- [ ] Production backend deployment and health verification
- [ ] End-to-end Android → backend → Tavily → chat verification

## Phase 9 — Memory + Tasks
- [x] Room schema v2 for memory and persistent tasks
- [x] Conversation memory with bounded recent-message retrieval
- [x] Long-term memory with relevance scoring
- [x] User-controlled memory management
- [x] Persistent task records and task steps
- [x] Task lifecycle and checkpointing
- [x] Resumable task discovery after restart
- [x] Application-scoped memory/task repositories
- [x] Unit coverage for memory relevance and task resume behavior
- [x] Chat UI memory controls
- [x] Task UI and task detail timeline
- [x] Full orchestrator integration for automatic task checkpointing
- [~] End-to-end process/device restart verification

## Phase 10 — Voice
- [ ] Speech-to-text
- [ ] Text-to-speech
- [ ] Voice agent mode
- [ ] Voice-triggered tasks

## Phase 11 — Background Agent
- [ ] Foreground/background task execution
- [ ] Notifications
- [ ] Scheduled workflows
- [ ] Recovery after interruption
- [ ] Persistent task runner

## Phase 12 — Power User / Device Owner / ADB
- [ ] Optional ADB integration
- [ ] Optional device-owner integrations where appropriate
- [ ] Advanced device administration capabilities
- [ ] Power-user diagnostics and controls
- [ ] Local model support
- [ ] Level 3 storage/system capability integration

## Storage Access Strategy

Vynnra uses a capability-based three-level storage model:

### Level 1 — Standard / Scoped Storage
Designed for normal Android installations without elevated storage access.
- App-owned files
- User-selected files and directories through Storage Access Framework
- Standard Documents / Downloads / Pictures / media workflows where Android grants access
- No assumption of unrestricted filesystem access

### Level 2 — Full Storage
The preferred full-storage mode for the main Vynnra Agent experience.
- Request the appropriate Android all-files capability where the device/app distribution is eligible
- Broad access to shared/external user storage
- File search, read, write, copy, move and delete across accessible shared-storage locations
- Vynnra must still detect and report paths protected by Android or unavailable to ordinary applications

### Level 3 — Power User
An optional advanced mode for specially configured devices.
- ADB-assisted operations
- Device-owner integrations where applicable
- Advanced diagnostics and device administration
- Additional system-level workflows only where the device configuration and Android security model permit them
- Root access is not assumed; a rooted/custom-ROM environment is required for genuinely root-only operations

The UI must always display the active storage level and the exact capability status instead of presenting access as universally unrestricted.

## Build Verification

Latest Phase 9 checkpoint:
- Current Phase 9 implementation commit series includes persistent memory/task UI, orchestrator checkpoint integration, and restart persistence coverage.
- Workflow run `35197665052` compiled the APK successfully and passed `testDebugUnitTest`.
- The Android emulator restart stage timed out at the hosted runner's emulator/test boundary (exit code 124) after prolonged ADB/emulator startup; no application assertion failure was reported in the captured log.
- The restart item therefore remains `[~]` pending reliable emulator/device verification.

The previous fully verified CI checkpoint before Phase 9 work remains:
- Workflow run: `35192201354`
- Build job: `105107062337`
- Head commit: `e158e62e8b4c5f02089b1f9e946e2d3b8404d2ab`
- Build result: **success**
- Debug APK artifact: `vynnra-agent-debug-apk`
- Artifact ID: `10484980571`
- Artifact size: 37,447,990 bytes
- Artifact SHA-256: `ac32fac55e7ff1fac710f498b5e6a5e947d716f66f3a4717b929ab655c674942`

## Vynnra Agent 1.0

The 1.0 milestone begins only after the 12 phases above are implemented, tested, and integrated into a coherent agent experience.
