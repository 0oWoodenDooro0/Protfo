# Agent Instructions

## Architecture

- `shared`: Kotlin Multiplatform module containing Room KMP SQLite database, domain models, repositories, and portfolio calculation use cases.
- `composeApp`: Compose Multiplatform Android application containing Material 3 UI screens, theme, and view models.

## Verification & Commands

- Run all unit and Robolectric UI tests: `./gradlew test`
- Run lint and full project check: `./gradlew check`
- Build debug Android APK: `./gradlew assembleDebug`

## UI & Coding Standards

- Strict prohibition of emojis across UI text, labels, and icons. Always use standard Material 3 vector icons (`androidx.compose.material.icons`).
- Number formatting: Use `NumberFormatHelper` for currency, share count, and fee formatting.

## Agent Skills

### Issue Tracker

Issues and specs are tracked in GitHub Issues using the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage Labels

Canonical triage roles mapped to GitHub labels (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`). See `docs/agents/triage-labels.md`.

### Domain Docs

Single-context repository (`GLOSSARY.md` and `docs/adr/` at the root). See `docs/agents/domain.md`.
