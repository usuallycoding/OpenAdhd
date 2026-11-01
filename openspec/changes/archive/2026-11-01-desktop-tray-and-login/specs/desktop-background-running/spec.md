# Spec Delta

## Purpose

Keeps the desktop app running in the background behind a tray icon, so reminders still reach the user after the window is closed, and lets the macOS app start itself at login.

## ADDED Requirements

### Requirement: Tray icon
Where the system has a tray, the desktop app SHALL show a tray icon from the moment it starts until it quits, whether the window is shown or hidden and whether or not reminders are pending. Its menu SHALL offer Open and Quit, and clicking the icon SHALL show the window.

#### Scenario: Icon while the window is open
- **WHEN** the desktop app is running with its window open and no reminders set
- **THEN** the tray icon is shown

#### Scenario: Menu
- **WHEN** the user opens the tray icon's menu
- **THEN** it offers Open and Quit

#### Scenario: Open
- **WHEN** the window is hidden and the user chooses Open or clicks the tray icon
- **THEN** the window is shown in front, on the screen it had before

### Requirement: Closing the window keeps the app running
Where the system has a tray, closing the window SHALL hide it and leave the app running, so reminders keep firing. Choosing Quit in the tray menu SHALL be the only way to exit, and it SHALL remove the tray icon and stop all reminders until the app is started again.

#### Scenario: Close
- **WHEN** the user closes the window
- **THEN** the window disappears, the tray icon stays and the app keeps running

#### Scenario: Reminder with the window closed
- **WHEN** a reminder is due while the window is hidden
- **THEN** the notification "Still to do" with the task's name is shown

#### Scenario: Quit
- **WHEN** the user chooses Quit in the tray menu
- **THEN** the app exits, the tray icon is removed and no later reminder is shown

### Requirement: Dock icon on macOS
On macOS the Dock icon SHALL stay while the window is hidden, and clicking it SHALL show the window.

#### Scenario: Click the Dock icon
- **WHEN** the window is hidden and the user clicks the app's Dock icon
- **THEN** the window is shown in front

### Requirement: First close notice
The first time the window is closed with the app staying in the tray, the app SHALL show a notice that OpenAdhd is still running in the tray and that Quit in the tray menu exits it. The notice MUST NOT be shown again on later closes or after the app is restarted.

#### Scenario: First close
- **WHEN** the user closes the window for the first time
- **THEN** a notice says OpenAdhd is still running in the tray and how to quit it

#### Scenario: Later closes
- **WHEN** the user closes the window again, or after restarting the app
- **THEN** no notice is shown

### Requirement: No tray available
Where the system has no tray, closing the window SHALL quit the app, because a hidden app could not be shown again. Reminders there SHALL appear as messages inside the app while it is open, as before.

#### Scenario: Close without a tray
- **WHEN** the system has no tray and the user closes the window
- **THEN** the app exits and no first-close notice is shown

#### Scenario: Reminder without a tray
- **WHEN** a reminder is due while the app is open on a system without a tray
- **THEN** an in-app message shows the task's name

### Requirement: Notification click shows the window
Where the system tells the app that a reminder notification was clicked, the app SHALL show the window. Nothing MUST be added, changed or spent by the click.

#### Scenario: Click on a reminder
- **WHEN** the window is hidden and the system reports a click on a reminder notification
- **THEN** the window is shown in front

### Requirement: Launch at login on macOS
On macOS, when the app is the packaged app, Settings SHALL offer a Launch at login switch that is off by default. Turning it on SHALL make the system start the app when the user logs in, with the window hidden and the tray icon shown, so reminders work without the user opening anything. Turning it off SHALL stop that. Starting the app any other way SHALL show the window.

#### Scenario: Start at login
- **WHEN** Launch at login is on and the user logs in
- **THEN** the app starts with no window and its tray icon shown

#### Scenario: Normal start
- **WHEN** the user starts the app by opening it
- **THEN** the window is shown

#### Scenario: Turn off
- **WHEN** Launch at login is on and the user turns it off
- **THEN** the app is not started at the next login

#### Scenario: Reminder after login
- **WHEN** Launch at login is on, the user logged in and never opened the window, and a reminder is due
- **THEN** its notification is shown

### Requirement: Launch at login follows the system
The Launch at login switch SHALL show whether the system will start the app at login, not what the app last saved, so removing the app from the login items outside the app turns the switch off.

#### Scenario: Removed in System Settings
- **WHEN** Launch at login was on and the user removes the app from the login items in System Settings
- **THEN** the switch shows off the next time Settings is opened

#### Scenario: Not the packaged app
- **WHEN** the app is run from a build tool and not as the packaged app
- **THEN** the Launch at login switch is not listed
