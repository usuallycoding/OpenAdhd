# Tasks

## 1. Fix the shared theme

- [x] 1.1 Move `Color.kt`, `Theme.kt`, `Type.kt` to package `com.usuallycoding.openadhd.ui.theme`, update the `App.kt` import, and verify no `com.example` references remain (`grep -r com.example shared androidApp`)
- [x] 1.2 Replace the Google Fonts setup in `Type.kt` with default font families, delete `values-v23/font_certs.xml` if unreferenced, and verify `Type.kt` has no `R` or `googlefonts` references
- [x] 1.3 Remove Android-only imports and APIs from `Theme.kt`, add the `platformColorScheme` platform hook (common declaration, Android implementation with API 31+ dynamic color in `shared/src@android`, null elsewhere), and verify `./kotlin build` succeeds for jvm and android
- [x] 1.4 Remove unused imports and add the missing `AppTypography` reference, and verify the build has no unresolved references on jvm, android, ios and wasmJs targets

## 2. Theme mode

- [x] 2.1 Add `ThemeMode { System, Light, Dark }` and a pure function resolving it plus the system flag to a dark boolean, with unit tests in `shared/test` covering all three modes and both system values; verify `./kotlin test` passes
- [x] 2.2 Change `AppTheme` to take `themeMode` and use the resolver and platform hook, and verify it builds on all targets

## 3. Top bar and chooser

- [x] 3.1 Add code-defined `ImageVector` icons for system, light and dark, and verify they render in a Compose preview
- [x] 3.2 Add the `ThemeChooser` composable (icon button showing the current mode that opens a dropdown of three icon-and-label items, current one highlighted) and verify the selected option follows the `themeMode` argument in a preview
- [x] 3.3 Wrap `App()` content in a `Scaffold` with a `CenterAlignedTopAppBar` with centered title "OpenAdhd" and the chooser, holding `ThemeMode` in `rememberSaveable` starting at System, and verify on the desktop app (`./kotlin run`) that tapping each option switches appearance and the selected state follows

## 4. Integration

- [x] 4.1 Verify the app launches and the chooser works on Android and web (wasmJs), including that on Android 12+ colors are dynamic and the layout fits a compact phone width and the dropdown opens fully on screen
