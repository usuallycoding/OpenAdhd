# Spec Delta

## ADDED Requirements

### Requirement: Energy filter in the Add to today picker
Under the search field, whenever at least one task template exists, the picker SHALL show the chips Low, Medium and High. At most one chip is selected; tapping a chip selects it and tapping the selected chip clears it. While one is selected the picker SHALL list only the templates with that energy, together with the text search. No chip is selected each time the picker opens.

#### Scenario: Filter by energy
- **WHEN** the templates are "Laundry" (Low), "Taxes" (High) and "Dishes" (Low) and the user selects Low
- **THEN** only "Laundry" and "Dishes" are listed

#### Scenario: Combined with the text
- **WHEN** the user selects Low and types "dis"
- **THEN** only "Dishes" is listed

#### Scenario: Clearing the filter
- **WHEN** the user taps the selected Low chip again
- **THEN** no chip is selected and every template matching the text is listed

#### Scenario: One at a time
- **WHEN** Low is selected and the user taps High
- **THEN** only High is selected and only High templates are listed

#### Scenario: Starts cleared
- **WHEN** the user selects High, closes the picker and opens it again
- **THEN** no chip is selected and every template is listed

#### Scenario: No templates
- **WHEN** no task template exists
- **THEN** the picker shows no energy chips

### Requirement: Nothing matches the filter
When the text search, the energy filter or both leave no template, the picker SHALL say so in place of the list, naming the energy filter when one is selected.

#### Scenario: Energy and text
- **WHEN** Low is selected, the user types "dis" and no Low template matches
- **THEN** the picker shows "No Low energy task matches dis"

#### Scenario: Energy only
- **WHEN** High is selected, the text is empty and no template has High energy
- **THEN** the picker shows "No High energy task"

### Requirement: Search and filters stay in view
The search field and the energy chips SHALL stay fixed at the top of the picker while the list of templates scrolls beneath them, so they are reachable however far the list has been scrolled.

#### Scenario: Scrolled list
- **WHEN** the list is longer than the sheet and the user scrolls it to the bottom
- **THEN** the search field and the energy chips are still visible and usable

#### Scenario: Typing after scrolling
- **WHEN** the user has scrolled the list down and then types in the search field
- **THEN** the list updates without the field having moved
