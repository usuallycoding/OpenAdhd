# Design

## Context

`shared` is an Amper `kmp/lib` (jvm, android, iosArm64, iosSimulatorArm64, wasmJs); all four apps call `App()`. `Theme.kt` and `Type.kt` sit in common source but use Android-only APIs, so only Android could ever compile them. They also use `com.example.*` packages, and `Theme.kt` references `AppTypography` without importing it. `Color.kt` is plain `Color` values and is fine. See proposal.md for motivation.

## Goals / Non-Goals

**Goals:**
- `shared` compiles on all targets; theme resolved from a `ThemeMode`.
- Topbar with an icon-only three-way chooser, no new library dependencies.

**Non-Goals:**
- Persisting the preference, custom fonts, wiring up the medium/high-contrast schemes, app navigation.

## Decisions

- **ThemeMode state lives in `App()`** as `rememberSaveable { mutableStateOf(ThemeMode.System) }` and is passed down. This survives Android configuration changes but not process death or restarts, which matches the in-memory requirement. Alternative: a ViewModel; unnecessary for one value. `ThemeMode` must be saveable (enum, so the default saver handles it via its name or ordinal; confirm during implementation).
- **`AppTheme(themeMode, content)`** computes `darkTheme` from the mode (`isSystemInDarkTheme()` for System) and asks a platform hook for a color scheme. The `dynamicColor` flag is dropped in favor of the hook.
- **Dynamic color via platform hook:** common declares `expect @Composable fun platformColorScheme(darkTheme: Boolean): ColorScheme?`; Android (`src@android`) returns the dynamic scheme on API 31+ and null otherwise; other platforms return null. Falls back to `lightScheme`/`darkScheme`. Alternative: a parameter passed from `MainActivity`; rejected because the other apps would not need it and `AppTheme` is called from `App()` in common.
- **Fonts:** remove the Google Fonts provider and `font_certs.xml` usage; `AppTypography` is the Material baseline `Typography()`. Keeps the file in common code and avoids network font loading. Bundling Roboto as compose resources is a later option. The unused `values-v23/font_certs.xml` resource is removed if nothing else references it.
- **Icons are code-defined `ImageVector`s** (auto/contrast, sun, moon) in `shared`. Alternatives: `material-icons-extended` (large dependency, deprecated in Compose Multiplatform) and drawable resources (SVG is not supported by Android's resource loader). Code vectors work identically on every target.
- **Chooser UI:** an `IconButton` in the `CenterAlignedTopAppBar` actions showing the icon of the current mode, opening a `DropdownMenu` with three items (icon + text label), the current one highlighted. Chosen by the user over a segmented button row: it stays one small icon in the bar at any width, at the cost of two taps to switch. `Scaffold` provides insets.
- **Title:** centered via `CenterAlignedTopAppBar`.
- **Packages:** all theme and UI code moves to `com.usuallycoding.openadhd` (`.ui.theme` for theme). `App.kt` content moves into the `Scaffold` body.
- **Unused medium/high-contrast schemes** stay in `Theme.kt` untouched (private, unused, warnings only) so the author's work is not discarded.

## Risks / Trade-offs

- [Amper `expect`/`actual` across `src` and `src@android` is untested in this repo] → verify with a build of jvm and android first; fall back to an `interface`-based provider if it is a problem.
- [Menu items need visible selected state without a check icon] → highlight the current item's background.
- [Default fonts differ per platform] → accepted; custom fonts are a follow-up.
- [No tests exist for UI] → theme resolution is kept as a pure function (`ThemeMode` + system flag to dark boolean) so it can be unit tested in `shared/test`.
