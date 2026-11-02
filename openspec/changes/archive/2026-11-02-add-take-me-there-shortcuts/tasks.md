# Tasks

## 1. Create requests in the view models

- [x] 1.1 Add `createRequested`, `requestCreate()` and `consumeCreateRequest()` to `TemplatesViewModel` and `RewardTemplatesViewModel`, with tests in `shared/test` that a request is set, is consumed once and survives until consumed; verify with `./kotlin test` passing those tests
- [x] 1.2 Make `TemplatesContent` and `RewardsContent` open their create form when the request is set and then consume it (the Tasks and Rewards screens pass the flag and the consume function); verify by running the app, calling the request path from 2.x and seeing the form open once

## 2. Take me there

- [x] 2.1 Today: remove the empty state's "Add a task" button, change its text to "Nothing planned for today. Tap + to add a task.", and add a "Take me there" button to the picker when there are no task templates (closing the picker first), threading a new callback from `TodayScreen`; verify the texts and the button by running the app with no tasks and no templates, and that the button is absent once a template exists
- [x] 2.2 Shop: shorten the empty message to "The shop is empty.", add a "Take me there" button under it and thread a callback from `ShopScreen` to `ShelvesList`; verify by running the app with no rewards
- [x] 2.3 In `App`, pass the two callbacks: set the destination to Templates, scroll the Templates pager to page 0 (tasks) or 1 (rewards) and call `requestCreate()`; verify end to end on a fresh database that Today's picker opens the task form on the Tasks tab, the Shop opens the reward form on the Rewards tab, closing the form and returning to the tab does not reopen it, and Reorder mode is off afterwards

## 3. Integration check

- [x] 3.1 Run `./kotlin test` for the whole project and `./kotlin build` for desktop; verify both succeed, and check that no test or preview still expects the removed button or the old shop text (search the repo for "Add a task" and "Add rewards on the Templates screen")
