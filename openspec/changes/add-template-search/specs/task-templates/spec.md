# Spec Delta

## ADDED Requirements

### Requirement: Search the task templates
The Tasks tab SHALL have a search field above the list whenever at least one task template exists. While it holds text, the list SHALL show only the templates whose name or description contains that text, ignoring upper and lower case and leading and trailing spaces, in the list's order. With the field empty every template is listed. The field SHALL offer a clear control while it holds text and MUST NOT take focus by itself.

#### Scenario: Filter by name
- **WHEN** the templates are "Laundry", "Dishes" and "Taxes" and the user types "dis"
- **THEN** only "Dishes" is listed

#### Scenario: Filter by description
- **WHEN** a template named "Reset the flat" has the description "wash the dishes" and the user types "dishes"
- **THEN** "Reset the flat" is listed

#### Scenario: Ignores case and outer spaces
- **WHEN** the user types "  LAUNDRY " and a template is named "Laundry"
- **THEN** "Laundry" is listed

#### Scenario: Empty field lists everything
- **WHEN** the user clears the search text
- **THEN** every task template is listed again

#### Scenario: Clear control
- **WHEN** the search field holds text
- **THEN** it offers a clear control that empties the field, and offers none while the field is empty

#### Scenario: Not focused on opening
- **WHEN** the user opens the Tasks tab
- **THEN** the search field is not focused and the keyboard is not shown

#### Scenario: No templates
- **WHEN** no task template exists
- **THEN** the tab shows no search field and keeps its message about creating the first one

### Requirement: Nothing matches the task search
When the search leaves no task template, the Tasks tab SHALL say so in place of the list, naming the text, and MUST NOT show the message for a tab without templates.

#### Scenario: No match
- **WHEN** no template matches the typed text "xyz"
- **THEN** the tab shows "No task matches xyz" in place of the list

### Requirement: The task search is kept and cleared
The text of the Tasks search SHALL be kept while the app runs, including when the user goes to the Rewards tab or another screen and comes back, and MUST NOT be saved. Saving a new task template SHALL clear it, so the new template is not hidden. Editing or deleting a template MUST leave it as it is.

#### Scenario: Kept across tabs
- **WHEN** the user types "dis" on the Tasks tab, swipes to Rewards and back
- **THEN** the search still holds "dis" and the list is still filtered

#### Scenario: Not saved
- **WHEN** the app is restarted
- **THEN** the search is empty

#### Scenario: Creating clears it
- **WHEN** the search holds "dis" and the user saves a new template named "Taxes"
- **THEN** the search is cleared and "Taxes" is listed

#### Scenario: Editing keeps it
- **WHEN** the search holds "dis" and the user edits "Dishes"
- **THEN** the search still holds "dis"

### Requirement: The task search keyboard can be put away
While the task search field has focus, the on-screen keyboard SHALL be hidden and the field SHALL lose focus when the user taps anywhere on the list area outside the field, scrolls the list, or presses the keyboard's Done key. The text stays as typed.

#### Scenario: Tap outside
- **WHEN** the keyboard is shown for the task search and the user taps an empty part of the list area or on a row
- **THEN** the keyboard is hidden and the field is no longer focused

#### Scenario: Scrolling
- **WHEN** the keyboard is shown for the task search and the user scrolls the list
- **THEN** the keyboard is hidden

#### Scenario: Done key
- **WHEN** the user presses Done on the keyboard
- **THEN** the keyboard is hidden and the search text is unchanged

#### Scenario: Tapping the field
- **WHEN** the user taps the search field
- **THEN** it takes focus and the keyboard is shown
