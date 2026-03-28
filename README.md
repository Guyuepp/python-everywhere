# Android Process Text + Local Python (Chaquopy)

中文说明见 [README.zh-CN.md](README.zh-CN.md).

An Android app that executes selected text as Python code via the system Process Text menu and shows structured results in a dialog-style overlay.

## What It Does

- Uses Android Process Text as a system entry point.
- Executes selected text as Python code locally (offline) through Chaquopy.
- Shows execution states: Running / Success / Error.
- Supports expandable details: `stdout`, `stderr`, `traceback`.
- Copies normalized result JSON to clipboard.

## Project Structure

- `android-text-python-app/`: Android application project.
- `docs/`: design, milestone verification, testing, and release docs.
  - `docs/m8/m8-1-user-guide.md`: user guide.
  - `docs/m8/m8-2-developer-guide.md`: developer guide.
  - `docs/m8/m8-3-release-checklist.md`: release checklist.
  - `docs/m8/m8-4-mvp-acceptance-review.md`: MVP acceptance review.

## Tech Stack

- Android (Kotlin, Views)
- Chaquopy (embedded Python)
- Coroutines for execution and cancellation control
- JUnit + AndroidX Test / Espresso

Version matrix is documented in:
- `docs/adr/0001-version-matrix-baseline.md`

## Build & Test

Run from `android-text-python-app/`.

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebugAndroidTest
./gradlew :app:connectedDebugAndroidTest
```

## Install

```bash
cd android-text-python-app
./gradlew :app:installDebug
```

## Basic Usage

1. Select text in an app that supports Process Text.
2. Tap `Run as Python`.
3. Check result status and output details.
4. Tap `Copy Result JSON` if needed.

## Known Limitations

Some apps do not expose Android's system Process Text menu. In our tests, some IM apps (WeChat / Telegram / QQ) had this limitation.

Suggested workaround:
- Copy text into an app that supports Process Text (browser/reader/editor), then run it there.

## License

No license file is provided yet. Add a `LICENSE` before open-source distribution if needed.

## Acknowledgements

This repository is maintained by the project author.
Special thanks to GitHub Copilot (GPT-5.3-Codex), which contributed a large portion of the implementation, testing, and documentation scaffolding during development.
