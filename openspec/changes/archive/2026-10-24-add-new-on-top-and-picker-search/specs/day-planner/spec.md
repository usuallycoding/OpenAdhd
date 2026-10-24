# Spec Delta

## ADDED Requirements

### Requirement: Search field in the Add to today picker
The Add to today picker SHALL have a search field above the task templates whenever at least one task template exists. The field MUST be focused, with the on-screen keyboard open, as soon as the picker opens, MUST start empty every time the picker opens, and SHALL offer a clear control while it holds text.

#### Scenario: Focus on open
- **WHEN** the user opens the Add to today picker with task templates
- **THEN** the search field is focused and the keyboard is open

#### Scenario: Starts empty each time
- **WHEN** the user types "dis", closes the picker and opens it again
- **THEN** the search field is empty and every template is listed

#### Scenario: Clear button
- **WHEN** the search field holds text
- **THEN** it offers a clear control that empties the field, and it offers none while the field is empty

#### Scenario: No templates
- **WHEN** no task template exists
- **THEN** the picker shows no search field and keeps its message about creating a template first

### Requirement: The picker lists the templates that match the search
While the search field holds text, the picker SHALL list only the task templates whose name or description contains that text, ignoring upper and lower case and leading and trailing spaces, in the task template order. With the field empty it SHALL list every task template. When none match it SHALL say so in place of the list. Tapping a listed template still adds it to today and closes the picker.

#### Scenario: Filter by name
- **WHEN** the templates are "Laundry", "Dishes" and "Taxes" and the user types "dis"
- **THEN** only "Dishes" is listed

#### Scenario: Ignores case
- **WHEN** the user types "LAUNDRY" and a template is named "Laundry"
- **THEN** "Laundry" is listed

#### Scenario: Filter by description
- **WHEN** a template named "Reset the flat" has the description "wash the dishes" and the user types "dishes"
- **THEN** "Reset the flat" is listed

#### Scenario: Keeps the template order
- **WHEN** two listed templates match the text
- **THEN** they appear in the task template order

#### Scenario: Empty field lists everything
- **WHEN** the user clears the search text
- **THEN** every task template is listed again

#### Scenario: Nothing matches
- **WHEN** no template matches the typed text "xyz"
- **THEN** the picker shows "No task matches xyz" in place of the list

#### Scenario: Picking from the results
- **WHEN** the user searches for "dis" and taps "Dishes"
- **THEN** "Dishes" is added to today and the picker closes
