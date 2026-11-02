# Design

## Context

See proposal.md for why. Observed state:

- `iosApp` is built by the Kotlin toolchain. `module.xcodeproj` is generated and committed; its single build phase runs `kotlin tool xcode-integration`, so `xcodebuild` drives the Kotlin build.
- The project has two configurations. Debug carries a personal `DEVELOPMENT_TEAM`; Release has an empty one. Only Debug tasks exist in `build/tasks` today, so a Release device build has not been shown to work.
- Version and build number are plain values in `iosApp/src/Info.plist` (1.0.0 / 1). The bundle id is `com.usuallycoding.openadhd.OpenAdhd`.
- The app uses AlarmKit (`NSAlarmKitUsageDescription`) and local notifications. Neither needs a paid-account entitlement, which is why a free-ID install keeps working reminders.
- The existing scripts write to `dist/`, check their own preconditions with a clear message, and are plain `sh`.

## Goals / Non-Goals

**Goals:**
- One command produces an IPA that AltStore, SideStore and Sideloadly can re-sign.
- Users learn the real limits (7-day signature, 3 active apps on a free ID) before they install.
- Updates reach AltStore/SideStore users without manual downloads.

**Non-Goals:**
- Signing the IPA, or any use of the maintainer's Apple ID or team.
- Automating GitHub Releases.
- Making the 7-day expiry go away; it is Apple's rule for free accounts.

## Decisions

**Unsigned IPA, built with `xcodebuild` and signing disabled.** The script runs `xcodebuild -project iosApp/module.xcodeproj -scheme iosApp -configuration Release -sdk iphoneos -derivedDataPath build/package-ios CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO CODE_SIGN_IDENTITY=""` and zips `Payload/OpenAdhd.app` into the IPA. Overriding on the command line leaves the committed project untouched. *Alternative:* `xcodebuild archive` plus `-exportArchive`; rejected because export insists on a signing identity or profile. *Alternative:* sign with the maintainer's free ID; rejected because such a profile only runs on that person's registered devices.

**Release, with a Debug fallback decided at implementation.** Release is what users should get, but the toolchain's Release path for iOS is unproven here. The first task builds it; if it does not work, the script builds Debug and the README and script comment say so. This does not change the interface, only the configuration name in one variable.

**AltStore source as a file in the repo.** `altstore/source.json` follows the AltStore source format (source name, one app with bundle id, version, build, size, download URL, icon, description). Its `downloadURL` points at the GitHub Release asset, and users add the raw file URL from the `main` branch. SideStore reads the same format. *Alternative:* no source, just a download link; kept as the fallback since the IPA alone works. *Alternative:* a hosted site; rejected as extra infrastructure for no gain.

**Version stays in Info.plist.** The script reads `CFBundleShortVersionString` and `CFBundleVersion` from it with `plutil` and uses them for the printed source entry, so there is one place to bump (as the README's Version section already says) and the script never invents a version.

**Preconditions checked up front.** The script stops with a message when Xcode's command line tools are missing or the iOS SDK is not installed, as `package-android.sh` does for its key, rather than failing midway through a long build.

**Docs name the routes honestly.** AltStore needs a Mac or PC companion on the same Wi-Fi; SideStore needs a computer only for first setup, then refreshes on the phone; Sideloadly is manual. The README says which to pick and does not promise automatic refresh without caveats.

## Risks / Trade-offs

- [Release device build may not work through the Kotlin toolchain] -> First task proves it; fall back to Debug and document it.
- [Unsigned `.app` is missing a signature, so a bad zip layout would fail only at install time] -> Verify the layout with `unzip -l` (Payload/OpenAdhd.app/Info.plist, executable present) and, if a phone is available, install through Sideloadly or AltStore once.
- [AlarmKit on a re-signed free-ID build] -> Not verified without a device on iOS 26; the README promises only what Apple documents (notification fallback exists in the app) and the manual check is a task.
- [Signature expires after 7 days; the app stops launching] -> README explains refresh options and that data survives; Settings export is the backup.
- [Source file and IPA can drift apart] -> The script prints the exact version, size and file name to paste; the release steps in the README list updating `source.json` together with attaching the IPA.
- [Jailbreak/TrollStore users excluded] -> Accepted; not worth supporting.
