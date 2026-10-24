# Proposal

## Why

The theme picker is a small icon in the top bar, a place that is already crowded and that holds nothing else the user chooses. The choice is also forgotten on every restart: the app always starts in "follow system", so someone who prefers dark has to switch it again each time they open the app. Settings is where choices are kept, so the theme belongs there, and it has to be remembered.

## What Changes

- **Theme moves to Settings:** a new Appearance group, shown first, holds a Theme row with its current value (Follow system theme, Light or Dark) and its default. Tapping it opens a bottom sheet with the three options, each with its icon and label and the current one highlighted. Choosing an option applies it at once and closes the sheet.
- **Top bar loses the button:** the theme icon and its dropdown are removed, so the top bar holds only the title, the Reorder control when it applies, and the balance.
- **The choice is saved:** the theme is stored like any other setting. The next start uses it, from the first frame, without a flash of the wrong colours. Restore puts back Follow system theme.
- **Everything that already covers settings covers the theme:** it is part of a backup and comes back on import, and Delete all data resets it to Follow system theme.
- **No flicker at start for the name either:** the settings are read once before the first frame is drawn, which also fixes the top bar title briefly showing "OpenAdhd" before the saved name appears.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `app-settings`: an Appearance group with the Theme setting, its default, its options sheet, and keeping it.
- `app-theming`: the theme mode is kept between launches, which replaces "Theme choice is not persisted".
- `app-topbar`: the theme chooser and its accessibility requirement are removed from the top bar.

## Impact

- `shared` domain: `ThemeMode` moves to the domain package; `Settings` gains the theme; `SettingKey.Theme` with a fixed set of choices and its validation; a new `Appearance` setting group.
- `shared` data: no database change, because settings are stored as key and value rows; `AppContainer` gains the settings read before the first frame.
- `shared` UI: `App.kt` takes the theme from the settings and the top bar loses the button; `ThemeChooser` becomes the options list in a Settings sheet; the Theme row.
- Platform entry points: no change beyond what `createContainer` does.
- Tests: the old top-bar theme test moves to Settings; new tests for saving, restoring, validation, keeping across a restart, the backup, and Delete all data.
- No new libraries and no migration.
- Out of scope: more themes or colours, a per-screen theme, and scheduling dark mode by time of day.
