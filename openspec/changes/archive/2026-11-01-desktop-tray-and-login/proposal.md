# Proposal

## Why

Desktop reminders only fire while the window is open, because closing the window quits the app and its timer. A reminder app that stays silent once the window is closed defeats its purpose, and people with ADHD are exactly the ones who close the window and forget. The desktop app already has a tray icon and a notification path, so the missing piece is letting the app keep running in the background.

## What Changes

- The desktop app shows a tray icon for the whole session, with a menu offering Open and Quit. Clicking the icon, choosing Open, or (on macOS) clicking the Dock icon brings the window back.
- Closing the window hides it and the app keeps running, so reminders still fire. Quit in the tray menu is the only way to exit fully. **BREAKING** for the rule that desktop reminders are delivered only while the app is running: they are now delivered until the user chooses Quit.
- The first time the window is closed the app shows a one-time notice that OpenAdhd is still running in the tray, and where to quit it.
- Where the system has no tray, closing the window still quits the app, because a hidden app could not be reopened. Reminders there keep appearing as in-app messages while the app is open.
- Settings gets a **Launch at login** switch, macOS only, off by default, that starts the packaged app when the user logs in, with the window hidden and only the tray icon showing.
- Desktop reminder timing no longer relies on one long delay per reminder. The scheduler checks the wall clock on a short tick, so reminders still fire on time after the computer sleeps, and a reminder missed while asleep is not shown late beyond a short grace period.
- The macOS Dock icon stays while the window is hidden.

Out of scope: a desktop Alarm kind (a later change, so the rule that desktop offers no Alarm choice stays), launch at login on Windows and Linux, and scheduling reminders at the operating system level while the app is fully quit.

## Capabilities

### New Capabilities
- `desktop-background-running`: how the desktop app lives in the background: the tray icon and its menu, hiding instead of quitting on close, the one-time notice, the fallback without a tray, and starting hidden at login.

### Modified Capabilities
- `task-reminders`: "Delivery on each platform" (desktop reminders are delivered while the app runs in the background, not only while the window is open) and "When a reminder fires" (a reminder missed while the computer slept is shown only within a short grace period).
- `app-settings`: a Launch at login switch on macOS, kept outside the saved settings table and outside backups because its state belongs to the operating system.

## Impact

- `desktopApp/src/main.kt`: window lifecycle (hide on close, start hidden, Dock reopen), tray menu, first-close notice.
- `shared/src@jvm/DesktopReminderScheduler.kt` and `DesktopNotifier`: wall-clock tick with grace period, tray icon for the whole session, notification to window wiring.
- New macOS login item support (a LaunchAgent written by the app, pointing at the packaged `.app`), and a small seam so Settings can read and change it on platforms that have it.
- `shared/src/ui/screens/SettingsScreen.kt` and `SettingsViewModel`: the Launch at login row, shown only where supported.
- `scripts/package-mac.sh` may need no change, since the packaged app's launcher path is read at runtime; to be confirmed during implementation.
- No database change and no new dependencies expected.
