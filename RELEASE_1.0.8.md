# BooksOpdsApp 1.0.8

Release date: 2026-05-17

## Version

- Version Name: `1.0.8`
- Version Code: `3`
- Application ID: `com.example.booksopdsapp`

## Highlights

- Updated app version metadata to `1.0.8`.
- Increased `versionCode` to `3`.
- Disabled debug URL popups for normal app usage.
- Documented the current app behavior and development notes in `README.md`.

## App Capabilities

- Browse OPDS feeds and child categories.
- Navigate OPDS pagination links.
- Search OPDS catalogs when search links are available.
- View book details with cover, title, author, summary, and available download formats.
- Download books through Android DownloadManager.
- Resolve file names from server headers when available.
- Avoid duplicate active download requests.
- Save OPDS profiles with encrypted passwords.
- Import and export OPDS profile JSON files.
- Toggle and remember dark mode.

## Notes

- Debug popup support remains in the code path but is disabled by `DEBUG_POPUP_ENABLED = false`.
- HTTP cleartext traffic is currently allowed for local or private OPDS servers.
- No full Gradle build was run for this documentation update, following the local project instruction to avoid compiling unless explicitly requested.
