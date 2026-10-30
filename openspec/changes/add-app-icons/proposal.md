# Proposal

## Why

The app still wears placeholder icons: the default Android robot as the adaptive launcher icon, a single 1024 px image on iOS with its dark and tinted slots empty, a macOS icon upscaled from a 192 px Android PNG, and no icon at all on the desktop window. The real artwork, `secret/open-adhd.svg`, is a transparent gear with the app's name, so each platform needs it placed on a background that fits the app's theme.

## What Changes

- **Script:** `scripts/make-icons.sh` reads `secret/open-adhd.svg` and writes the icon files of every platform. It is run by hand when the artwork changes, and its output is committed, so building the app never needs `secret/`.
- **Background:** the app's own palette: the light surface color (`#F9FAEF`) for the normal icon, the dark surface (`#12140E`) for dark variants. On Android 12 and later the adaptive icon's background follows the phone's wallpaper color through a system color resource, with the palette color as the fallback for older versions and for the legacy icons.
- **Android:** adaptive foreground layers per density with the art inside the safe zone, a background color resource, legacy and round launcher PNGs, and a monochrome layer for Android 13 themed icons; the default robot drawables go.
- **iOS:** the light 1024 px icon, a dark variant and a tinted (grayscale) variant, and their entries in `Contents.json`.
- **macOS and desktop:** a 1024 px PNG with a rounded-square shape and padding, which `package-mac.sh` turns into the `.icns` (instead of the 192 px Android PNG) and which the desktop window uses as its icon.
- Not changed: the artwork itself, the app name, and any app behavior.

## Capabilities

### New Capabilities
None. This is build tooling and assets and changes no app behavior, so the change sets `skip_specs: true`.

### Modified Capabilities
None.

## Impact

- New: `scripts/make-icons.sh`, generated files under `androidApp/res` (mipmaps, a foreground and monochrome layer per density, a background color and `values-v31` override), `iosApp/src/Assets.xcassets/AppIcon.appiconset`, and `desktopApp/resources`.
- Edited: the launcher icon XML in `androidApp/res`, `scripts/package-mac.sh` (icon source), `desktopApp/src/main.kt` (window icon), `README.md`.
- Needs an SVG renderer (`rsvg-convert`, or Inkscape or ImageMagick as fallbacks), plus `sips` and `iconutil`, which are built into macOS. The script fails with a message if the SVG or a renderer is missing.
- Out of scope: Windows and Linux icons, a notification small icon, splash screens, and changing the artwork.
