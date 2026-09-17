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
- [ ] Server-side web search
- [ ] Tavily integration
- [ ] Research workflows
- [ ] Source/citation handling
- [ ] Secret-safe API configuration

## Phase 9 — Memory + Tasks
- [ ] Conversation memory
- [ ] Long-term memory
- [ ] Persistent tasks
- [ ] Task state/resume after restart
- [ ] User-controlled memory management

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

Latest verified Phase 7 CI run:
- Workflow run: `35191428977`
- Build job: `105104644185`
- Head commit: `41cd58344fa8c57a54dd37c6f188e5ecb63384fa`
- Build result: **success**
- Debug APK artifact: `vynnra-agent-debug-apk`
- Artifact ID: `10482934778`
- Artifact size: 37,426,255 bytes (~37.4 MB)
- Artifact SHA-256: `f40cd4647e22b35ca1a12bb2ea48085079ef373770ecf3675777ed793e3fe8c6`

CI success verifies compilation and APK generation. Real-device runtime testing is still required for Chrome UI behavior, AI-native browser behavior, Android DocumentsUI/upload handoff, DownloadManager/OEM behavior, and end-to-end browser/file workflows.

## Vynnra Agent 1.0

The 1.0 milestone begins only after the 12 phases above are implemented, tested, and integrated into a coherent agent experience.
