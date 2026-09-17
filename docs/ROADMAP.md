# Vynnra Agent AI Roadmap

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
- [ ] Orchestrator
- [ ] Tool registry
- [ ] Planner/executor loop
- [ ] Observe → Understand → Plan → Act → Verify → Recover → Respond
- [ ] Verification engine
- [ ] Recovery engine
- [ ] Action journal
- [ ] Cancellation / emergency stop

## Phase 4 — Permission Center
- [ ] Capability inventory
- [ ] Accessibility permission flow
- [ ] Screen-capture consent flow
- [ ] Storage/file capability flow
- [ ] Notification access
- [ ] Overlay permission
- [ ] Microphone permission
- [ ] Background execution capability
- [ ] Permission status monitoring

## Phase 5 — Android Control
- [ ] Accessibility service
- [ ] Screen inspection
- [ ] Gesture/input execution
- [ ] App launching
- [ ] Global navigation actions
- [ ] Permission-aware device actions

## Phase 6 — Vision
- [ ] Screenshot pipeline
- [ ] Visual understanding
- [ ] Accessibility + vision fusion
- [ ] Robust UI target resolution
- [ ] Target verification

## Phase 7 — Browser + Files
- [ ] Chrome automation
- [ ] AI-native browser option
- [ ] File agent
- [ ] File search/read/write/move/copy/delete
- [ ] Download/upload workflows
- [ ] Full Storage capability tier selected as the target default for user-authorized broad shared-storage access
- [ ] All-files access setup/status flow where supported and appropriate
- [ ] Storage capability reporting: ACCESS_GRANTED / ACCESS_PARTIAL / ACCESS_DENIED / NOT_SUPPORTED
- [ ] Explicit handling of Android-protected app-private/system areas without claiming unrestricted filesystem access
- [ ] Confirmation gate for destructive file operations (delete/overwrite/bulk move)

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

## Vynnra Agent 1.0

The 1.0 milestone begins only after the 12 phases above are implemented, tested, and integrated into a coherent agent experience.
