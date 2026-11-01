# Spec Delta

## MODIFIED Requirements

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

## ADDED Requirements

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
