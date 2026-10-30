# Proposal

## Why

`scripts/package-android.sh` builds the debug APK, signed with the debug key in `~/.android/debug.keystore`. That key belongs to one Mac, so the APK can only be updated from that machine, and the app is shipped as a debuggable build. A proper release key, kept outside git, makes the APK a normal signed release that installs over earlier releases from any machine that has the key.

## What Changes

- **Secrets directory:** a `secret/` directory at the project root, ignored by git, holds `android-release.keystore` and `android-signing.properties` (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`). The README documents the `keytool` command that creates the keystore; no script generates it.
- **Signing configuration:** `androidApp/module.yaml` enables the toolchain's Android signing and points it at `secret/android-signing.properties`.
- **Script:** `scripts/package-android.sh` builds the release variant, which comes out signed, copies it to `dist/OpenAdhd.apk`, and still checks the signature with `apksigner`. It stops with a clear message when `secret/` is missing or incomplete, and it never falls back to the debug build.
- **Docs:** the README Packaging section describes the release key, how to create it, that `secret/` must be backed up (losing the keystore means no further updates over an installed app), and that the first install replaces a debug-signed one, so a backup should be exported and imported around it.
- **BREAKING (one time):** Android refuses to update an APK signed with a different key, so the debug-signed app on the phone must be uninstalled before the first release APK is installed, which erases its data unless it was exported first.

## Capabilities

### New Capabilities
None. This is build tooling and changes no app behavior, so the change sets `skip_specs: true`.

### Modified Capabilities
None.

## Impact

- `androidApp/module.yaml`, `scripts/package-android.sh`, `.gitignore` (adds `secret/`) and `README.md`.
- New, untracked: `secret/android-release.keystore` and `secret/android-signing.properties`, created by hand with `keytool`.
- The release build is minified; it must be checked against Room and the bundled SQLite before it is relied on.
- Out of scope: a keystore generator script, Play Store upload, CI, version numbers, and the macOS app.
