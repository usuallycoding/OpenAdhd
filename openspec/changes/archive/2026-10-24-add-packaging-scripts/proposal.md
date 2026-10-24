# Proposal

## Why

The app can only be run from the IDE or with `./kotlin run`. To use it day to day there is no way to get an installable macOS app on the Mac or an APK for the phone, and the toolchain's `package` command stops at jars and an Android App Bundle.

## What Changes

- **macOS script:** `scripts/package-mac.sh` builds the desktop app and wraps it with `jpackage` into `dist/OpenAdhd.app`, with its own bundled Java runtime, so it runs without Java installed. It also produces a `dist/OpenAdhd.dmg`.
- **Android script:** `scripts/package-android.sh` builds the Android app and writes an installable `dist/OpenAdhd.apk`, signed with the Android debug key of this Mac, which stays the same between builds, so a newer build installs over an older one and keeps its data.
- **Output and docs:** `dist/` is ignored by git, and the README says how to run each script and how to install the results.
- **For personal use only:** nothing is notarised, Apple-signed or uploaded anywhere. macOS asks for one confirmation the first time the app is opened.

## Capabilities

### New Capabilities
None. This is build tooling and changes no app behavior, so the change sets `skip_specs: true`.

### Modified Capabilities
None.

## Impact

- New files: `scripts/package-mac.sh`, `scripts/package-android.sh`.
- `.gitignore` gains `dist/`; `README.md` gains a Packaging section.
- No app code, database or dependency change. Requires JDK 21 with `jpackage` and the Android SDK; the toolchain's `build` command produces the APK, so no extra tool is needed.
- Out of scope: Windows and Linux builds, notarisation, Play Store upload, CI, and automatic version numbers.
