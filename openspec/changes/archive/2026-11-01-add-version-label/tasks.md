# Tasks

## 1. The label text and the Settings row

- [x] 1.1 Add `AppVersion.label(name, build)` in shared code ("OpenAdhd 1.0.0 (1)" with a name and build, "OpenAdhd 1.0.0" without a build, "OpenAdhd dev" when the name is missing or blank, a blank build left out), a `versionLabel` on `AppContainer` (default "OpenAdhd dev") and `createContainer`, and a `versionLabel` parameter on `SettingsViewModel` filled from the container in `App.kt`. Verify with a unit test in `shared/test` for every case of the helper and for the view model exposing the label it was given, and that `./kotlin test -m shared` passes
- [x] 1.2 Show the label as the last `LazyColumn` item of `SettingsScreen` after the Delete all data row: small body text in the muted color, centered, vertical padding, not clickable, with a test tag. Verify with a Compose UI test next to `SettingsUiTest`: the label shows the injected text, it is below the Delete all data row, tapping it opens nothing, and a container with no label shows "OpenAdhd dev"

## 2. Each platform's own version

- [x] 2.1 Desktop: have `createDesktopContainer` pass `AppVersion.label(System.getProperty("jpackage.app-version"), null)`, taking the property through a parameter so a test can set it. Verify with a unit test: a version gives "OpenAdhd 1.0.0" and no property gives "OpenAdhd dev"
- [x] 2.2 Android: set `versionName: 1.0.0` and `versionCode: 1` under `settings.android` in `androidApp/module.yaml`, and have `createAndroidContainer` read the package info (`longVersionCode` from API 28, the typed-flags call on API 33 and later, "dev" if it cannot be read). Verify by building the APK (`scripts/package-android.sh` or `./kotlin build -m androidApp`) and reading `aapt2 dump badging` for `versionName='1.0.0'` and `versionCode='1'`, and that the label reads "OpenAdhd 1.0.0 (1)" in the app
- [x] 2.3 iOS: put the literal `1.0.0` and `1` in `iosApp/src/Info.plist` in place of the project variables, and have `createIosContainer` read `CFBundleShortVersionString` and `CFBundleVersion` from the main bundle. Verify with a test in `shared/test@ios` for the reading helper with a given dictionary, that `./kotlin test -m shared` still passes the iOS tests, and that the plist holds the two literals

## 3. Docs and integration

- [x] 3.1 Add a short README note listing the three places the version is set (`androidApp/module.yaml`, `iosApp/src/Info.plist`, `scripts/package-mac.sh`) and what the label shows on each platform. Verify the three files hold 1.0.0 and build 1 as the note says
- [x] 3.2 Verify end to end: the packaged macOS app (`scripts/package-mac.sh`) shows "OpenAdhd 1.0.0" at the end of Settings and the app run with `./kotlin run -m desktopApp` shows "OpenAdhd dev"; the Android build shows "OpenAdhd 1.0.0 (1)". Record anything that differs from the spec

## Workflow follow-up

- Archive the change after the tasks above are verified, then sync the specs.
