This is a Kotlin Multiplatform project targeting Android, Desktop (JVM) and iOS, built with the [Kotlin Toolchain](https://kotlin-toolchain.org/dev/).

- [/androidApp](./androidApp) contains the Android application.
- [/desktopApp](./desktopApp) contains the desktop (JVM) application.
- [/iosApp](./iosApp) contains the iOS application.
- [/shared](./shared) holds the code shared across your applications — Compose UI, business logic, and platform-specific implementations. [src](./shared/src) is for common code; the sibling `src@<platform>` folders (for example `src@android`) hold code compiled only for the platform named in the folder.

The `kotlin` (macOS/Linux) and `kotlin.bat` (Windows) scripts in the project root are self-bootstrapping wrappers for the Kotlin Toolchain: they download the pinned toolchain version on first use, so no separate installation is required. Build the whole project with `./kotlin build`.

### Data

Task templates (with descriptions and subtasks), daily tasks, reward templates, purchases and the coin ledger are stored locally in a SQLite database (`openadhd.db`) and never leave the device. On desktop it lives in `~/.openadhd/`; on Android it is in the app's database folder, and on iOS in the app's Documents folder.

### Running

Use the run configurations provided by the run widget in your IDE's toolbar. You can also run each module from the command line:

- Android app: `./kotlin run -m androidApp`
- Desktop app: `./kotlin run -m desktopApp`
- iOS app: `./kotlin run -m iosApp`

### Packaging (for your own use)

Three scripts leave their results in `dist/` (ignored by git):

- macOS: `scripts/package-mac.sh` builds `OpenAdhd.app` and `OpenAdhd.dmg`, with its own Java runtime. It needs a JDK 25 or newer with `jpackage`: it uses `JAVA_HOME` if that is one, otherwise the JDK the Kotlin toolchain downloaded. The app is not signed, so the first time, right-click it and choose Open. Your data stays in `~/.openadhd/` when you replace the app.
- Android: `scripts/package-android.sh` builds the release `OpenAdhd.apk`, signed with your own key from `secret/`, and prints its certificate. Install it with `adb install -r dist/OpenAdhd.apk`, or copy it to the phone. The script stops with a message if the key is missing and never builds a debug APK instead.
- iOS: `scripts/package-ios.sh` builds the release app for a real iPhone and zips it, **unsigned**, as `OpenAdhd.ipa`. It needs Xcode with the iOS SDK. Nobody can install that file as it is; see "Installing on an iPhone" below.

#### The Android release key

`secret/` is ignored by git and holds two files that you create once. From the project root:

```sh
mkdir secret
keytool -genkeypair -v -keystore secret/android-release.keystore -storetype PKCS12 \
  -alias openadhd -keyalg RSA -keysize 2048 -validity 10000
```

`keytool` asks for a password (use the same one for the keystore and the key) and a name. Then create `secret/android-signing.properties`:

```properties
storeFile=../secret/android-release.keystore
storePassword=the password you chose
keyAlias=openadhd
keyPassword=the password you chose
```

`storeFile` is relative to `androidApp/`, so keep it as shown. **Back up `secret/` somewhere outside this machine**: with a lost keystore or password you can never update an installed app, only reinstall it.

Android will not update an app signed with a different key. The first release APK therefore cannot replace an older debug-signed install: export a backup under Settings, uninstall the old app, install the new one and import the backup. After that every build installs over the last one and keeps its data, from any machine that has `secret/`.

### Version

The end of Settings shows the version of the app that is installed, such as "OpenAdhd 1.0.0 (1)" on Android and iOS (version and build number) and "OpenAdhd 1.0.0" on the packaged macOS app. The desktop app run with `./kotlin run` has no packaged version and shows "OpenAdhd dev". Each platform keeps its own number, so a release changes all three, and the label shows what each build really holds:

- Android: `versionName` and `versionCode` under `settings.android` in `androidApp/module.yaml` (raise the code with every release, Android only installs a higher one over an older one).
- iOS: `CFBundleShortVersionString` and `CFBundleVersion` in `iosApp/src/Info.plist`. They are written there as plain values, not as project variables, because the toolchain regenerates `module.xcodeproj`.
- macOS: `version=` at the top of `scripts/package-mac.sh`.

### Installing on an iPhone

OpenAdhd is not in the App Store and there is no TestFlight build, because both need a paid Apple Developer account. What works with a **free Apple ID**:

- **The IPA, re-signed by you.** Download `OpenAdhd.ipa` from the [latest release](https://github.com/usuallycoding/OpenAdhd/releases/latest) and let one of these tools sign it with your own Apple ID and put it on the phone:
  - [AltStore](https://altstore.io): a helper app (AltServer) on a Mac or PC on the same Wi-Fi installs the app and refreshes it. To get update prompts, add this source in AltStore: `https://raw.githubusercontent.com/usuallycoding/OpenAdhd/main/altstore/source.json`.
  - [SideStore](https://sidestore.io): the same source file works. It needs a computer for the first setup only and then refreshes the app on the phone.
  - [Sideloadly](https://sideloadly.io): a computer app that installs the IPA once. You run it again every week.
- **Build it yourself.** Open `iosApp/module.xcodeproj` in Xcode, pick your own team under Signing & Capabilities, change the bundle identifier to one of yours (the one in the project belongs to someone else's team) and run it on your phone.

Any of these needs Developer Mode switched on on the phone (Settings, Privacy & Security). Apple's limits for a free Apple ID apply to all of them:

- The signature lasts **7 days**. After that the app no longer opens until it is refreshed or installed again, so choose AltStore or SideStore if you want that to happen by itself. Your data stays on the phone and is there again after a refresh. Reinstalling over the old app keeps it too, but deleting the app removes it, so export a backup under Settings now and then.
- Only 3 apps signed with a free Apple ID can be active at once.
- Reminders and alarms use the phone's own local notifications and AlarmKit, so they do not need anything a free account lacks. An alarm needs iOS 26 or later; before that it shows as a notification.

#### Making a release

1. Raise the version in `iosApp/src/Info.plist` (see Version above) and run `scripts/package-ios.sh`.
2. Create a GitHub release tagged `v<version>` (for example `v1.0.0`) and attach `dist/OpenAdhd.ipa`.
3. Paste the `version`, `versionDate`, `downloadURL` and `size` lines the script printed into `altstore/source.json` and commit it. The size changes with every build, so use the one printed for the file you attached.

### App icons

`scripts/make-icons.sh` makes the icon of every platform from `secret/open-adhd.svg`, a transparent drawing kept out of git with the signing key. It puts the drawing on the app's surface colors (light, and dark for the dark variants) and writes the Android adaptive icon (with a themed-icon layer, and a background that follows the wallpaper color on Android 12 and later), the iOS light, dark and tinted icons, and the rounded macOS and desktop window icon. Run it again only when the drawing changes, and commit what it writes: building the app does not need the SVG. It needs `magick` (ImageMagick) and `sips` (part of macOS), and draws the SVG with `rsvg-convert`, Inkscape or ImageMagick, whichever is installed.

### Appearance

The theme (Follow system theme, Light or Dark) is chosen in Settings under Appearance and is remembered the next time the app starts. It is a normal setting, so it can be restored to its default, it is part of a backup, and Delete all data resets it.

### Reminders

On Today, a task that is not done yet has a "..." button. Its sheet lets you set one reminder for the task and delete it (while nothing in it is done):

- **Start:** In 30 minutes, In 1 hour, This evening (18:00) or a time you pick.
- **Repeat:** off, or every 15, 30, 60 or 120 minutes. A reminder repeats until the task is done or the stop time is reached. "Stop reminders at" is in Settings (default 22:00); you choose it on a clock, with minutes, and a plain hour saved by an earlier version still counts as that hour.
- Completing the task stops its reminders, unchecking it brings back the ones still to come, and they all end at midnight.
- The first time you save a reminder the system asks to allow notifications.
- **Notification or alarm:** a reminder can be a Notification (the default) or an Alarm, in the task's "..." sheet and in the template form. On **Android** an alarm rings with the alarm sound on the alarm volume, even on silent, vibrates, and shows an alarm screen over the lock screen with **Open task** and **Dismiss**. Dismiss stops this ring and later repeats still ring; opening the app, from the launcher or anywhere else, or swiping the notification away where the phone allows it, stops it the same way; an ignored ring stops after 10 minutes and leaves a notification. On Android 14 and later the lock-screen screen needs "full-screen alerts" to be allowed for OpenAdhd in the device settings (saving an alarm tells you and opens the setting); without it the alarm still rings, with a heads-up notification. Phone battery savers can delay alarms on some devices. On an **iPhone with iOS 26 or later** an alarm is a system alarm (AlarmKit): it rings through Silent mode and Focus, shows on the lock screen with the system's Stop button and an **Open task** button, and the first alarm you save asks to allow alarms for OpenAdhd. If you refuse, or the iPhone is older than iOS 26, the reminder is shown as a notification. On the **desktop** an alarm rings with a looping beep and an alarm card that stays above other windows, even when the main window is hidden in the tray; on macOS the Dock icon also bounces. The card has a row for each ringing task with **Open task** (shows the window and the task) and **Dismiss** (ends that ring; later repeats still ring), several alarms stack on one card with one sound, a ring stops by itself after 10 minutes, and it ends when its task is completed or deleted. Every ring also leaves the usual notification. An alarm that came due while the Mac slept rings once when it wakes, saying when it was due. The ring comes from the app, so Focus does not silence it, but nothing rings while the app is quit or the Mac is asleep, a muted Mac is silent (the card still shows), and the card cannot appear over the lock screen or a full-screen app.
- **When reminders are turned off in the system:** if notifications (or, on an iPhone, alarms, or on Android 14 and later, full-screen alerts) are off for OpenAdhd, saving a reminder still works and says so, in the task's sheet and in a message after saving a template. Both have an **Open settings** button that opens the app's notification settings (alarms: the app's settings page on an iPhone, the full-screen alerts setting on Android). When you come back to the app it arms its reminders again, so a reminder you set while notifications were off fires at its time once you turn them on, without restarting the app.
- **Tapping a notification** on Android and iOS opens the app on Today, scrolls to the task it is about and highlights it briefly, whether the app was closed, in the background or in front. On **macOS** tapping a notification does not jump to the task.
- **Android and iOS** show the notification even when the app is closed (Android also after a restart). The **desktop** app must be running, but closing its window only hides it: it stays in the menu bar (macOS) or system tray, where **Open** brings the window back and **Quit** exits and stops reminders. The first close says so once. Where the system has no tray, closing the window quits the app. A reminder that comes due while the app is not running is not shown later, and one that comes due while the computer sleeps is shown on waking only if it is at most 2 minutes late. On the packaged **macOS** app, **Launch at login** under Settings, Startup (off by default) starts it hidden in the menu bar at login, so reminders work without opening it; it is a launch agent in `~/Library/LaunchAgents/`, which you can also remove there.

### Adding a task to Today

The + button on Today opens a list of your task templates. The search field and the Low, Medium and High chips stay at the top while the list scrolls:

- **Search** matches the title and the description. **Energy chips** show only templates of that energy; tap the selected chip again to clear it. Both apply together.
- Press **Done**, scroll the results or tap empty space in the list to put the keyboard away; tap the field to bring it back.
- **Dice:** the dice next to the chips picks one of the templates currently listed, highlights it and scrolls to it, without adding anything. Tap it to add the task, or roll again; a roll avoids repeating the previous one when more than one is listed.

### Searching templates

The Tasks and Rewards tabs of Templates each have a search field above the list. It matches the name and the description, ignoring case, and each tab remembers its own text until the app is closed. Tap the list, scroll it or press Done to put the keyboard away. While a search holds text, **Reorder** is unavailable (typing turns it off), and saving a new template or reward clears that tab's search so the new one is not hidden.

### Browsing past days

The History tab of Today and the history in the Shop show one day at a time. Tap the date to open a calendar: every earlier day can be opened (the Shop's also today), and days that have tasks or redemptions carry a dot under the number. An empty day opens on its "nothing here" message. Use the arrows to change month, then **OK** to show the chosen day.

### Scheduled templates

In the template form, "Add to Today schedule" lets a template put itself on Today:

- **None:** only when you add it by hand. **Date:** one day. **Weekly:** the weekdays you tick. **Period:** every day from one date to a later one. **Daily:** every day.
- **Reminder on the generated task:** an optional time of day and, once a time is set, a repeat (off, or every 15, 30, 60 or 120 minutes). The task you get on Today carries that reminder, and you can still change or remove it from its "..." sheet without touching the template. A time that has already passed is skipped, so a repeating reminder resumes at its next time.
- The task is added when the app starts, when the date changes while it is running, and right away when you save a template that is due today. Only today is filled in: days you did not open the app are not made up.
- A template adds at most one task a day. If you delete the generated task, it does not come back. Adding the template by hand with the + button gives the task the same reminder.
- **Reminders when the app is not opened:** the reminders and alarms of scheduled templates are armed before the tasks exist, so they fire even on a day you do not open the app. On **Android** the plan covers today and the next 29 days. The phone only lets so many be armed at once (20 alarms, about 100 notifications), so the rest is kept by the app and armed as each reminder fires and after a restart; up to 1000 upcoming times are kept, so a reminder that repeats every 30 minutes lasts one to two weeks and a once-a-day reminder about a month. On **iOS** the plan covers 7 days and the phone allows 64 pending notifications, so a frequently repeating reminder reaches only a few days; opening the app arms the rest. After the plan runs out nothing is left to fire until you open the app. The desktop app still needs to be running (it keeps running in the tray after its window is closed). Tapping such a reminder opens Today; the task appears there once the app has added it.

### Backup

In Settings, under Data, you can save everything to one JSON file and bring it back:

- **Export a backup** opens the system's save dialog (`openadhd-<date>.json`). The file holds your task and reward templates, Today entries and their progress, the coin ledger, purchases and rolls, the order of every list, and any settings you changed.
- **Import a backup** opens a file, checks all of it, shows what it holds, and only then replaces everything in the app. A file that is not a backup, is damaged, or comes from a newer version of the app is refused and nothing changes.
- **Delete all data** removes everything, settings included, after a confirmation that offers to export a backup first.

To move to another device, export on the old one, copy the file across and import it on the new one. A backup made on macOS, Android or iOS can be imported on any of them.

### Testing

Run the project's tests with `./kotlin test`, or `./kotlin test -m <module>` for a single module.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html), [Kotlin Toolchain](https://kotlin-toolchain.org/dev/), [Compose Multiplatform](https://kotlinlang.org/compose-multiplatform/).
