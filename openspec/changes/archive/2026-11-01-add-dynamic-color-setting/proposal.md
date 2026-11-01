# Proposal

## Why

On Android 12 and newer the app always takes its colors from the wallpaper, and the user cannot opt out. Some people prefer the app's own palette, so the dynamic color the app already has should be a choice, kept with the other appearance settings.

## What Changes

- Add a "Dynamic color" setting in the Appearance group, next to Theme. It is a two-choice sheet like Theme: "Dynamic (wallpaper colors)" or "App colors". The default is dynamic, so existing Android 12+ installs look the same.
- The setting is only listed on platforms that offer dynamic color (Android 12 or newer). It is hidden on older Android, desktop and iOS.
- When "App colors" is chosen the app uses its own light or dark scheme, as it does on every other platform.
- The choice is kept between launches, restored with Restore, and is part of a backup like every other setting.
- Out of scope: the medium and high contrast schemes, and the alarm screen, which today ignores the saved theme mode and keeps using the default theme.

## Capabilities

### New Capabilities

### Modified Capabilities
- `app-theming`: the Android dynamic color requirement becomes conditional on the new setting, and the setting is kept like the theme mode.
- `app-settings`: a new Dynamic color setting with its default, shown only where the platform supports it.

## Impact

- `shared`: `Settings` and `SettingKey` (new key, stored in the existing settings table, no migration), `AppTheme`, the `platformColorScheme` actuals, the Settings view model and screen, and the Dynamic color sheet.
- Backup format: one more optional settings key, which older backups simply lack.
- `androidApp`: no change; `AlarmActivity` keeps calling `AppTheme` with defaults.
