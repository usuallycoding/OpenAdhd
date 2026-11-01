# Tasks

## 1. Setting model

- [x] 1.1 Add `dynamicColor` (default true) to `Settings` and `SettingKey.DynamicColor` after `Theme` in the Appearance group, with `text`, `with`, `display` and the labels; verify with unit tests in `SettingsTest` for default, text round trip, `overrides` (absent when on, present when off) and `fromOverrides` ignoring an invalid value
- [x] 1.2 Verify Restore, backup export and import and delete-all keep and reset the new key by extending the existing backup and store contract tests

## 2. Theme

- [x] 2.1 Add `expect val supportsDynamicColor` with actuals (Android `SDK_INT >= S`, desktop and iOS false) and a `dynamic` parameter on `platformColorScheme` that returns null when false; verify the app compiles for Android, desktop and iOS targets
- [x] 2.2 Add `dynamicColor` to `AppTheme`, pass `settings.dynamicColor` from `App.kt`, and leave `AlarmActivity` unchanged; verify with a theme test that off yields the app's own scheme and that toggling applies without a restart

## 3. Settings screen

- [x] 3.1 Make `SettingsViewModel` omit `DynamicColor` when dynamic color is unsupported, through a constructor parameter that defaults to `supportsDynamicColor`; verify with view model tests for both cases
- [x] 3.2 Add `DynamicColorSheet` and open it for the key in `SettingsScreen`; verify with a UI test like `ThemeSettingUiTest` that choosing "App colors" closes the sheet, shows the new value and enables Restore
- [x] 3.3 Run the full test suite and, on an Android 12+ device or emulator, confirm the colors switch between wallpaper and app colors and that the row is absent on Android 11 or desktop
