# Spec Delta

## ADDED Requirements

### Requirement: The picker's keyboard can be put away
While the Add to today picker's search field has focus, the on-screen keyboard SHALL be hidden and the field SHALL lose focus when the user presses the keyboard's Done key, scrolls the list of templates, or taps empty space or the no-match message in the list area. The search text and the energy filter stay as they are, and tapping a listed template still adds it to today.

#### Scenario: Done key
- **WHEN** the user presses Done on the keyboard in the picker
- **THEN** the keyboard is hidden and the search text is unchanged

#### Scenario: Scrolling the results
- **WHEN** the keyboard is shown in the picker and the user scrolls the list
- **THEN** the keyboard is hidden

#### Scenario: Tapping the no-match message
- **WHEN** the keyboard is shown, nothing matches the text, and the user taps the message in the list area
- **THEN** the keyboard is hidden

#### Scenario: Tapping a template
- **WHEN** the keyboard is shown and the user taps a listed template
- **THEN** the template is added to today and the picker closes

#### Scenario: Tapping the field
- **WHEN** the user taps the search field after putting the keyboard away
- **THEN** it takes focus and the keyboard is shown again
