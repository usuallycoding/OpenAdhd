# Proposal

## Why

A new user who has no task templates or rewards is sent to another screen by a sentence of text ("Create one on the Templates tab first", "Add rewards on the Templates screen, in the Rewards tab") and has to find the right tab and the + button alone. At the same time Today's empty state repeats the + button with an "Add a task" button, so the same action is offered twice.

## What Changes

- Today's empty state no longer has an "Add a task" button, since the + button is always there. Its message becomes "Nothing planned for today. Tap + to add a task."
- The Add to today picker, when there are no task templates, gets a **Take me there** button next to its message. It closes the picker, shows the Templates screen on its Tasks tab and opens the form to create a task template, as if the user had tapped + there.
- The Shop's empty state (no rewards) gets a **Take me there** button under its message. It shows the Templates screen on its Rewards tab and opens the form to create a reward template.
- The shop message is shortened to "The shop is empty." because the button now does the pointing.
- Both Templates tabs open their create form when the app asks them to, even when the tab was not on screen yet.

Out of scope: returning the user to Today or the Shop after saving, shortcuts from any other empty state (History, search with no match), and any change to the forms themselves.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `day-planner`: the empty Today state (no button, new hint text), and a new requirement for the picker's Take me there button when there are no task templates.
- `reward-shop`: the empty shop state gets a Take me there button.
- `task-templates`: a new requirement that the Tasks tab opens its create form when asked to.
- `reward-templates`: a new requirement that the Rewards tab opens its create form when asked to.

## Impact

- `shared/src/ui/screens/TodayScreen.kt`: remove the empty-state button, change the text, add the button to the no-templates picker, take a new callback.
- `shared/src/ui/screens/ShopScreen.kt`: add the button and callback, shorten the message.
- `shared/src/ui/screens/TemplatesScreen.kt` and `RewardsScreen.kt`: open the create form when the ViewModel's create request is set, then clear it.
- `TemplatesViewModel` and `RewardTemplatesViewModel`: a create-request flag with a request and a consume function.
- `shared/src/App.kt`: the navigation (destination, tab, request) passed to Today and Shop as callbacks.
- No database, dependency or platform-specific changes. Existing tests that read the old texts need updating.
