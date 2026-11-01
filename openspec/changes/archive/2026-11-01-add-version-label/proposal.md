# Proposal

## Why

Nothing in the app says which build is running, so a bug report or a question like "is this the new APK?" cannot be answered from the screen. The version is also not defined anywhere in a consistent way: the Android build reports `versionName` "unspecified", the iOS project says 1.0 and the macOS packaging script says 1.0.0.

## What Changes

- Settings ends with a small, dimmed, centered label below the Data section, such as "OpenAdhd 1.0.0". On Android and iOS it also shows the build number, "OpenAdhd 1.0.0 (1)". It is not tappable and is not a setting.
- Each platform shows what the installed app actually reports: Android its version name and code, iOS its short version and bundle version, the packaged macOS app its jpackage version. The app run from the build tool has no packaged version and shows "OpenAdhd dev".
- Every platform gets a real, matching starting version, 1.0.0 with build 1: Android's `versionName` and `versionCode` are set (they were unset), the iOS Info.plist states its version directly, and the macOS script already says 1.0.0.
- The README says where to change the version when releasing.

Out of scope: tapping the label to copy it, a release script that bumps every place at once, an About screen, and update checks.

## Capabilities

### New Capabilities

None.

### Modified Capabilities
- `app-settings`: Settings gains the version label at its end, so the "Settings tab" requirement (what the page lists and in what order) changes, and a new requirement describes the label and where its version comes from.

## Impact

- `shared`: a small version-text helper, a `versionLabel` carried by `AppContainer` and `SettingsViewModel`, and the label row in `SettingsScreen`.
- `androidApp/module.yaml`: `versionName` and `versionCode`; `createAndroidContainer` reads them from the package info.
- `iosApp/src/Info.plist`: literal `1.0.0` and build `1` in place of the project variables, which the toolchain regenerates; `createIosContainer` reads the bundle info.
- `createDesktopContainer`: reads `jpackage.app-version`.
- `README.md`: where to bump the version.
- No database change and no new dependencies.
