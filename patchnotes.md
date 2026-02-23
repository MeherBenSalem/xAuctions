# xAuctions v1.2.0 — Patch Notes

## What’s New
- **Sort and filter persistence** in the auction browser – remembers each player's last-used category and sort order.
- Polished startup/shutdown console output with a branded banner, step‑by‑step progress, and runtime info.

## Fixes & Improvements
- Added per‑player preferences manager and eviction on quit to avoid memory leaks.
- Simplified initialization sequence by embedding manager setup directly into startup steps.
- Better integration logging for optional features (PlaceholderAPI/Discord).

## Build Artifact
- Standard jar: `target/xAuctions-1.2.0.jar`
- Obfuscated release jar: `target/xAuctions-1.2.0-obf.jar`

