# Proposal

## Why

The Templates screen lists every task template and every reward in one long scrolling list each, and finding one means scrolling. The Add to today picker already has a search; the places where the lists are managed should have one too.

## What Changes

- **Search on the Tasks tab and on the Rewards tab:** a field at the top of each list that shows only the items whose name or description contains the text, ignoring upper and lower case and spaces around it, in the list's order. It is shown whenever the tab has at least one item, has a clear control while it holds text, and does not take focus by itself.
- **Nothing found:** a message such as "No task matches xyz" or "No reward matches xyz" in place of the list, apart from the message for a tab with no items at all.
- **Kept while the app runs:** each tab remembers its text when the user swipes to the other tab and back, and forgets it when the app restarts.
- **Creating clears it:** saving a new template or reward clears that tab's search, so the new item, which goes to the top, is not hidden by a filter it does not match. Editing and deleting keep the search.
- **Reorder and search do not mix:** the Reorder toggle is not offered while a tab's search holds text, and typing in the search turns Reorder off.
- **Not in this change:** energy, tier or other filters on these screens, search in the Shop or in History, and any change to the Add to today picker.

## Capabilities

### New Capabilities

None.

### Modified Capabilities
- `task-templates`: searching the Tasks tab.
- `reward-templates`: searching the Rewards tab.
- `reordering`: Reorder is unavailable while a search holds text.

## Impact

- `shared` domain: a matching function for rewards beside the one for tasks.
- `shared` UI: each of the two view models keeps the query, exposes the matching list and ends Reorder while searching; both list screens get the field, the clear control and the no-match message.
- Tests: the matching rules, the view model rules (query kept, cleared on create, Reorder off), and UI tests for both tabs.
- No storage, backup or platform code, and no new libraries.
