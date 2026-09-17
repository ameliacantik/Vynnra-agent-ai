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
- Concrete Phase 5 `VynnraTool` implementations for Android actions.
- Central `ToolRegistry` registration through `AndroidToolRegistrar`.
- Capability-aware tool definitions for Accessibility and screen interaction.
- `AndroidVerificationEngine` for post-action UI assertions such as expected visible text.
- `AndroidAgentTools` wiring the controller, registry, capability gate, verifier, recovery engine, and orchestrator.

## Verification model
Android tools expose `supportsVerification` where an asynchronous UI change is expected. A plan can provide `expectedText` as a post-condition. The verifier waits briefly for the UI to settle, checks the active Accessibility tree, and only then considers the action verified. When no deterministic post-condition is supplied, verification falls back to the tool's explicit success result; this is intentionally weaker and should be strengthened with screen-state assertions in Phase 6.

## Safety and platform boundaries
- Actions require the corresponding Android capability to be enabled by the user.
- The implementation does not attempt to bypass Android sandboxing, app-private storage, lock-screen security, or other platform protections.
- Gesture dispatch remains asynchronous at the Android API level; verification is responsible for checking the resulting UI state rather than assuming dispatch means success.
- Destructive, financial, communication, installation, and security-sensitive actions must remain behind confirmation gates in the orchestrator/tool layer.

## Remaining Phase 5 work
1. App/package discovery.
2. Cancellation-aware gesture callbacks and richer gesture result reporting.
3. Stronger package/activity verification for app launch.
4. Integration with Phase 6 screen capture/vision.
5. Physical-device runtime testing across several Accessibility-enabled apps.
