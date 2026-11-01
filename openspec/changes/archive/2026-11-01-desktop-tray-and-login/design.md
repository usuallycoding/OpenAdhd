# Design

## Context

See proposal.md for why. Today `desktopApp/src/main.kt` opens one `Window(onCloseRequest = ::exitApplication)` and creates the container inside `application {}`. `DesktopReminderScheduler` keeps a coroutine that `delay`s until each time in turn, and `DesktopNotifier` adds an AWT `TrayIcon` only while something is pending (`onPending`) and shows notifications with `displayMessage`, falling back to an in-app message through the container's `reminderNotices` flow. Settings rows are all `SettingKey` entries stored in the database; none of them reflects state owned by the operating system.

## Goals / Non-Goals

**Goals:**
- Reminders keep firing after the window is closed, on every desktop OS that has a tray, with no new dependencies.
- Launch at login on macOS whose state is the system's, not a copy that can drift.
- Timers that survive the computer sleeping.

**Non-Goals:**
- A desktop Alarm kind, Windows/Linux login items, and OS-level scheduling while quit (see proposal.md).
- A new process or helper. The one running app stays the only thing that fires reminders.

## Decisions

**1. The window's visibility becomes state; the application, not the window, owns the lifecycle.**
`application { }` gets a `var windowVisible by mutableStateOf(...)` and `Window(visible = windowVisible, onCloseRequest = { hide })`. The container is created once at the `application` level and is unaffected by the window being hidden. Alternatives: removing the `Window` from the composition when hidden frees memory but throws away UI state (selected tab, scroll, open sheets); with `visible = false` the state survives and Open restores it exactly. Memory is accepted as the cost.

**2. Whether to hide or quit on close depends on `SystemTray.isSupported()`.**
With a tray, close hides; without one, close calls `exitApplication` as today. This keeps a hidden app from becoming unreachable. The same check drives the first-close notice, which is only shown when closing actually hid the window.

**3. The tray comes from Compose's `Tray` composable, not the hand-built AWT `TrayIcon`.**
It lives for the whole `application`, supports a menu (Open, Quit) and a primary action (click), and `rememberTrayState().sendNotification` shows notifications. `DesktopNotifier` stops creating and removing a tray icon, so `setPending` and the scheduler's `onPending` callback are deleted; the notifier becomes a thin sink that sends to the tray state, or to the in-app fallback when `isTraySupported` is false. Alternative: keep the AWT icon and add a `PopupMenu`; rejected because it duplicates what the composable already does and keeps lifecycle code outside `application`.

**4. The scheduler ticks on the wall clock.**
`DesktopReminderScheduler` keeps the sorted queue but a loop wakes every 20 seconds, takes every queued time that is now at or before `clock.now()`, and shows it only if it is no more than 2 minutes late (the grace period); older ones are dropped without being shown. This fixes sleep, because `delay` runs on the monotonic clock which does not advance during sleep on macOS, so one long `delay` would fire late by the sleep length. 20 seconds keeps the worst on-time lateness under that and costs one wake-up per 20 seconds. Alternatives: a wake-from-sleep OS hook (platform-specific, rejected); re-arming `delay` for the remaining time on each tick (equivalent to the tick but more code). `replaceAll` keeps its contract and still resets the queue, so a replaced schedule needs no extra handling. The tick interval, grace and clock are constructor parameters so tests drive virtual time as they do now.

**5. Launch at login is a LaunchAgent the app writes itself, and the file is the source of truth.**
A small `LoginItem` seam in shared JVM code (`isAvailable`, `isEnabled()`, `setEnabled(Boolean)`) with a macOS implementation that writes `~/Library/LaunchAgents/com.usuallycoding.openadhd.plist` with `RunAtLoad`, `ProgramArguments` = the packaged launcher plus `--background`, and `LimitLoadToSessionType` `Aqua`. `isEnabled()` reads whether that file exists and points at the current launcher, so removing it elsewhere turns the switch off. The launcher path comes from the `jpackage.app-path` system property that jpackage sets for the packaged app; when it is absent (running from the build tool), `isAvailable` is false and the row is not listed. The `--background` argument makes `main` start with `windowVisible = false`; with no tray it is ignored and the window is shown.
Alternatives: `SMAppService` (needs a native bridge and signing), AppleScript login items through `osascript` (prompts for automation permission and is harder to read back), and a `Library/LoginItems` alias (deprecated). Writing the plist needs no permission, is readable, and is undone by deleting the file. It is not removed by uninstalling the app, which is acknowledged under Risks. Nothing is loaded into launchd by the app: the agent runs at the next login, which is all the spec asks for, so there is no `launchctl` call to get wrong.

**6. The Settings row is not a `SettingKey`.**
`SettingKey` is the database-backed list used by backup, Restore and the defaults requirement. The login item gets its own row, shown in a Startup section between Reminders and Data, backed by `LoginItem` handed to `SettingsViewModel` as an optional dependency (null elsewhere, like `files`). The row is a switch, since it is on or off, and it reads `isEnabled()` whenever Settings is shown so it follows the system. Backup and Delete all data do not touch it because they only touch the store.

**7. Showing the window from outside Compose.**
Open (menu and icon click) sets `windowVisible = true` and brings the window to the front; on macOS a Dock click arrives through `Desktop.getDesktop().addAppEventListener(AppReopenedListener)`. A click on a notification is wired through the tray icon's action where the system reports it, as best effort (see Risks). All paths only set the visibility; none touches the store.

**8. The first-close notice is remembered in a file next to the database.**
An empty marker file in `~/.openadhd/` is created when the notice is shown, and checked before showing it again. This keeps it out of the settings table (so a backup or Delete all data neither carries nor resets it, and a restore on another computer does not suppress the notice there) and needs no new store API. It is sent as a tray notification, or skipped if the system cannot show one.

## Risks / Trade-offs

- [macOS notifications from `displayMessage` may not report clicks, and may be attributed to a generic sender in some JDK builds] → The click-to-open requirement is worded as "where the system tells the app", and the tray icon and Dock click remain the dependable ways to reopen. Verify attribution and click behavior on the packaged `.app` during implementation; if clicks never arrive, say so in the archive notes instead of promising it.
- [A resident JVM holds roughly 150-300 MB while hidden] → Accepted for now. Revisit by disposing the window composition if it proves too heavy.
- [Tray support on Linux is uneven (GNOME needs an extension)] → `isSupported` gates the whole behavior and close then quits, so nothing becomes unreachable.
- [The LaunchAgent stays behind if the user deletes the app without turning the switch off, and launches nothing at login] → Harmless because the launcher path no longer exists; `isEnabled()` also checks that the path in the file is the current launcher, so a moved app shows off until turned on again.
- [Moving the app after enabling it leaves the agent pointing at the old path] → Same check as above; turning the switch on again rewrites the path.
- [Two copies running (the login-started one plus a manual start) would both fire reminders and open the same database] → The macOS `.app` is single-instance by the system; running the jar by hand alongside it is a developer-only case and accepted.
- [A 20-second tick can show a reminder up to 20 seconds late] → Acceptable for reminders at minute resolution.

## Migration Plan

No data migration. Users get the new close behavior on update; the first-close notice explains it. Rolling back is shipping the previous build; a LaunchAgent left by the new build then starts the old build at login, harmlessly, with a visible window.

## Open Questions

- Whether the tray icon should be a monochrome template image on macOS instead of the colored app icon. It is cosmetic and does not change the specs.
