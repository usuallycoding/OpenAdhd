# Design

## Context

`ReminderCoordinator.run()` follows `today`, `store.entriesFor(today)` and the stop hour, turns the pending entries into `UpcomingReminder`s with `ReminderRules.upcoming`, and hands the whole list to `ReminderScheduler.replaceAll`. Android arms it with `AlarmManager`, iOS with local notifications and AlarmKit, both keeping the soonest times only (`AlarmPlan`: 20 alarms, 100 notifications on Android; 20 alarms and 64 pending requests on iOS). The entry that carries a template's reminder is created by `ScheduledEntries` through `store.generateDue`, which runs only while the app runs, so nothing is armed for a day until the app has been open on it. `TaskTemplate` has `schedule.isDue(date)`, an optional `TemplateReminder` (minute of day, repeat, kind) and `lastGenerated`, set when the entry for that day was created. A tapped reminder whose entry is not on Today already opens Today with nothing highlighted.

## Goals / Non-Goals

**Goals:**
- Template reminders and alarms fire on the days the app is not opened, on Android and iOS.
- No platform code changes: the shared coordinator hands over a longer list.

**Non-Goals:**
- A background refresh on Android (a later change), desktop delivery while closed, or generating entries ahead of time.

## Decisions

- **Plan from templates, not from entries.** A pure function in the reminder rules, say `TemplateReminders.plan(templates, today, days, stopHour, now, zone)`, returns an `UpcomingReminder` for each day in `today .. today + 6` on which a template with a reminder `isDue` and `lastGenerated != day`. Its times come from `ReminderRules.upcoming(Reminder(ReminderRules.at(day, hour, minute, zone), repeat, kind), day, stopHour, now, zone)`, so the stop time and past-time rules are the existing ones. Alternative: generate the entries ahead of time; rejected because entries on future days would show in History and the planner, break "Today shows only today's entries", and need undoing when a template changes.
- **Real entries win.** `lastGenerated == day` means the entry exists (or was deleted by the user), so no planned reminder is made for that day; the entry's own reminder, edited, completed or removed, is the only one. Because `replaceAll` replaces the whole list, the planned reminder is gone as soon as the entry is generated and its own takes over, so nothing fires twice.
- **Synthetic entry id.** A planned reminder uses the id `template:<templateId>:<date>` and the template's name as text. No entry has that id, so a tap falls under "A tapped task that cannot be shown" and opens Today. Ids contain only characters the platforms already encode in their request codes and URIs; a test checks the Android and iOS encodings of such an id round trip.
- **Horizon of 7 days (today plus 6).** The platform caps decide how much is armed, and `AlarmPlan` already arms the soonest first, so today's reminders always outrank later days'. A longer horizon would only be cut by the caps. The constant sits next to the planner.
- **Coordinator inputs.** `run()` also combines `store.templates`, so editing a template, its schedule or its reminder re-plans immediately, and a changed stop hour or day does too. The existing `distinctUntilChanged` still prevents handing over an unchanged list.
- **No change to generation.** `ScheduledEntries` keeps creating entries when the app starts, the day changes or templates change.

## Risks / Trade-offs

- [A planned reminder cannot know the task was done elsewhere, such as by editing the data on another device] → There is only one device and the entry for a day replaces the plan once the app generates it; a day's repeats keep firing until then, as for any armed reminder.
- [Repeating reminders use many slots (every 30 minutes until 21:00 is about 25 a day), so only 2-4 days fit under the caps] → Soonest-first keeps the near days; the rest is armed the next time the app opens, and an Android background refresh is the planned follow-up.
- [The plan is only refreshed while the app runs, so after a week without opening it nothing is left] → Accepted for this change.
- [A reminder for an entry the user would have deleted that morning still fires] → Only possible before the app has generated the entry; deleting after generation stops it.
- [Synthetic ids reach platform code that expects entry ids] → Covered by the request-code and URI test above, and by the existing spec for taps that match no entry.
