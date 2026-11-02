# Tasks

## 1. Build an unsigned IPA

- [x] 1.1 Prove a device build works unsigned: run the `xcodebuild` Release command from design.md by hand and verify `build/package-ios/Build/Products/Release-iphoneos/OpenAdhd.app` exists (if Release fails through the toolchain, try Debug and record which one the script will use)
- [x] 1.2 Write `scripts/package-ios.sh` (plain `sh`, `set -eu`, like the other scripts): check Xcode and the iphoneos SDK, build, zip `Payload/OpenAdhd.app` to `dist/OpenAdhd.ipa`, and print version, build number and size read with `plutil`; verify with `unzip -l dist/OpenAdhd.ipa` showing `Payload/OpenAdhd.app/Info.plist` and the executable
- [x] 1.3 Verify the project is left alone: `git status` shows no change to `iosApp/module.xcodeproj` caused by running the script, and running it with Xcode missing from PATH (or SDK absent) stops with a clear message

## 2. AltStore / SideStore source

- [x] 2.1 Add `altstore/source.json` in the AltStore source format with the app's bundle id, name, version, build, size, a download URL pointing at the GitHub Release asset `OpenAdhd.ipa`, icon URL and a short description; verify it is valid JSON (`python3 -m json.tool`) and that every field matches what the script prints
- [x] 2.2 Have `package-ios.sh` print the `version`, `size` and `downloadURL` lines to paste into `source.json`; verify by running it and comparing with the file

## 3. Documentation

- [x] 3.1 Add "Installing on an iPhone" to `README.md`: the IPA from GitHub Releases, AltStore, SideStore, Sideloadly and build-from-source with a free Apple ID, which to choose, the 7-day expiry and what it does to the app and its data, the free-ID limits (3 active apps), and Settings export as the backup; verify every command and URL in it is one that exists in the repo
- [x] 3.2 Update the README's Packaging and Version sections for `package-ios.sh` and for the release steps (bump `Info.plist`, run the script, attach `dist/OpenAdhd.ipa` to a GitHub Release, update `altstore/source.json`); verify by following the steps once up to the release

## 4. Integration check

- [x] 4.1 Install the IPA on an iPhone with a free Apple ID through Sideloadly or AltStore, open the app, create a task with a reminder and confirm it fires (and an Alarm on iOS 26 or later); report the result, or state plainly that no device was available

## Workflow follow-up

- Archive the change once the IPA has been built and the README reviewed.
