# Tasks

## 1. Keys and ignore rule

- [x] 1.1 Add `secret/` to `.gitignore` (verify: `git check-ignore secret/android-signing.properties` prints the path, and `git status` shows nothing under `secret/` after creating files there)
- [x] 1.2 Create `secret/android-release.keystore` with `keytool` and `secret/android-signing.properties` with `storeFile`, `storePassword`, `keyAlias`, `keyPassword`, using the command the README will document (verify: `keytool -list -keystore secret/android-release.keystore` shows the alias; `git status` does not list either file)

## 2. Signing

- [x] 2.1 Enable `settings.android.signing` in `androidApp/module.yaml` with the properties file at `../secret/android-signing.properties` (verify: `./kotlin build -m androidApp -p android -v release` produces a signed APK; note whether a debug build and `./kotlin build` without `secret/` still work, and adjust per the design if not)
- [x] 2.2 Update `scripts/package-android.sh` to check `secret/` and its four keys, build `-v release`, copy the signed APK to `dist/OpenAdhd.apk` and keep the `apksigner` check (verify: with `secret/` present the script prints a certificate that is not the Android Debug one; with the properties file or keystore moved away it exits non-zero with a message naming the missing piece and does not touch `dist/OpenAdhd.apk`; no password appears in its output)

## 3. Docs and verification

- [x] 3.1 Update the Packaging section of `README.md`: release key instead of debug key, the `keytool` command, an example `android-signing.properties`, the one-time uninstall with a backup export and import, and backing up `secret/` (verify: following the text on a clean checkout gives a working signed APK)
- [x] 3.2 Run `./kotlin build` and `./kotlin test` for all targets (verify: both succeed with no failing tests)
- [x] 3.3 On the phone, export a backup, uninstall the old app, `adb install -r dist/OpenAdhd.apk`, import the backup, then use every tab, add and check a task, redeem a reward, export a backup and set a reminder, and finally build and install again over it (verify: manual; nothing crashes from minification and the second install keeps the data)
