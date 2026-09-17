# Phase 5 — Android Control

## Goal
Provide a controlled Android action layer that Vynnra's orchestrator can use after the user grants Accessibility capability.

## Implemented
- Accessibility service lifecycle tracking.
- Global Back, Home, Recents, and Notifications actions.
- Coordinate tap gestures.
- Coordinate swipe gestures with bounded duration.
- Text entry into the focused/editable accessibility node.
- Accessibility-tree text lookup.
- Click-by-text using the nearest clickable parent.
- Launch an installed app through its launch intent.
- A single `AndroidController` facade for agent tool integration.
- Explicit blocked result when Accessibility is unavailable.

## Safety and platform boundaries
- Actions require the corresponding Android capability to be enabled by the user.
- The implementation does not attempt to bypass Android sandboxing, app-private storage, lock-screen security, or other platform protections.
- Gesture dispatch is asynchronous; later verification should inspect resulting UI state rather than assuming dispatch means success.
- Destructive, financial, communication, installation, and security-sensitive actions must remain behind confirmation gates in the orchestrator/tool layer.

## Next integration work
1. Register concrete Android tools in `ToolRegistry`.
2. Add post-action verification using the accessibility tree and screen state.
3. Add app/package discovery.
4. Add cancellation-aware gesture callbacks.
5. Integrate with Phase 6 screen capture/vision.
