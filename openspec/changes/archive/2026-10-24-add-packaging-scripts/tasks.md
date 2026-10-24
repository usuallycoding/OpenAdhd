# Tasks

## 1. Find out what the toolchain produces

- [x] 1.1 Run `./kotlin package -m androidApp -v debug` and `./kotlin package -m desktopApp -f executable-jar`, note which files are written and where, and record in design.md whether an APK is produced directly (verify: the output paths are listed and design.md names the chosen Android path)

## 2. macOS script

- [x] 2.1 Add `scripts/package-mac.sh`: build the executable jar, create the `.icns` from the launcher PNG (skipping the icon if that fails), run `jpackage` to make `dist/OpenAdhd.app` and `dist/OpenAdhd.dmg`, and fail with a clear message if `jpackage` is missing (verify: running it twice leaves both files in `dist/`)
- [x] 2.2 Open the built `.app` and confirm it starts with its own runtime, shows the icon and keeps the data in `~/.openadhd/` (verify: manual, the app opens, data created before is still there)

## 3. Android script

- [x] 3.1 Add `scripts/package-android.sh` following the path chosen in 1.1, running `./kotlin build -m androidApp -p android` and copying the debug APK to `dist/OpenAdhd.apk` (verify: the file exists and `apksigner verify` accepts it)
- [x] 3.2 Install the APK on a phone, then build again and install over it with `adb install -r` (verify: manual, the second install succeeds and the app keeps its data)

## 4. Housekeeping

- [x] 4.1 Add `dist/` to `.gitignore` and a Packaging section to `README.md` with both commands, the install steps and the first-open confirmation on macOS (verify: `git status` shows no `dist/` files after a build, and the README steps work as written)
