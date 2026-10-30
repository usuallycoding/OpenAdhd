#!/bin/sh
# Makes the app icon of every platform from secret/open-adhd.svg, a transparent drawing, by putting it on the
# app's theme colors. Run it by hand when the drawing changes and commit what it writes; builds do not need the SVG.
#
#   Android  androidApp/res: adaptive icon layers, background colors, legacy and round icons
#   iOS      iosApp/src/Assets.xcassets/AppIcon.appiconset: light, dark and tinted icons
#   desktop  desktopApp/resources/app-icon.png: the macOS app icon and the window icon
set -eu

root=$(cd "$(dirname "$0")/.." && pwd)
svg="$root/secret/open-adhd.svg"

# The surface colors of the app theme (shared/src/ui/theme/Color.kt: surfaceLight and surfaceDark).
light=F9FAEF
dark=12140E

fail() { echo "$1" >&2; exit 1; }

[ -f "$svg" ] || fail "Missing $svg: put the icon drawing there."
command -v magick >/dev/null 2>&1 || fail "ImageMagick (magick) is needed to put the drawing on its background."
command -v sips >/dev/null 2>&1 || fail "sips is needed (it is part of macOS)."

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

# Draws the SVG on a transparent square of $2 pixels into $1, with whichever renderer is installed.
render_svg() {
  if command -v rsvg-convert >/dev/null 2>&1; then
    rsvg-convert -w "$2" -h "$2" "$svg" -o "$1"
  elif command -v inkscape >/dev/null 2>&1; then
    inkscape "$svg" -w "$2" -h "$2" -o "$1" >/dev/null 2>&1
  else
    magick -background none -density 300 "$svg" -resize "${2}x${2}" "$1"
  fi
}
{ command -v rsvg-convert || command -v inkscape || command -v magick; } >/dev/null 2>&1 \
  || fail "An SVG renderer is needed: rsvg-convert, Inkscape or ImageMagick."

# The drawing without its empty margin, so it can be centred and sized on its own.
render_svg "$work/master.png" 2048
magick "$work/master.png" -trim +repage "$work/art.png"

# Every PNG is written the same way, so running the script again changes nothing.
png() { magick -depth 8 -define png:exclude-chunk=all "$@"; }

# place SIZE WIDTH OUT: a transparent SIZE square with the drawing centred, WIDTH percent of the square wide.
place() {
  png -size "${1}x${1}" xc:none \( "$work/art.png" -resize "$(( $1 * $2 / 100 ))x" \) -gravity center -composite "$3"
}

# square SIZE COLOR WIDTH RADIUS OUT: the drawing on a square of COLOR, with corners rounded RADIUS percent.
square() {
  place "$1" "$3" "$work/layer.png"
  r=$(( $1 * $4 / 100 ))
  png -size "${1}x${1}" xc:none -fill "#$2" -draw "roundrectangle 0,0 $(( $1 - 1 )),$(( $1 - 1 )) $r,$r" \
    "$work/layer.png" -composite "$5"
}

# ---- Android

res="$root/androidApp/res"
# density, adaptive layer pixels (108dp), legacy icon pixels (48dp)
for spec in mdpi:108:48 hdpi:162:72 xhdpi:216:96 xxhdpi:324:144 xxxhdpi:432:192; do
  d=${spec%%:*}; rest=${spec#*:}; layer=${rest%%:*}; legacy=${rest#*:}
  dir="$res/mipmap-$d"
  mkdir -p "$dir"
  # The adaptive layers keep the drawing inside the 66 percent safe zone; the launcher's mask may cut the rest.
  place "$layer" 48 "$dir/ic_launcher_foreground.png"
  png "$dir/ic_launcher_foreground.png" -fill black -colorize 100 "$dir/ic_launcher_monochrome.png"
  square "$legacy" "$light" 66 14 "$dir/ic_launcher.png"
  png "$dir/ic_launcher.png" \( -size "${legacy}x${legacy}" xc:black -fill white -draw "circle $(( legacy / 2 )),$(( legacy / 2 )) $(( legacy / 2 )),0" \) \
    -alpha off -compose CopyOpacity -composite "$dir/ic_launcher_round.png"
done

# The background of the adaptive icon follows the wallpaper color from Android 12, and the theme colors before.
write_color() { # DIR COLOR
  mkdir -p "$res/$1"
  printf '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <color name="ic_launcher_background">%s</color>\n</resources>\n' "$2" > "$res/$1/ic_launcher_background.xml"
}
write_color values "#$light"
write_color values-night "#$dark"
write_color values-v31 "@android:color/system_accent1_100"
write_color values-night-v31 "@android:color/system_accent1_800"

for icon in ic_launcher ic_launcher_round; do
  printf '<?xml version="1.0" encoding="utf-8"?>\n<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n    <background android:drawable="@color/ic_launcher_background" />\n    <foreground android:drawable="@mipmap/ic_launcher_foreground" />\n    <monochrome android:drawable="@mipmap/ic_launcher_monochrome" />\n</adaptive-icon>\n' > "$res/mipmap-anydpi-v26/$icon.xml"
done
# The Android build keeps the resources of an earlier build and can miss files that were replaced or removed, so the
# next build starts from the new ones.
rm -rf "$root"/build/tasks/_androidApp_*

# The template's robot is no longer used.
rm -f "$res/drawable/ic_launcher_background.xml" "$res/drawable-v24/ic_launcher_foreground.xml"
rmdir "$res/drawable" "$res/drawable-v24" 2>/dev/null || true

# ---- iOS (full-bleed and without transparency: iOS rounds the corners itself)

icons="$root/iosApp/src/Assets.xcassets/AppIcon.appiconset"
mkdir -p "$icons"
for variant in light dark; do
  eval "color=\$$variant"
  place 1024 62 "$work/ios-$variant.png"
  png -size 1024x1024 "xc:#$color" "$work/ios-$variant.png" -composite -alpha off "$icons/app-icon-$variant-1024.png"
done
mv "$icons/app-icon-light-1024.png" "$icons/app-icon-1024.png"
# The tinted icon is gray on black; the system colors it.
png -size 1024x1024 xc:black "$work/ios-light.png" -composite -colorspace Gray -alpha off "$icons/app-icon-tinted-1024.png"
cat > "$icons/Contents.json" <<'EOF'
{
  "images" : [
    {
      "filename" : "app-icon-1024.png",
      "idiom" : "universal",
      "platform" : "ios",
      "size" : "1024x1024"
    },
    {
      "appearances" : [
        {
          "appearance" : "luminosity",
          "value" : "dark"
        }
      ],
      "filename" : "app-icon-dark-1024.png",
      "idiom" : "universal",
      "platform" : "ios",
      "size" : "1024x1024"
    },
    {
      "appearances" : [
        {
          "appearance" : "luminosity",
          "value" : "tinted"
        }
      ],
      "filename" : "app-icon-tinted-1024.png",
      "idiom" : "universal",
      "platform" : "ios",
      "size" : "1024x1024"
    }
  ],
  "info" : {
    "author" : "xcode",
    "version" : 1
  }
}
EOF

# ---- macOS and the desktop window: a rounded square inside a transparent margin, as macOS app icons are drawn

mkdir -p "$root/desktopApp/resources"
place 824 62 "$work/mac-art.png"
png -size 1024x1024 xc:none -fill "#$light" -draw "roundrectangle 100,100 923,923 185,185" \
  "$work/mac-art.png" -gravity center -composite "$root/desktopApp/resources/app-icon.png"

echo "Icons written for Android, iOS and the desktop from $svg"
