# Tasks

## 1. First working alarm (spike)

- [x] 1.1 Add `IosAlarmBridge`, its item type and the `IosBridge` holder to `shared`, and make `createIosContainer` use `IosBridge.entryRequests` (verify: the iOS native build compiles and the existing tests still pass)
- [x] 1.2 Add the Swift side in `iosApp/src`: an AlarmKit bridge guarded by `#available(iOS 26, *)` that requests authorization and schedules a fixed-date alarm with a title and an Open task button, an `OpenTaskIntent` that hands the entry id to `IosBridge.entryRequests`, registering the bridge at start-up, and `NSAlarmKitUsageDescription` in `Info.plist` (verify: the iOS build compiles with no Widget Extension)
- [x] 1.3 Route alarm-kind times from `IosReminderScheduler` to the bridge through a pure routing function in `shared` (soonest 20 as alarms, everything else, and alarms that cannot be used, as notifications) and test it (verify: unit tests for available and authorized, unavailable, unauthorized, the limit of 20 with 25 alarm times, notifications not counted, and an empty schedule)
- [x] 1.4 Try it on an iPhone and record the findings in design.md under Spike findings: an alert-only alarm without a Widget Extension, ringing on the lock screen and on silent, the Open task button opening the task from a locked phone, the prompt and plist key (verify: manual; pause here for a decision if any of these fails)

## 2. Permission and fallbacks

- [x] 2.1 Add `ensureAlarmPermission()` to `ReminderScheduler` and the test fake, a `SavedAlarmsOff` save result returned for an alarm when it is refused, the message in the entry sheet and the notice after saving a template, and answer it from the iOS bridge (verify: view model and UI tests for a refused alarm saving anyway, no prompt for notifications, precedence after the notification step, the template notice, and no message when allowed)
- [x] 2.2 Make older iOS and a denied permission deliver alarms as notifications, and make the Swift bridge cancel only alarms that are not alerting before scheduling the new set (verify: routing tests for both cases pass; checked by hand in 3.3)

## 3. Docs and verification

- [x] 3.1 Update the Reminders section of `README.md`: an alarm rings on iOS 26 and later with Open task and Stop, the alarm permission, and that older iPhones and the desktop show notifications (verify: the steps work as written in 3.3)
- [x] 3.2 Run `./kotlin build` and `./kotlin test` for all targets (verify: both succeed with no failing tests)
- [x] 3.3 On an iPhone with iOS 26: save an alarm and see the permission prompt once; an alarm with the app closed, locked and on silent; Open task from the lock screen and with the phone unlocked; Stop and the repeat after it; completing the task before the alarm; refusing the permission and seeing the message and a notification instead; and a notification-kind reminder still quiet (verify: manual)
