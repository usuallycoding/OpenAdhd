# reward-templates Specification

## Purpose

Lets the user keep a library of rewards they want to earn, each placed in a tier that sets its price and tagged with the energy it takes, to be bought in the shop.

## Requirements

### Requirement: Create a reward template
The app SHALL let the user create a reward template with a non-empty name, an optional description, a tier from 1 to 5, an energy level of Low, Medium or High, and, for tier 5 only, a custom price.

#### Scenario: Valid reward
- **WHEN** the user enters the name "Movie night", picks tier 2 and energy Low, and saves
- **THEN** the reward appears in the reward list with tier 2, energy Low and a price of 50 coins

#### Scenario: Empty name
- **WHEN** the user tries to save a reward whose name is empty or only whitespace
- **THEN** the reward is not saved and the user is told a name is required

### Requirement: Price by tier
Rewards in tiers 1 to 4 SHALL cost a fixed price set by their tier in Settings: by default 20 coins for tier 1, 50 for tier 2, 120 for tier 3 and 300 for tier 4. A tier 5 reward SHALL cost the custom price the user entered, which MUST be a whole number larger than the tier 4 price.

#### Scenario: Fixed tier price
- **WHEN** a reward is saved in tier 3
- **THEN** its price is 120 coins

#### Scenario: Custom tier 5 price
- **WHEN** the user saves a tier 5 reward with a custom price of 500
- **THEN** its price is 500 coins

#### Scenario: Tier 5 price must exceed tier 4
- **WHEN** the user tries to save a tier 5 reward with a custom price of 300 or less
- **THEN** the reward is not saved and the user is told a tier 5 price must be larger than 300

#### Scenario: Smallest tier 5 price
- **WHEN** the user saves a tier 5 reward with a custom price of 301
- **THEN** the reward is saved with a price of 301 coins

#### Scenario: Tier 5 needs a price
- **WHEN** the user tries to save a tier 5 reward without entering a price
- **THEN** the reward is not saved and the user is told a price is required

#### Scenario: Changed tier price
- **WHEN** the tier 4 price in Settings is 400 and the user tries to save a tier 5 reward with a custom price of 400
- **THEN** the reward is not saved and the user is told a tier 5 price must be larger than 400

### Requirement: Edit a reward template
The app SHALL let the user change the name, description, tier, energy level and custom price of an existing reward, and the reward's price MUST follow its new tier.

#### Scenario: Change tier
- **WHEN** the user edits a reward from tier 2 to tier 3 and saves
- **THEN** the list shows the reward at tier 3 with a price of 120 coins

#### Scenario: Leaving tier 5
- **WHEN** the user edits a tier 5 reward with a custom price of 500 to tier 2 and saves
- **THEN** the reward costs 50 coins and no custom price is kept

#### Scenario: Edit does not alter past purchases
- **WHEN** a reward that has been redeemed is edited
- **THEN** the redemption history keeps the name and price the reward had when it was bought

### Requirement: Delete a reward template
The app SHALL let the user delete a reward template, and deleting MUST NOT remove redemptions or ledger entries that came from it.

#### Scenario: Delete a redeemed reward
- **WHEN** the user deletes a reward that has been redeemed before
- **THEN** the reward leaves the reward list and the shop, and the redemption history and the balance are unchanged

### Requirement: List reward templates
The app SHALL show all reward templates on the Rewards tab of the Templates screen, each with its tier, energy level and price.

#### Scenario: Tasks and rewards tabs
- **WHEN** the user opens the Templates screen
- **THEN** it offers a Tasks tab and a Rewards tab, and the Rewards tab lists the reward templates

#### Scenario: Empty library
- **WHEN** no reward templates exist
- **THEN** the Rewards tab shows an empty state that invites the user to create one

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

### Requirement: The create form can be opened on request
When another part of the app asks for a new reward template, the Rewards tab SHALL open its form to create one, exactly as if the user had pressed +, even when the Rewards tab was not on screen when the request was made. A request MUST open the form once and MUST NOT open it again later.

#### Scenario: Requested from the Shop
- **WHEN** the app shows the Templates screen on its Rewards tab because a new reward template was requested
- **THEN** the form to create a reward template is open

#### Scenario: Not opened twice
- **WHEN** the user closes that form and leaves and returns to the Rewards tab
- **THEN** the form is not opened again
