# Spec Delta

## ADDED Requirements

### Requirement: Search the reward templates
The Rewards tab SHALL have a search field above the list whenever at least one reward exists. While it holds text, the list SHALL show only the rewards whose name or description contains that text, ignoring upper and lower case and leading and trailing spaces, in the list's order. With the field empty every reward is listed. The field SHALL offer a clear control while it holds text and MUST NOT take focus by itself.

#### Scenario: Filter by name
- **WHEN** the rewards are "Movie night", "Tea" and "New headphones" and the user types "tea"
- **THEN** only "Tea" is listed

#### Scenario: Filter by description
- **WHEN** a reward named "Movie night" has the description "with snacks" and the user types "snacks"
- **THEN** "Movie night" is listed

#### Scenario: Ignores case and outer spaces
- **WHEN** the user types "  TEA " and a reward is named "Tea"
- **THEN** "Tea" is listed

#### Scenario: Empty field lists everything
- **WHEN** the user clears the search text
- **THEN** every reward is listed again

#### Scenario: Clear control
- **WHEN** the search field holds text
- **THEN** it offers a clear control that empties the field, and offers none while the field is empty

#### Scenario: Not focused on opening
- **WHEN** the user opens the Rewards tab
- **THEN** the search field is not focused and the keyboard is not shown

#### Scenario: No rewards
- **WHEN** no reward exists
- **THEN** the tab shows no search field and keeps its message about adding the first one

### Requirement: Nothing matches the reward search
When the search leaves no reward, the Rewards tab SHALL say so in place of the list, naming the text, and MUST NOT show the message for a tab without rewards.

#### Scenario: No match
- **WHEN** no reward matches the typed text "xyz"
- **THEN** the tab shows "No reward matches xyz" in place of the list

### Requirement: The reward search is kept and cleared
The text of the Rewards search SHALL be kept while the app runs, including when the user goes to the Tasks tab or another screen and comes back, and MUST NOT be saved. Saving a new reward SHALL clear it, so the new reward is not hidden. Editing or deleting a reward MUST leave it as it is.

#### Scenario: Kept across tabs
- **WHEN** the user types "tea" on the Rewards tab, swipes to Tasks and back
- **THEN** the search still holds "tea" and the list is still filtered

#### Scenario: Not saved
- **WHEN** the app is restarted
- **THEN** the search is empty

#### Scenario: Creating clears it
- **WHEN** the search holds "tea" and the user saves a new reward named "Cake"
- **THEN** the search is cleared and "Cake" is listed

#### Scenario: Editing keeps it
- **WHEN** the search holds "tea" and the user edits "Tea"
- **THEN** the search still holds "tea"

### Requirement: The reward search keyboard can be put away
While the reward search field has focus, the on-screen keyboard SHALL be hidden and the field SHALL lose focus when the user taps anywhere on the list area outside the field, scrolls the list, or presses the keyboard's Done key. The text stays as typed.

#### Scenario: Tap outside
- **WHEN** the keyboard is shown for the reward search and the user taps an empty part of the list area or on a row
- **THEN** the keyboard is hidden and the field is no longer focused

#### Scenario: Scrolling
- **WHEN** the keyboard is shown for the reward search and the user scrolls the list
- **THEN** the keyboard is hidden

#### Scenario: Done key
- **WHEN** the user presses Done on the keyboard
- **THEN** the keyboard is hidden and the search text is unchanged

#### Scenario: Tapping the field
- **WHEN** the user taps the search field
- **THEN** it takes focus and the keyboard is shown
