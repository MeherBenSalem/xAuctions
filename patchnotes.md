# xAuctions v1.1.0 — Patch Notes

## What’s New
- Added full hybrid support for both **Paper** and **Folia** in one plugin build.
- Improved task handling to better match modern multithreaded server execution.
- Reduced risk of scheduler/thread warnings on Folia servers.
- Updated platform metadata so Folia is officially declared as supported.

## Runtime Update
- xAuctions now targets **Java 21**.
- Server hosts should run Java 21 for this release.

## Stability & Performance
- Improved scheduling behavior for player/world actions.
- Better background handling for non-gameplay work (storage and other heavy operations).
- General reliability improvements for busy SMP/network environments.

## Build Artifact
- Standard jar: `target/xAuctions-1.1.0.jar`
- Obfuscated release jar: `target/xAuctions-1.1.0-obf.jar`
