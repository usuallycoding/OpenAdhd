# Design

## Context

`secret/open-adhd.svg` is 1024x1024 with one transparent canvas and two fills: a green gear and pink "OPEN ADHD" text, together about the middle 55% of the canvas. The Android launcher uses an adaptive icon (`mipmap-anydpi-v26/ic_launcher.xml` and `_round`, background `drawable/ic_launcher_background.xml`, foreground `drawable-v24/ic_launcher_foreground.xml`) plus legacy PNGs per density, all still the template defaults. iOS has `AppIcon.appiconset` with `app-icon-1024.png` and two slots, dark and tinted, that have no file. `package-mac.sh` builds the `.icns` from `androidApp/res/mipmap-xxxhdpi/ic_launcher.png` at sizes 16 to 512. The desktop window sets no icon. The Android theme uses Material You (`dynamicLightColorScheme`) from Android 12, otherwise the palette in `Color.kt`. `rsvg-convert`, Inkscape, ImageMagick, `sips` and `iconutil` are present on the build Mac.

## Goals / Non-Goals

**Goals:**
- One script turns the artwork into the icons of every platform, repeatable at any time.
- The icons look right on their platform: adaptive on Android, no transparency on iOS, rounded on macOS.

**Non-Goals:**
- Changing the artwork, adding Windows or Linux icons, or runtime theming of the iOS and macOS icons.

## Decisions

- **A script run by hand, with its output committed.** The SVG lives in the git-ignored `secret/`, so the build cannot depend on it; the generated PNGs and XML are committed. Alternative: generating during the build; rejected because a clone without `secret/` could not build.
- **Rasterise with `rsvg-convert`, falling back to Inkscape and then ImageMagick**, picked by what is installed. Each draws the SVG at the size asked for, with transparency kept; the script composites the background with ImageMagick (`magick`, or `convert`). The script fails with a message naming what is missing.
- **Background colors come from the app palette, defined once in the script** as light `#F9FAEF` and dark `#12140E` (the surface colors in `Color.kt`), with a comment that they mirror it. The gear's green has weak contrast on the palette's light-green container colors, so the surface colors are used.
- **Android adaptive icon.** The foreground is the whole 1024 canvas drawn into the 108dp layer, since the artwork already sits well inside the 66% safe zone, as PNGs per density (108 to 432 px). The background is a color resource: `values/ic_launcher_background.xml` holds the light palette color, and `values-v31` points it at the system accent color (`@android:color/system_accent1_100`), so Android 12 and later follow the wallpaper. `values-night` and `values-night-v31` do the same for the dark theme with the dark palette color and `system_accent1_800`. Legacy `mipmap-*` PNGs and the round variants are composited with the palette color. A monochrome layer (the art in a single color) gives Android 13 themed icons. Alternative: converting the SVG to a vector drawable; rejected because it has two fills in one path and no gradients are needed.
- **iOS.** Three 1024 px opaque PNGs: light (the art on light surface), dark (on dark surface) and tinted (the art in grayscale on black), and `Contents.json` gets a `filename` for each; iOS rounds the corners itself.
- **macOS and desktop.** The script writes one 1024 px PNG with a rounded square (about 22% corner radius) and a transparent margin of about 10%, as macOS expects from an app icon, to `desktopApp/resources`. `package-mac.sh` builds the `.icns` from it at sizes 16 to 1024 with `@2x` pairs, and the desktop `Window` uses it as its icon through `painterResource`. Alternative: the Android PNG for the desktop; rejected because it is low resolution and has no rounded shape.
- **Safe to rerun.** The script writes only the files it owns, and removes the placeholder robot drawables once.

## Risks / Trade-offs

- [The system accent color as an icon background may clash with the green art, or give poor contrast, on some wallpapers] → Light uses a very light tone (`accent1_100`) and dark a very dark one (`accent1_800`), so the gear and text keep their contrast; checked on the device with a few wallpapers, and the palette color remains the fallback.
- [Three renderers may draw the SVG slightly differently] → The script prefers `rsvg-convert`; the others are fallbacks, and the output is compared by eye once.
- [Generated binaries add noise to the repository] → They are small PNGs, regenerated only when the artwork changes.
- [Themed (monochrome) icons use one color, so the gear and text merge] → The text and gear are made one shape for the monochrome layer; looks are checked on an Android 13 device.
