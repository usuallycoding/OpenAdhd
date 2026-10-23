# Proposal

## Why

The shared module's new theme (`Color.kt`, `Theme.kt`, `Type.kt`) cannot compile for the non-Android targets: it uses Android-only APIs (`Build`, `LocalContext`, dynamic color, Google Fonts provider, `R.array`) from common code, and has unresolved references and `com.example` packages. The app also has no way for the user to choose between following the system theme, light, or dark.

## What Changes

- Fix the shared theme so `shared` compiles on jvm, android, ios and wasmJs:
  - Move theme code to the `com.usuallycoding.openadhd.ui.theme` package and update `App.kt`.
  - Remove Android-only APIs from common code; dynamic color becomes an Android-only platform hook.
  - Replace the Google Fonts provider with the platform default font families.
  - Resolve the missing `AppTypography` reference and remove unused imports.
- Add a top app bar to `App()` with an app title and a three-option theme chooser (follow system, light, dark), each shown as an icon.
- Add a `ThemeMode` preference (System, Light, Dark) held in memory; it resets to System on every launch.

## Capabilities

### New Capabilities
- `app-theming`: How the app resolves its light/dark appearance from the selected theme mode, including Android dynamic color when available.
- `app-topbar`: The top app bar and its theme chooser control.

### Modified Capabilities

## Impact

- `shared/src/App.kt`, `shared/src/ui/theme/{Theme,Type}.kt`; new topbar/chooser composables and icon vectors in `shared`.
- New Android-only source under `shared/src@android` for dynamic color.
- No new library dependencies. `Color.kt` values are unchanged.
- Out of scope: persisting the theme choice, bundling custom fonts, medium/high-contrast themes.
