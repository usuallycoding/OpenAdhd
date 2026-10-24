# Tasks

## 1. The setting

- [x] 1.1 Move `ThemeMode` to the domain package with a `label`, add `Settings.themeMode`, `SettingKey.Theme` with `choices`, the `Appearance` group listed first, choice validation, and `Settings.fromOverrides` ignoring a value that is not a choice (verify: `SettingsTest` covers the default, each choice, an unknown value refused and ignored when stored, Restore, and the override being stored only when it differs from System)
- [x] 1.2 Show choices by their labels in `SettingRow` (verify: `SettingsViewModelTest` shows "Follow system theme" as value and default, and "Dark" after choosing it)

## 2. The first frame and the app shell

- [x] 2.1 Read the settings once in `createContainer` into `AppContainer.initialSettings`, use it as the initial state in `App`, take the theme from the settings, and remove the local theme state (verify: a test that creates a container over a store with a saved theme and name and checks the initial settings, and a UI test that the first composition already uses the dark theme and shows the name's title)
- [x] 2.2 Remove the theme button from the top bar and delete `ThemeChooser` (verify: the project builds and a UI test shows the top bar has no theme control)

## 3. The Settings screen

- [x] 3.1 Add the Theme row and its options sheet to `SettingsScreen`: three rows with icon and label, the current one highlighted, choosing applies and closes (verify: UI tests for the Appearance group being first, the row's value and default, choosing Dark, the highlight, Restore and its disabled state, and replacing the old top-bar theme test in `AppUiTest`)

## 4. Persistence across restart and backup

- [x] 4.1 Cover keeping the theme (verify: `RoomStoreTest` saves a theme, restarts and reads it back; a backup round-trip test keeps it; deleting all data resets it to System in the contract tests)

## 5. Docs

- [x] 5.1 Mention in the README that the theme is chosen in Settings under Appearance and is remembered (verify: the sentence matches 6.2)

## 6. Verification

- [x] 6.1 Run `./kotlin build` and `./kotlin test` for all targets (verify: both succeed with no failing tests)
- [x] 6.2 On macOS: choose Dark with the system set to light, restart the app and see it open dark with no flash, restore and see it follow the system, and see the name in the title at once (verify: manual, as the specs say)
- [x] 6.3 On Android: the same, including the app starting from a closed state (verify: manual)
- [x] 6.4 On iOS: the same (verify: manual)

## Workflow follow-up

- When archiving, rewrite the Purpose line of `app-topbar` (it still mentions choosing the theme), because syncing a delta leaves Purpose untouched.
