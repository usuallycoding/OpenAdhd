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

Two scripts leave their results in `dist/` (ignored by git):

- macOS: `scripts/package-mac.sh` builds `OpenAdhd.app` and `OpenAdhd.dmg`, with its own Java runtime. It needs a JDK 25 or newer with `jpackage`: it uses `JAVA_HOME` if that is one, otherwise the JDK the Kotlin toolchain downloaded. The app is not signed, so the first time, right-click it and choose Open. Your data stays in `~/.openadhd/` when you replace the app.
- Android: `scripts/package-android.sh` builds the release `OpenAdhd.apk`, signed with your own key from `secret/`, and prints its certificate. Install it with `adb install -r dist/OpenAdhd.apk`, or copy it to the phone. The script stops with a message if the key is missing and never builds a debug APK instead.

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

### Appearance

The theme (Follow system theme, Light or Dark) is chosen in Settings under Appearance and is remembered the next time the app starts. It is a normal setting, so it can be restored to its default, it is part of a backup, and Delete all data resets it.

### Reminders

On Today, a task that is not done yet has a "..." button. Its sheet lets you set one reminder for the task and delete it (while nothing in it is done):

- **Start:** In 30 minutes, In 1 hour, This evening (18:00) or a time you pick.
- **Repeat:** off, or every 15, 30, 60 or 120 minutes. A reminder repeats until the task is done or the stop time is reached. "Stop reminders at" is in Settings (default 22).
- Completing the task stops its reminders, unchecking it brings back the ones still to come, and they all end at midnight.
- The first time you save a reminder the system asks to allow notifications.
- **Notification or alarm:** on Android and iOS a reminder can be a Notification (the default) or an Alarm, in the task's "..." sheet and in the template form. On **Android** an alarm rings with the alarm sound on the alarm volume, even on silent, vibrates, and shows an alarm screen over the lock screen with **Open task** and **Dismiss**. Dismiss stops this ring and later repeats still ring; opening the app, from the launcher or anywhere else, or swiping the notification away where the phone allows it, stops it the same way; an ignored ring stops after 10 minutes and leaves a notification. On Android 14 and later the lock-screen screen needs "full-screen alerts" to be allowed for OpenAdhd in the device settings (saving an alarm tells you and opens the setting); without it the alarm still rings, with a heads-up notification. Phone battery savers can delay alarms on some devices. On an **iPhone with iOS 26 or later** an alarm is a system alarm (AlarmKit): it rings through Silent mode and Focus, shows on the lock screen with the system's Stop button and an **Open task** button, and the first alarm you save asks to allow alarms for OpenAdhd. If you refuse, or the iPhone is older than iOS 26, the reminder is shown as a notification. The **desktop** app always shows an alarm as a notification.
- **Tapping a notification** on Android and iOS opens the app on Today, scrolls to the task it is about and highlights it briefly, whether the app was closed, in the background or in front. On **macOS** tapping a notification does not jump to the task.
- **Android and iOS** show the notification even when the app is closed (Android also after a restart). On **macOS** the app must be running: a reminder that comes due while it is closed is not shown later.

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
- **Reminders when the app is not opened:** the reminders and alarms of scheduled templates are armed before the tasks exist, so they fire even on a day you do not open the app. On **Android** the plan covers today and the next 29 days. The phone only lets so many be armed at once (20 alarms, about 100 notifications), so the rest is kept by the app and armed as each reminder fires and after a restart; up to 1000 upcoming times are kept, so a reminder that repeats every 30 minutes lasts one to two weeks and a once-a-day reminder about a month. On **iOS** the plan covers 7 days and the phone allows 64 pending notifications, so a frequently repeating reminder reaches only a few days; opening the app arms the rest. After the plan runs out nothing is left to fire until you open the app. The desktop app still needs to be running. Tapping such a reminder opens Today; the task appears there once the app has added it.

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
