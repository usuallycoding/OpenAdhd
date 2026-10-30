# Design

## Context

`scripts/package-android.sh` runs `./kotlin build -m androidApp -p android -v debug` and copies `build/tasks/_androidApp_buildAndroidDebug/gradle-project-debug.apk`. The release variant already builds (`_androidApp_buildAndroidRelease`) but, without signing configured, only produces `gradle-project-release-unsigned.apk`, which is minified (about 7 MB against 25 MB for debug). The Kotlin Toolchain 0.13 has an `android.signing` setting in `module.yaml` that fills `storeFile`, `storePassword`, `keyAlias` and `keyPassword` from a properties file. `.gitignore` ignores `dist/` and `build/` only.

## Goals / Non-Goals

**Goals:**
- One command still leaves `dist/OpenAdhd.apk`, now signed with a stable release key kept out of git.
- A missing or incomplete key fails early with a message that says what to create.

**Non-Goals:**
- Generating the keystore from a script, Play Store signing, key rotation, CI, or changing the macOS packaging.

## Decisions

- **Use the toolchain's `android.signing` setting, not `apksigner` in the script.** `androidApp/module.yaml` gets `settings.android.signing` with `enabled: true` and `propertiesFile` pointing at `../secret/android-signing.properties` (relative to the module). The toolchain then signs the release APK itself, and the script only builds and copies it. Alternative: sign `release-unsigned` with `apksigner` in the script; rejected because it duplicates what the toolchain does and needs the passwords on a command line.
- **Release variant, signed.** The script builds with `-v release` and copies the signed `gradle-project-release*.apk`. The signed file is `gradle-project-release.apk` next to the unsigned one. Checked: debug builds keep the debug key, and with the properties file missing the toolchain only warns and still builds, leaving the release unsigned, which is why the script does its own check.
- **Secrets in `secret/` with fixed names**, `android-release.keystore` and `android-signing.properties`, where `storeFile` is `../secret/android-release.keystore`, because the toolchain resolves it from the module directory (`androidApp/`), not from the properties file. `secret/` is added to `.gitignore`. The script checks that both files exist and that the four keys are present before building.
- **No fallback to debug.** A debug-signed APK cannot update a release-signed install, so quietly producing one would only cause a failed install later.
- **Keystore creation is documented, not scripted.** The README shows the `keytool -genkeypair` command (RSA 2048, alias, about 27 years of validity) and an example `android-signing.properties`.
- **Passwords stay out of output.** The script prints no property values and no command line containing them.

## Risks / Trade-offs

- [Minified release code may break Room, the bundled SQLite or reflection-based code at run time; the earlier packaging change flagged this as untested] → Install the release APK and exercise every tab, a backup export and import, and a reminder before relying on it; if something breaks, add keep rules or turn minification off for release.
- [Losing the keystore or its passwords means no more updates over an installed app] → The README says to back `secret/` up outside this machine.
- [The first release install cannot update the debug-signed one] → Export a backup first, uninstall, install, import; the README says so.
- [Passwords sit in plain text in `secret/`] → Acceptable for a personal app on one's own machine; the directory is never committed.
- [`module.yaml` points at a file that does not exist on a fresh clone] → Checked: the toolchain only warns, and builds other than the package script are unaffected.
