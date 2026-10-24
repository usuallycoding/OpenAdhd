# Design

## Context

The theme is `ThemeMode` (System, Light, Dark) held in `App.kt` as `rememberSaveable { ThemeMode.System }` and changed by `ThemeChooser`, a dropdown in the top bar. Nothing is saved, and `app-theming` says so. Settings are a `Settings` value read from the database as a flow, with each changed value stored as a key and text row; `SettingKey` knows each key's label, group and range, and the Settings screen lists rows grouped by `SettingGroup`, each with a Restore button and an edit sheet. `App` collects `container.store.settings` with `Settings.Default` as its initial value, so the first frame uses defaults until the first read arrives (today only the top bar title notices). The old top-bar test lives in `AppUiTest`. See proposal.md for motivation.

## Goals / Non-Goals

**Goals:**
- The theme is a normal setting: default, Restore, backup, Delete all data and restart persistence all come from the existing settings code.
- The saved theme is used from the first frame, with no flash of the wrong colours.
- A choice editor for a setting that has a fixed set of options.

**Non-Goals:**
- New themes or colours, scheduled dark mode, a theme per screen, or a database migration.

## Decisions

- **The theme is a `SettingKey`.** `SettingKey.Theme` with storage key `theme` and the values `System`, `Light` and `Dark`, in a new `SettingGroup.Appearance` listed first. `Settings` gets `themeMode`. `ThemeMode` moves from the UI package to the domain package, because the domain `Settings` now holds it; it gains a `label` ("Follow system theme", "Light", "Dark") shared by the row and the sheet. No migration is needed, as settings rows are key and value text.
- **Choice settings.** `SettingKey` gets an optional list of allowed `choices`. Validation accepts only one of them ("Choose one of the options" otherwise), and `Settings.fromOverrides` ignores a stored value that is not allowed, so an unknown value falls back to System and can never break the app. A key is a number, a name or a choice. Alternative: store the theme outside the settings table in its own preference; rejected because it would need its own backup, reset and restart code on three platforms.
- **Value shown for a choice.** `SettingRow` shows the option's label instead of its stored text, for both the value and the default.
- **Editing a choice.** Tapping the Theme row opens a `FormSheet` with one radio-style row per option, each with its icon and label and the current one highlighted. Tapping an option calls `saveSetting` and closes the sheet; there is no Save button, because a choice among three needs no confirmation. The `ThemeChooser` dropdown is deleted and its option list, icons and highlight are reused by the sheet. Other rows keep the text editing sheet.
- **Read the settings before the first frame.** `createContainer` reads the settings once with `runBlocking` while it builds the container, before any UI exists, and keeps them as `AppContainer.initialSettings`. `App` uses them as the initial value of its settings state, replacing `Settings.Default`. Room's queries run on its own dispatcher, so this does not block on itself, and it costs one small read at start. This also fixes the title flicker for the name. Alternatives: draw nothing until the first read arrives (still shows the wrong background if the blank box does not match the saved theme), or keep a second copy of the theme in a platform preference (three more implementations, and a second place the theme lives); both rejected.
- **App shell.** `App` takes the mode from `settings.themeMode` and passes it to `AppTheme`; the local `themeMode` state is removed, and the top bar no longer renders the chooser.
- **Spec housekeeping.** `app-topbar`'s Purpose sentence still mentions the theme control. The requirements are removed by the delta; the Purpose line in the main spec is rewritten by hand when the change is archived, since a sync leaves Purpose untouched.

## Risks / Trade-offs

- [The start-up read adds a small delay] → One query on a small table; it replaces a flash with a few milliseconds before the first frame.
- [`runBlocking` on the Android main thread] → Room runs the query on its own dispatcher, and the container is created lazily in the activity, so the wait is only for that one read.
- [A saved value that no longer exists] → Ignored on read, so the theme falls back to System.
- [Existing users lose nothing, but the button moves] → The README and the Settings screen show where it went; there is no data change.

## Migration Plan

None for data: the theme is just another settings row, and nobody has one yet, so everyone starts on Follow system theme as before. The top bar button disappears with the update.
