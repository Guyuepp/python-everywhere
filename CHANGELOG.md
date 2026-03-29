# Changelog

All notable changes to this project are documented in this file.

## Unreleased

## v0.1.3 - 2026-03-29

### Added
- Launcher now opens a dedicated execution history screen.
- Execution history persistence for Process Text runs, capped to the latest 100 records.
- History interactions: refresh, clear-all (with confirmation), and per-item detail dialog.
- History now includes input preview, result preview, and message for each record.
- Instrumentation coverage for launcher and Process Text intent routing.

### Changed
- Split app entry routing: `HistoryActivity` handles `MAIN/LAUNCHER`, `MainActivity` handles `PROCESS_TEXT` only.
- Updated user and developer docs for history-first launcher behavior.

### Release
- Published signed APK release `v0.1.3`.
- Signed artifact: `python-everywhere-v0.1.3-release.apk`.
- Signature verification: APK Signature Scheme v2/v3 verified.

### Verification
- Unit tests passed.
- Core instrumentation tests passed on connected Android device (7/7).

## v0.1.1 - 2026-03-28

### Changed
- Refined acknowledgements in English and Chinese README files.
- Added release and changelog links in both README files.
- Bumped Android app version to `versionCode = 2` and `versionName = "0.1.1"`.

## v0.1.0 - 2026-03-28

### Added
- Initial public MVP release.
- Android Process Text flow for running selected text as local Python code.
- Structured output view: status, stdout, stderr, traceback, and copyable JSON.
- Offline runtime with Chaquopy integration and cancellation/timeout controls.
- User and developer documentation set under `docs/`.
