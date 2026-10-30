## ADDED Requirements

### Requirement: Opening the app ends a ringing alarm
On Android, whenever the app comes to the front while an alarm rings, the ring SHALL stop, with its vibration, and the ringing notification and the alarm screen MUST go away, as with Dismiss. Only the ring ends: the next repeat of the reminder, if the task is still pending, SHALL ring as usual. An alarm that comes due while the app is already in front MUST still ring, and is ended by the user or by the app next coming to the front.

#### Scenario: Opened from the launcher
- **WHEN** an alarm rings with a heads-up banner only and the user opens the app from the launcher
- **THEN** the sound and vibration stop and the ringing notification is removed

#### Scenario: Opened from the recent apps
- **WHEN** an alarm rings and the user returns to the app from the recent apps
- **THEN** the ring stops

#### Scenario: Repeats still ring
- **WHEN** the user opens the app during a ring of a reminder that repeats every 30 minutes and its task is still pending
- **THEN** the next repeat rings

#### Scenario: No ring in progress
- **WHEN** the user opens the app while no alarm rings
- **THEN** nothing changes

#### Scenario: Already in front
- **WHEN** an alarm comes due while the app is in front
- **THEN** it rings and stays ringing until the user ends it or the app comes to the front again

## MODIFIED Requirements

### Requirement: The alarm screen
While an alarm rings, the app SHALL show an alarm screen above the lock screen with the task's name and two buttons, Open task and Dismiss. A notification with the same actions MUST be shown for as long as it rings. Tapping Open task, or the notification itself, SHALL stop the ring and open the app on Today with the task scrolled to and highlighted. Dismiss SHALL stop the ring and remove the notification, and MUST NOT cancel the alarm's later repeats. The notification MUST stay while it rings unless the user swipes it away where the system allows that, and swiping it away SHALL stop the ring like Dismiss.

#### Scenario: Open task
- **WHEN** the user taps Open task while an alarm rings
- **THEN** the ring stops and the app shows the task on Today, highlighted

#### Scenario: Tapping the notification
- **WHEN** the user taps the ringing notification
- **THEN** it behaves as Open task

#### Scenario: Dismiss
- **WHEN** the user taps Dismiss
- **THEN** the ring stops, the notification is removed and the alarm screen closes

#### Scenario: Dismiss keeps the repeats
- **WHEN** the user dismisses a ring of a reminder that repeats and the task is still pending
- **THEN** the next repeat still rings

#### Scenario: Swiped away
- **WHEN** the system lets the user swipe the ringing notification away and they do
- **THEN** the ring stops, the same as with Dismiss, and later repeats still ring

#### Scenario: Not swipeable
- **WHEN** the system does not let the user swipe the ringing notification away
- **THEN** it stays until the ring ends and the alarm is stopped with Dismiss, Open task, opening the app or the time limit
