# Design

## Context

See proposal.md for why and the specs for behavior. `DesktopReminderScheduler` (from `desktop-tray-and-login`) keeps a sorted queue of `(time, text)`, wakes every 20 seconds, calls `notify(text)` for a time at most 2 minutes late and drops older ones. It flattens each `UpcomingReminder` and so loses its `entryId` and `kind`. `supportsAlarms` is false, which hides the Notification/Alarm choice in the entry sheet and template form (both read `ReminderScheduler.supportsAlarms`). `entryRequests` already makes the main window scroll to and highlight a task, and ids that start with `template:` are planned reminders whose entry does not exist yet. `main.kt` owns the window, the tray and `windowVisible`.

## Goals / Non-Goals

**Goals:**
- A ring that cannot be lost to Focus: app audio plus an always-on-top window.
- All ring logic (rows, limits, wake handling) testable with a fake sound and virtual time.
- Everything outside `main.kt` is shared desktop code, so Windows and Linux inherit it.

**Non-Goals:**
- Waking a sleeping Mac, ringing over the lock screen, a separate alarm volume, a choice of sound, actions inside the notification (see proposal.md).

## Decisions

**1. The scheduler keeps each reminder's entry and kind, and hands due alarms to a ringer.**
The queue holds `(time, entryId, text, kind)`. In each tick the due items split by kind:
- notifications behave as now: shown if at most the 2-minute grace late, else dropped;
- alarms are grouped by `entryId`, and each group produces one ring for its latest due time, with that time attached when it is more than the grace late (the "Due 07:30" label). Earlier due times of the group are dropped.
`supportsAlarms` becomes true. Because the queue only ever holds times that were still in the future when `replaceAll` ran, an alarm that passed before the app started never enters it, which gives "started after the time" for free. A due alarm still calls the existing `notify`, which is the companion notification.
Alternative: have the coordinator pass past alarm times too. Rejected: it would change a shared interface for every platform to serve one.

**2. `DesktopAlarmRinger` owns the rows; the UI and the sound observe it.**
It holds `rows: StateFlow<List<AlarmRow>>` (entry id, text, started-at, due-at or null, the day it rang) and has `ring`, `dismiss(entryId)`, `dismissAll()` and `finished(ids)`. A row per entry id: a ring for an id already ringing replaces its row and restarts its clock. One `AlarmSound` is started when the list goes from empty to non-empty and stopped when it goes back, so stacked alarms are one sound. Expiry uses the wall clock on a 5-second check while any row exists, not one long `delay` per row, for the same reason as the scheduler: the monotonic clock does not count sleep, and a row must not outlive its 10 minutes just because the computer slept. The clock, check interval and sound are constructor parameters so tests drive virtual time with a fake sound. The row's day is taken when it starts, so a ring that crosses midnight still watches its own day's entries.

**3. The sound is synthesized in code, behind `AlarmSound`.**
`ToneAlarmSound` builds one short pattern once (three beeps of about 0.15 s and a pause, around 1.5 s in all, mono 16-bit PCM) and plays it with a `javax.sound.sampled` `Clip` looped continuously. Starting is wrapped so any failure (no mixer, no device, headless) leaves the ring silent but intact, as the spec requires. Alternatives: the macOS `/System/Library/Sounds/*.aiff` files (nicer, but macOS-only and not guaranteed to exist), a bundled sound file (an asset to license and ship), and shelling out to `afplay` (a process to track and kill). The synthesized pattern is the same on every desktop and has no files; the tone is easy to change later.

**4. The card is a shared composable in its own always-on-top window.**
`AlarmCard(rows, onOpenTask, onDismiss)` lives in shared JVM code so it can be tested with Compose UI tests. `main.kt` shows it in a separate `Window` while `rows` is non-empty, with `alwaysOnTop = true`, `resizable = false`, centred, and `onCloseRequest = dismissAll`. It is independent of the main window's `visible`, so it appears with the window hidden in the tray. The window is `focusable = false` so a ring that appears while the user is typing elsewhere cannot take their keystrokes or be dismissed by a stray Enter; mouse clicks still reach its buttons. If a non-focusable window turns out not to receive clicks on macOS, the fallback is a focusable window with no default button (checked in the tasks on the packaged app).

**5. The Dock bounce is `Taskbar.requestUserAttention`.**
When the first row appears, `main.kt` calls `requestUserAttention(true, true)` if the platform supports `Taskbar.Feature.USER_ATTENTION`, which bounces the macOS Dock icon until the app is activated, and cancels it with `requestUserAttention(false, true)` when the last row ends. It is a no-op where unsupported. The app is deliberately not brought to the foreground, which would steal focus and, with the main window hidden, show nothing anyway.

**6. Open task shows the main window through one function that always fronts it.**
`main.kt` gets `showMainWindow()`: sets `windowVisible = true` and bumps a counter that the existing `LaunchedEffect` also keys on, so the window comes to the front even when it was already visible but covered. Open task calls `entryRequests.open(entryId)`, `showMainWindow()` and `dismiss(entryId)`. For a `template:` id or a deleted task the existing "tapped task that cannot be shown" behavior applies unchanged, because it is the same request path as on Android and iOS. Open from the Dock, the tray icon and the menu call `showMainWindow()` and do not touch the ringer.

**7. A finished task ends its row through a watcher on the store.**
The scheduler only ever sees future times, so it cannot tell "no more repeats" from "completed". A small watcher in `DesktopContainer` combines `store.entriesFor(day)` for the days of the current rows and calls `finished` with the ids that are completed or no longer in the list. Ids that start with `template:` are skipped, since their entry does not exist yet and absence means nothing. It runs only while rows exist.

**8. Entry sheet and template form need no new UI.**
Both already show Notification/Alarm when `supportsAlarms` is true, with the helper line "An alarm rings where your device supports it; otherwise it is shown as a notification", which stays accurate. `ensureAlarmPermission` and `fullScreenAlarmsAllowed` keep their defaults (nothing is asked, nothing is warned about). The work is the spec change, tests, and the README line.

## Risks / Trade-offs

- [A store change in the 20 seconds after waking, before the first tick, rebuilds the queue and drops a missed alarm that would have rung] → Narrow and rare; flushing the old queue first was rejected because it would ring for a task just completed. Accept, and note it in the archive if it ever matters.
- [A Mac that is muted gives a silent ring] → The card, the Dock bounce and the notification remain. No separate alarm volume exists on macOS.
- [A full-screen app or a locked screen hides the card] → Sound and Dock bounce still work; the card is there on return. Accepted in proposal.md.
- [Focus and Do Not Disturb still silence the companion notification] → Intended: the ring is the part that gets through.
- [`focusable = false` might not deliver mouse clicks on macOS] → Verified on the packaged app in the tasks, with a focusable no-default-button window as the fallback.
- [A very long sleep wakes into many missed alarms] → One row per task, so the card is as long as the number of tasks with missed alarms; not capped, since a missed task should not vanish.
- [`Clip` can fail to open on some Java sound setups] → Caught and logged; the ring continues silently (spec requirement).

## Migration Plan

None: no data changes. Alarms already saved from an Android or iOS backup were notifications on the desktop and now ring. A rollback to the previous build shows them as notifications again.

## Open Questions

- The exact pitch and rhythm of the tone. It is cosmetic and does not change the specs.
