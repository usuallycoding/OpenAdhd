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
- Android: `scripts/package-android.sh` builds `OpenAdhd.apk` and checks its signature. It is the debug build, signed with the debug key in `~/.android/debug.keystore`. Install it with `adb install -r dist/OpenAdhd.apk`, or copy it to the phone. Build it on the same Mac every time: an APK signed with a different key will not install over the old one.

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
