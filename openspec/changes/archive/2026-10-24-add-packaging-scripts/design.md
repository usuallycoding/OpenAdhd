# Design

## Context

The project is built with the Kotlin Toolchain 0.13 (`./kotlin`), not Gradle. Its `package` command supports the formats `jar`, `executable-jar`, `aab` and `maven-central-bundle` only, so there is no `.app`, `.dmg` or `.apk` output. The desktop module's main class is `com.usuallycoding.openadhd.MainKt`. The Android module has no signing configuration of its own and no desktop icon exists; the Android launcher icons are PNGs under `androidApp/res`. The machine builds on macOS with JDK 21, which includes `jpackage`. User data lives outside the app (`~/.openadhd/` on desktop), so replacing the app keeps it.

## Goals / Non-Goals

**Goals:**
- One command per target that leaves a ready file in `dist/`, runnable again at any time.
- Scripts that fail early with a clear message when a tool is missing.

**Non-Goals:**
- Windows, Linux, notarisation, Play Store, CI or version bumping.
- Changing the app, its modules or the toolchain version.

## Decisions

- **macOS: `executable-jar` plus `jpackage`.** `./kotlin package -m desktopApp -f executable-jar` produces a self-contained jar; `jpackage --type app-image` wraps it with a bundled runtime into `OpenAdhd.app`, then `--type dmg` makes the disk image. Alternative: ship the jar alone; rejected because it needs Java installed and has no icon or dock presence.
- **jpackage must come from a JDK 25 or newer.** The classes are compiled for Java 25 (class file version 69), so a bundled Java 21 runtime fails at start with `UnsupportedClassVersionError`. The script uses `JAVA_HOME` when that is JDK 25 or newer, otherwise the JDK the toolchain downloads into its cache. Alternative: lower the JVM target in the module settings; rejected because it changes the app build.
- **Do not launch the toolchain's executable jar as it is.** Probing showed it fails with `NoClassDefFoundError: androidx/compose/runtime/Composer`: where androidx and org.jetbrains.compose publish a jar with the same file name, the fat jar keeps the few-KB redirect stub instead of the real jar. The script unpacks the jar, replaces every lib under 20 KB with the larger jar of the same name from the toolchain's Maven cache, repacks `BOOT-INF/classes` as `app.jar`, and has `jpackage` start `MainKt` directly with all jars on the classpath.
- **Runtime size: let `jpackage` bundle the full JDK runtime first.** A trimmed runtime made with `jlink` saves size but can break Compose or SQLite at run time; revisit only if the size matters.
- **Icon: convert the Android launcher PNG to `.icns` with `sips` and `iconutil` (both built into macOS)** inside the script, so no new asset is committed. If the conversion fails, the script builds without an icon rather than failing.
- **Android: build the debug APK and copy it.** The probe showed that `./kotlin package` writes no APK ("No package tasks were found" for `-v debug`; only `-f aab` and the release bundle exist), but `./kotlin build -m androidApp -p android` writes `build/tasks/_androidApp_buildAndroidDebug/gradle-project-debug.apk` (about 25 MB), already signed. The release variant gives only an unsigned, minified APK (about 7 MB), which would need `apksigner` and its minified code is untested against Room. The script uses the debug APK: no `bundletool`, no extra tooling.
- **Signing: the Android debug keystore is the stable key.** The debug APK is signed with `~/.android/debug.keystore`, which stays the same between builds on this Mac, so a newer APK installs over an older one. No keystore is created or committed. Trade-off: the build must stay on this Mac, since another machine would have a different debug key.
- **Scripts live in `scripts/`, output goes to `dist/`.** Both are plain `sh` with `set -eu`, run from any directory by resolving the project root from the script's location. `dist/` is git-ignored.

## Risks / Trade-offs

- [The stub replacement relies on the toolchain cache layout and on this executable-jar bug] → The script fails loudly if no JDK 25 is found; if a toolchain update fixes the bug, the replacement step simply finds no stubs.
- [Unsigned macOS app shows a Gatekeeper prompt] → Acceptable for personal use; the README says to right-click, Open once.
- [The debug APK is debuggable and about 25 MB] → Acceptable for personal use; a signed, minified release APK is a later option.
- [Changing signing keys between builds breaks in-place updates] → One stable key, never switched; the README warns about it.
- [The bundled runtime makes the `.app` large (about 150 MB or more)] → Accepted; `jlink` trimming is a later option.
