# Tasks

## 1. The script

- [x] 1.1 Write `scripts/make-icons.sh`: check the SVG and a renderer (`rsvg-convert`, else Inkscape, else ImageMagick) and `sips`/`iconutil`, keep the two palette background colors in one place, and render the artwork at a given size with transparency (verify: running it without `secret/open-adhd.svg` or without a renderer exits non-zero with a message that names what is missing; rerunning changes no files)

## 2. Platforms

- [x] 2.1 Android: generate foreground layers per density, the monochrome layer, legacy and round mipmaps, the light and dark background colors with the system-color overrides for Android 12 and later, point the adaptive icon XML at them and remove the robot drawables (verify: `./kotlin build -m androidApp` succeeds; on a device the launcher shows the gear, themed icons show it in one color on Android 13, and the background follows the wallpaper on Android 12 and later in light and dark)
- [x] 2.2 iOS: generate the light, dark and tinted 1024 px icons and fill `Contents.json` (verify: the three files exist with no alpha channel; the icon shows on the home screen in light, dark and tinted modes on a device or simulator)
- [x] 2.3 macOS and desktop: generate the rounded 1024 px PNG in `desktopApp/resources`, make `package-mac.sh` build the `.icns` from it and give the desktop `Window` that icon (verify: `scripts/package-mac.sh` produces an app whose icon shows in Finder and the Dock; running the desktop app shows the icon in the Dock)

## 3. Docs and verification

- [x] 3.1 Add a short section to `README.md` on regenerating the icons, what the script needs and that the SVG is kept in `secret/` (verify: following it on this Mac regenerates the same files)
- [x] 3.2 Run `./kotlin build` and `./kotlin test` for all targets (verify: both succeed, apart from the unrelated iOS device signing failure if it still occurs)
- [x] 3.3 Look at every icon on its platform: Android in light and dark with two wallpapers, iOS in three modes, the macOS app and the desktop window (verify: manual; the gear and text are fully visible, centred and readable on each)
