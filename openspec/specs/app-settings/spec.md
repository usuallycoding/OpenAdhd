# app-settings Specification

## Purpose

Lets the user tune the numbers that make up the game, such as the debt limit, coin payouts, reward prices, subtask limit and luck chances, and set a name, from a Settings tab with a default and a restore control for each value.

## Requirements

### Requirement: Settings tab
The app SHALL have a Settings destination in the bottom bar, after Shop, that lists every setting grouped as Appearance, Profile, Coins, Subtasks, Shop, Luck and Reminders, followed by a Data section and, at the very end, the version label. Each setting SHALL show its name, its current value and its default.

#### Scenario: Opening Settings
- **WHEN** the user taps Settings in the bottom bar
- **THEN** the screen lists the settings with their current values and defaults

#### Scenario: Nothing changed yet
- **WHEN** the user opens Settings on a fresh install
- **THEN** every setting shows its default value

#### Scenario: No reorder control
- **WHEN** the Settings destination is shown
- **THEN** the top bar offers no Reorder control

#### Scenario: Data section last
- **WHEN** the user scrolls to the end of the Settings list
- **THEN** the Data section is shown after the Reminders group, and only the version label follows it

#### Scenario: Appearance first
- **WHEN** the user opens Settings
- **THEN** the Appearance group is the first one listed

### Requirement: Settings and their defaults
The settings and their defaults SHALL be: theme, follow system theme; dynamic color, dynamic; name, empty; debt limit, 500 coins; subtask ceiling, 10; the coins paid by task tiers 1 to 4, 5, 10, 20 and 40; the Shop price of reward tiers 1 to 4, 20, 50, 120 and 300; critical hit chance, 1 in 8; critical hit multiplier, 2; roll upgrade chance, 1 in 6; the time reminders stop at, 22:00.

#### Scenario: Defaults match the original rules
- **WHEN** no setting has been changed
- **THEN** the debt limit, payouts, prices, subtask limit and chances behave as described in the other specs

#### Scenario: Stop time default
- **WHEN** no setting has been changed
- **THEN** "Stop reminders at" shows 22:00

#### Scenario: Theme default
- **WHEN** no setting has been changed
- **THEN** Theme shows "Follow system theme"

#### Scenario: Dynamic color default
- **WHEN** no setting has been changed
- **THEN** Dynamic color is on, where the platform offers it

### Requirement: Editing a setting
Tapping a setting SHALL open a bottom-sheet form with the current value, and saving a valid value SHALL apply it at once and keep it. A value that is not valid MUST NOT be saved, and the form MUST say what is wrong. Cancelling leaves the value as it was.

#### Scenario: Change the debt limit
- **WHEN** the user opens the debt limit, enters 200 and saves
- **THEN** the debt limit shows 200 and a purchase that would take the balance below minus 200 is refused

#### Scenario: Cancel
- **WHEN** the user opens a setting, changes the text and cancels
- **THEN** the setting keeps its previous value

#### Scenario: Not a number
- **WHEN** the user clears a number setting or types something that is not a whole number and tries to save
- **THEN** nothing is saved and the form says a whole number is needed

### Requirement: Restoring a default
Each setting SHALL have a Restore control that puts its default back and removes the saved override. The control MUST be disabled while the value already is the default. Restoring one setting MUST NOT change any other.

#### Scenario: Restore
- **WHEN** the debt limit is 200 and the user taps Restore on it
- **THEN** the debt limit shows 500, and the Restore control is disabled

#### Scenario: Already the default
- **WHEN** a setting holds its default value
- **THEN** its Restore control is disabled

#### Scenario: Others are untouched
- **WHEN** the user restores the debt limit while the subtask ceiling is 6
- **THEN** the subtask ceiling is still 6

#### Scenario: Saving the default value
- **WHEN** the user saves the value 500 for the debt limit
- **THEN** the setting counts as the default and its Restore control is disabled

### Requirement: Value limits
The name SHALL be trimmed and at most 40 characters long. The debt limit SHALL be a whole number from 0 to 100000. The subtask ceiling SHALL be from 1 to 50. A task tier payout SHALL be from 1 to 1000. A reward tier price SHALL be from 1 to 100000. The critical hit chance and the roll upgrade chance SHALL each be 1 in a whole number from 2 to 1000, and the critical hit multiplier a whole number from 2 to 10. The time reminders stop at SHALL be from 00:01 to 23:59.

#### Scenario: Debt limit of zero
- **WHEN** the user saves a debt limit of 0
- **THEN** it is saved, and no purchase may take the balance below zero

#### Scenario: Out of range
- **WHEN** the user tries to save a critical hit chance of 1 in 1
- **THEN** nothing is saved and the form says the chance must be between 2 and 1000

#### Scenario: Long name
- **WHEN** the user tries to save a name of 41 characters
- **THEN** nothing is saved and the form says the name can be at most 40 characters

#### Scenario: Stop hour out of range
- **WHEN** the user picks 00:00 as the time reminders stop at and saves
- **THEN** nothing is saved and the dialog says the time must be between 00:01 and 23:59

### Requirement: Tiers stay in order
Each task tier SHALL pay more than the tier below it, and each reward tier price SHALL be higher than the tier below it, so a value that would break that order MUST NOT be saved and the form MUST say which tier it has to stay above or below.

#### Scenario: Payout too low
- **WHEN** tier 2 pays 10 and the user tries to set tier 3 to 10
- **THEN** nothing is saved and the form says tier 3 must pay more than tier 2

#### Scenario: Price too high
- **WHEN** tier 3 costs 120 and the user tries to set tier 2 to 120
- **THEN** nothing is saved and the form says tier 2 must cost less than tier 3

#### Scenario: Move several tiers in turn
- **WHEN** tiers 1, 2 and 3 pay 5, 10 and 20 and the user wants tier 1 to pay 12
- **THEN** setting tier 1 to 12 is refused until tier 2 has been raised to 15 first, and then it succeeds

### Requirement: Changes that would break existing data are refused
A setting MUST NOT be saved if it would leave existing data invalid, and the form SHALL name the item that stops it. The price of tier 4 MUST stay below the price of every tier 5 reward. The coins paid by a task tier MUST NOT drop below the number of subtasks of any template of that tier.

#### Scenario: Tier 4 price and a tier 5 reward
- **WHEN** a tier 5 reward "Weekend away" costs 500 and the user tries to set the tier 4 price to 500
- **THEN** nothing is saved and the form says tier 4 must cost less than "Weekend away", which costs 500

#### Scenario: Payout and a template's subtasks
- **WHEN** a tier 2 template "Clean kitchen" has 8 subtasks and the user tries to set the tier 2 payout to 6
- **THEN** nothing is saved and the form says "Clean kitchen" has 8 subtasks and needs at least 8 coins

#### Scenario: Lowering the ceiling
- **WHEN** a template has 8 subtasks and the user sets the subtask ceiling to 5
- **THEN** it is saved, the template keeps its 8 subtasks, and saving that template again is refused until it has 5 or fewer

### Requirement: A change applies from then on
A changed setting SHALL affect only what happens afterwards. Today entries already assigned MUST keep the coins they were assigned with, ledger entries and redemptions MUST keep their amounts and prices, and the balance MUST NOT change when a setting changes. A reward's Shop price follows its tier, so a changed reward tier price SHALL apply at once to the rewards of that tier.

#### Scenario: Assigned entries keep their coins
- **WHEN** a tier 3 entry worth 20 coins is on Today and the user sets the tier 3 payout to 25
- **THEN** completing that entry still pays 20 coins, and a tier 3 entry assigned afterwards pays 25

#### Scenario: Shop price follows the tier
- **WHEN** the user sets the tier 2 price to 60
- **THEN** every tier 2 reward in the Shop costs 60, and earlier redemptions still show what they cost

#### Scenario: Balance is stable
- **WHEN** the user changes any setting
- **THEN** the coin balance and the ledger are unchanged

#### Scenario: Debt above a lowered limit
- **WHEN** the balance is -400 and the user sets the debt limit to 100
- **THEN** the balance stays -400, purchases are refused and the Shop says how many more coins are needed

### Requirement: Theme setting
The Appearance group SHALL have a Theme setting with three options, each with an icon and a text label: Follow system theme, Light and Dark. Tapping it SHALL open a bottom sheet listing the options with the current one highlighted. Choosing an option SHALL apply it to the whole app at once, close the sheet and keep the choice, and the top bar MUST NOT have a theme control.

#### Scenario: Choosing dark
- **WHEN** the user taps Theme, chooses Dark in the sheet
- **THEN** the sheet closes, the app switches to the dark appearance, and the Theme row shows Dark

#### Scenario: Current option highlighted
- **WHEN** the theme is Light and the user opens the sheet
- **THEN** Light is highlighted and the other two are not

#### Scenario: Restore
- **WHEN** the theme is Dark and the user taps Restore on the Theme row
- **THEN** the theme becomes Follow system theme and the Restore control is disabled

#### Scenario: Options are labelled
- **WHEN** a screen reader reads the options in the sheet
- **THEN** each option has a visible text label naming its mode

#### Scenario: No theme control in the top bar
- **WHEN** the app is shown on any screen
- **THEN** the top bar has no theme button

### Requirement: The theme is applied from the start
The saved theme SHALL be in use from the first frame the app draws after it starts, so there is no moment in which the app shows the wrong colours before switching to the saved theme.

#### Scenario: Saved dark on a light device
- **WHEN** the theme is saved as Dark, the device is set to light, and the app starts
- **THEN** the first screen is drawn in the dark appearance

#### Scenario: Saved name shown at once
- **WHEN** a name is saved and the app starts
- **THEN** the top bar shows the name's title on the first screen, not "OpenAdhd" first

### Requirement: Dynamic color setting
Settings SHALL offer a Dynamic color setting in the Appearance group, after Theme, with the choices "Dynamic (wallpaper colors)" and "App colors". The default MUST be Dynamic. It SHALL be listed only on platforms that offer dynamic color, which is Android 12 or newer, and MUST NOT be listed anywhere else. Tapping it SHALL open a bottom sheet with the two choices, the current one highlighted, and choosing one applies it and closes the sheet.

#### Scenario: Android 12 or newer
- **WHEN** the user opens Settings on Android 12 or newer
- **THEN** Appearance lists Theme and then Dynamic color, showing "Dynamic (wallpaper colors)" with that as its default

#### Scenario: Not supported
- **WHEN** the user opens Settings on desktop, iOS, or Android older than 12
- **THEN** Appearance lists Theme only

#### Scenario: Choose App colors
- **WHEN** the user opens Dynamic color and chooses "App colors"
- **THEN** the sheet closes, the setting shows "App colors", and the app switches to its own colors

#### Scenario: Restore
- **WHEN** Dynamic color is "App colors" and the user taps Restore on it
- **THEN** it shows "Dynamic (wallpaper colors)" and its Restore control is disabled

### Requirement: Launch at login setting
On macOS, when the app is the packaged app, Settings SHALL list a Launch at login switch in a Startup section placed after the Reminders group and before the Data section, off by default. It MUST NOT be listed anywhere else. It is not one of the saved settings: it MUST NOT be part of a backup, and Delete all data MUST NOT change it.

#### Scenario: On macOS
- **WHEN** the user opens Settings in the packaged macOS app
- **THEN** a Startup section after Reminders lists Launch at login, off

#### Scenario: Turn on
- **WHEN** the user turns Launch at login on
- **THEN** the switch shows on and the app starts at the next login

#### Scenario: Other platforms
- **WHEN** the user opens Settings on Android, iOS, Windows or Linux
- **THEN** there is no Startup section

#### Scenario: Backup and Delete all data
- **WHEN** Launch at login is on and the user exports a backup, or deletes all data
- **THEN** the backup does not mention it and the switch is still on afterwards

### Requirement: Version label
Settings SHALL end with a small, dimmed, centered line below the Data section that reads "OpenAdhd" and the app's version, such as "OpenAdhd 1.0.0". On Android and iOS the build number SHALL follow in parentheses, such as "OpenAdhd 1.0.0 (1)". The line MUST NOT be tappable and MUST NOT be a setting, so no backup carries it and Restore does not apply to it.

#### Scenario: On a phone
- **WHEN** the user scrolls to the end of Settings in version 1.0.0, build 1, on Android or iOS
- **THEN** the last line reads "OpenAdhd 1.0.0 (1)"

#### Scenario: On the desktop
- **WHEN** the user scrolls to the end of Settings in the packaged desktop app of version 1.0.0
- **THEN** the last line reads "OpenAdhd 1.0.0", with no build number

#### Scenario: Not a control
- **WHEN** the user taps the version line
- **THEN** nothing happens

#### Scenario: Backup
- **WHEN** the user exports a backup
- **THEN** the backup does not mention the version label

### Requirement: Where the version comes from
The label SHALL show the version of the installed app as its platform reports it: on Android its version name and version code, on iOS its short version string and bundle version, on the desktop the version of the packaged app. A desktop app that is not packaged, such as one run from the build tool, SHALL read "OpenAdhd dev". Every platform's first release is version 1.0.0, build 1.

#### Scenario: Android
- **WHEN** the Android app is installed from a build with version name 1.0.0 and version code 1
- **THEN** the label reads "OpenAdhd 1.0.0 (1)"

#### Scenario: iOS
- **WHEN** the iOS app is installed from a build with short version 1.0.0 and bundle version 1
- **THEN** the label reads "OpenAdhd 1.0.0 (1)"

#### Scenario: Packaged desktop app
- **WHEN** the desktop app is started from the package built with version 1.0.0
- **THEN** the label reads "OpenAdhd 1.0.0"

#### Scenario: Desktop run from the build tool
- **WHEN** the desktop app is started from the build tool and so has no packaged version
- **THEN** the label reads "OpenAdhd dev"

### Requirement: Stop reminders at setting
The Reminders group SHALL have a "Stop reminders at" setting that is a time of day with minutes. The row SHALL show it as hours and minutes on 24 hours, such as 22:30. Tapping it SHALL open a 24-hour clock picker with the current time, and saving a valid time SHALL apply it at once and keep it. A saved value that is only an hour, such as 22, MUST be read as that hour and 00 minutes, and the value written from now on SHALL be hours and minutes.

#### Scenario: Opening the clock
- **WHEN** the user taps "Stop reminders at"
- **THEN** a 24-hour clock picker opens showing the current stop time

#### Scenario: Choosing a time with minutes
- **WHEN** the user picks 22:30 in the clock and confirms
- **THEN** the row shows 22:30 and reminders stop at 22:30 from then on

#### Scenario: Cancelling
- **WHEN** the user opens the clock, changes the time and cancels
- **THEN** the row keeps its time

#### Scenario: An hour saved by an earlier version
- **WHEN** the stored value of the setting is 18, from an earlier version or from a backup of one
- **THEN** the row shows 18:00 and reminders stop at 18:00

#### Scenario: Restoring the default
- **WHEN** the stop time is 21:15 and the user taps Restore on it
- **THEN** it shows 22:00 again

#### Scenario: A value that is not a time
- **WHEN** the stored value of the setting is not a valid time, such as 24 or 25:00
- **THEN** it is ignored and the setting shows its default, 22:00
