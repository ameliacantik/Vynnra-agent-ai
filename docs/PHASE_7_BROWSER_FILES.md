# Phase 7 — Browser + Files

## Implementation checklist

- [~] Chrome automation foundation
- [ ] Chrome target navigation and extraction hardened with device runtime coverage
- [ ] AI-native browser option
- [x] Capability-aware file agent
- [x] File search/read/write/move/copy/delete primitives
- [x] Protected Android path rejection
- [x] Level 2 shared-storage capability declaration
- [x] Bulk file delete with explicit confirmation and bounded batch size
- [~] Download workflow via Android DownloadManager
- [~] Upload workflow via Android document-picker handoff

## Browser contract

Vynnra's initial Chrome controller uses Android intents for URL navigation and the existing Accessibility service for click, type, scroll, back, and page-text extraction. This keeps the implementation compatible with Chrome UI changes without assuming a private DOM interface.

The controller falls back to the device default browser if Chrome is unavailable.

## File contract

The file agent supports app-owned storage on normal Android installations and shared-storage filesystem operations when Level 2 all-files access is granted. It canonicalizes paths, rejects protected Android/system prefixes, bounds recursive traversal, and exposes a capability-status result.

Destructive and mutating tool definitions require explicit confirmation. Bulk deletion is capped at 100 paths per invocation.

## Download / upload boundary

Downloads are queued through Android `DownloadManager` into the public Downloads directory. Upload is currently represented as a user-mediated document-picker handoff; selecting a file and driving the resulting browser upload control still requires device-level integration testing.

## Verification boundary

CI verifies compilation and APK generation. Chrome behavior, Accessibility coverage of Chrome/DocumentsUI, DownloadManager behavior, OEM storage behavior, and end-to-end upload selection require real-device verification before the corresponding checklist items can move to `[x]`.
