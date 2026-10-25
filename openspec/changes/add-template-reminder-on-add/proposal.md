# Proposal

## Why

A template can carry a reminder, but a task added to Today with the + button comes without it, so the same template behaves differently depending on how it reached Today. And a reminder set on a template can silently never show: the system asks for notification permission only when a reminder is saved on an entry, not when it is saved on a template.

## What Changes

- **Reminder on a hand-added task:** adding a template to Today with the + button gives the entry the template's reminder, built exactly as for a scheduled entry. Times already past are skipped, so a repeating reminder resumes at its next time. This replaces the earlier rule that a hand-added task has no reminder.
- **Permission when saving a template reminder:** saving a template that has a reminder time asks for notification permission if the platform needs it. A refusal still saves the template and is reported in a short message after the form closes. Saving without a reminder time, and starting the app, ask nothing.
- **README:** the line saying hand-added tasks do not carry the reminder is corrected.
- **Not in this change:** a different reminder per add, asking for permission when a schedule generates an entry, and changes to the reminder sheet on Today.

## Capabilities

### New Capabilities

None.

### Modified Capabilities
- `task-reminders`: the "Added by hand" rule becomes "a hand-added task carries the template's reminder", and saving a template reminder asks for notification permission.
- `task-templates`: the template form says when permission was refused after saving a reminder.

## Impact

- `shared` domain and data: `assign` builds the template's reminder for the entry (one shared builder with generation); no schema or backup change.
- `shared` UI: the templates view model receives the reminder scheduler and reports a refusal through the app's existing message bar; `App` wires both.
- Tests: assigning a template with and without a reminder, past times, permission asked or not and refusal still saving, in both stores.
- No platform-specific code and no new libraries.
