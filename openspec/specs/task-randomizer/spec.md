# task-randomizer Specification

## Purpose

Takes the decision of what to do next away from the user: given how much energy they have right now, it suggests one task template that fits, which they can add to today, swap for another, or dismiss.

## Requirements

### Requirement: Pick one for me
The Today screen SHALL offer a "Pick one for me" action that starts a suggestion by asking for the user's current energy: Low, Medium or High. The action MUST be available whether or not today already has entries.

#### Scenario: Starting a suggestion
- **WHEN** the user taps "Pick one for me"
- **THEN** the app asks for their current energy with the options Low, Medium and High

#### Scenario: Available on a busy day
- **WHEN** today already has entries
- **THEN** the "Pick one for me" action is still offered

### Requirement: Which tasks can be suggested
A suggestion SHALL be a task template whose energy is not higher than the chosen energy. A template that already has an entry on today's list, pending or completed, MUST NOT be suggested. Reward templates and subtasks MUST NOT be suggested.

#### Scenario: Low energy
- **WHEN** the user chooses Low and the templates are one Low, one Medium and one High
- **THEN** only the Low template can be suggested

#### Scenario: Medium energy
- **WHEN** the user chooses Medium and the templates are one Low, one Medium and one High
- **THEN** only the Low and Medium templates can be suggested

#### Scenario: Already planned
- **WHEN** a template already has an entry on today's list, pending or completed
- **THEN** it is not suggested

### Requirement: Better fits are more likely
Among the templates that can be suggested, each SHALL be chosen with a chance proportional to a weight: 4 when its energy equals the chosen energy, 2 when it is one level below and 1 when it is two levels below.

#### Scenario: High energy day
- **WHEN** the user chooses High and the templates are one High, one Medium and one Low
- **THEN** they are suggested with chances of 4 in 7, 2 in 7 and 1 in 7

#### Scenario: Medium energy day
- **WHEN** the user chooses Medium and the templates are one Medium and one Low
- **THEN** the Medium template is suggested with a chance of 2 in 3 and the Low one with 1 in 3

#### Scenario: Equal fits
- **WHEN** two eligible templates have the same energy
- **THEN** they have the same chance

### Requirement: The suggestion
The suggestion SHALL show the template's name, description when it has one, tier, energy level, the coins it pays, and the number of subtasks when it has any. Nothing MUST be added to today until the user accepts.

#### Scenario: Suggestion details
- **WHEN** the app suggests "Clean the kitchen", a tier 3 template with 3 subtasks and a description
- **THEN** the suggestion shows its name, description, tier 3, its energy, 20 coins and that it has 3 subtasks

#### Scenario: Nothing is added by suggesting
- **WHEN** a suggestion is shown
- **THEN** today's list is unchanged until the user accepts

### Requirement: Accepting a suggestion
Accepting a suggestion SHALL add the template to today exactly as choosing it from the template list does, with its rewards frozen at that moment, and close the suggestion.

#### Scenario: Accept
- **WHEN** the user taps "Add to today" on a suggestion
- **THEN** a new entry for that template appears on today's list with its description, subtasks and rewards, and the suggestion closes

### Requirement: Asking for another
"Another" SHALL show a different eligible template. A template MUST NOT be suggested again until every eligible template has been shown once for the chosen energy, and choosing the energy again MUST start over.

#### Scenario: No repeats
- **WHEN** three templates are eligible and the user taps "Another" twice
- **THEN** three different templates have been shown

#### Scenario: Everything has been shown
- **WHEN** the user taps "Another" after every eligible template has been shown
- **THEN** the app says that was everything that fits their energy and offers to choose a different energy, and shows no suggestion

#### Scenario: Choosing a different energy
- **WHEN** the user chooses another energy after seeing suggestions
- **THEN** the shown templates are forgotten and the new energy's eligible templates can be suggested again

### Requirement: Dismissing
"Not now" SHALL close the suggestion without adding anything or spending anything.

#### Scenario: Not now
- **WHEN** the user taps "Not now"
- **THEN** the suggestion closes and today's list and the balance are unchanged

### Requirement: When there is nothing to suggest
If there are no task templates, or none is eligible for the chosen energy, the app SHALL say so in plain words and MUST NOT show a suggestion. With no templates at all it SHALL point the user to creating one.

#### Scenario: No templates
- **WHEN** the user starts a suggestion and there are no task templates
- **THEN** the app tells them to create a task template first

#### Scenario: Nothing fits
- **WHEN** the user chooses Low and no Low template is available
- **THEN** the app says nothing fits Low energy and offers to choose a different energy
