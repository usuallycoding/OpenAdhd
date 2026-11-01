# Design

## Context

Dynamic color already exists: `AppTheme` calls `platformColorScheme(darkTheme)`, whose Android actual returns the system scheme on API 31+ and whose desktop and iOS actuals return `null`, meaning "use the app's own scheme". Settings are `SettingKey` entries; `Settings.overrides` stores only values that differ from the default, and `fromOverrides` ignores invalid stored values, so backup, restore and delete-all already cover any new key. Theme is the precedent for a choice setting (`choices`, a sheet in `SettingsScreen`). Export and Import are hidden through `canTransfer`, the precedent for a platform-dependent control. `AlarmActivity` calls `AppTheme { }` with no arguments.

## Goals / Non-Goals

**Goals:**
- A Dynamic color setting that turns the existing behaviour off and on, default on.
- Listed only where the platform supports it.

**Non-Goals:**
- Medium and high contrast schemes.
- Making the alarm screen follow the saved settings (theme mode or dynamic color). It keeps its defaults, a known gap that exists today for theme mode.
- A Switch control or any new row type.

## Decisions

- **Stored as a choice key.** `SettingKey.DynamicColor("dynamic_color", "Dynamic color", Appearance, null, listOf("dynamic", "app"))`, with `Settings.dynamicColor: Boolean = true`. Alternative: a boolean key type. Rejected because it adds a third kind of setting for one use, while the choice path already gives the sheet, display labels, validation, Restore and backup. The key is declared after Theme so it lists after it.
- **Labels.** `display` maps "dynamic" to "Dynamic (wallpaper colors)" and "app" to "App colors".
- **Platform support as an `expect` value.** Add `internal expect val supportsDynamicColor: Boolean` beside `platformColorScheme` (Android: `SDK_INT >= S`; desktop and iOS: `false`). `platformColorScheme(darkTheme, dynamic)` returns `null` when `dynamic` is false. Alternative: probe `platformColorScheme` for null from the view model. Rejected because it is a composable and the view model should not depend on it.
- **Hiding the row.** `SettingsViewModel.buildSections` takes the support flag, defaulting to the platform value, and omits `DynamicColor` when it is false. A constructor parameter keeps the view model testable without a device, like `files`. The key stays in `SettingKey.entries`, so a backup from Android restores on any platform and the value is kept harmlessly where it has no effect.
- **Wiring.** `AppTheme(themeMode, dynamicColor = true)`; `App.kt` passes `settings.dynamicColor`. The alarm screen's default of `true` preserves its current look.
- **Sheet.** A `DynamicColorSheet` modelled on `ThemeSheet`, with test tags `dynamic-color-option-<name>`. The screen picks the sheet by key, as it does for Theme.

## Risks / Trade-offs

- [Alarm screen ignores the setting] → Documented non-goal; same gap as theme mode today.
- [A backup with Dynamic color off, restored on an unsupported platform] → The value is stored and has no effect; if the user later uses it on Android 12+ it applies.
- [Settings read before the first frame] → The new field lives in `Settings`, which is already read before the first frame, so there is no flash of the wrong scheme.
