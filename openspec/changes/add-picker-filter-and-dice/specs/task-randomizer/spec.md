# Spec Delta

## ADDED Requirements

### Requirement: Roll a template in the Add to today picker
Beside the energy chips the picker SHALL have a dice button that draws one template from those currently listed, all with the same chance, after the text search and the energy filter have applied. The list SHALL scroll so the drawn template is visible and highlight it for a short while. Rolling MUST NOT add, change or spend anything, and the template is added only when the user taps it.

#### Scenario: Roll from the shown list
- **WHEN** the picker lists "Laundry", "Dishes" and "Taxes" and the user taps the dice
- **THEN** one of the three is highlighted and scrolled into view, and nothing is added to today

#### Scenario: Roll respects the filters
- **WHEN** Low is selected, the text is "dis" and the picker lists only "Dishes"
- **THEN** rolling highlights "Dishes"

#### Scenario: Equal chances
- **WHEN** the picker lists two templates of different energies
- **THEN** each has the same chance of being drawn

#### Scenario: Add the roll
- **WHEN** the user rolls "Dishes" and taps it
- **THEN** "Dishes" is added to today and the picker closes

### Requirement: A roll avoids the previous one
When more than one template is listed, a roll SHALL NOT draw the template drawn by the previous roll in the same picker. With one template listed it draws that one. The memory of the previous roll ends when the picker closes.

#### Scenario: Roll again
- **WHEN** two templates are listed and the user rolls twice
- **THEN** the second roll highlights the other template

#### Scenario: Only one listed
- **WHEN** one template is listed and the user rolls twice
- **THEN** both rolls highlight it

#### Scenario: New picker
- **WHEN** the user rolls, closes the picker and opens it again
- **THEN** the first roll may draw any listed template

### Requirement: Nothing to roll in the picker
The dice SHALL be disabled while no template is listed, and the picker MUST show no dice when no task template exists.

#### Scenario: No match
- **WHEN** the search and filter leave no template
- **THEN** the dice is disabled

#### Scenario: No templates
- **WHEN** no task template exists
- **THEN** the picker shows no dice
