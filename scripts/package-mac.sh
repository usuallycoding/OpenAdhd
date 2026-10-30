#!/bin/sh
# Builds dist/OpenAdhd.app and dist/OpenAdhd.dmg (own Java runtime bundled, unsigned).
set -eu

root=$(cd "$(dirname "$0")/.." && pwd)
dist="$root/dist"
work="$root/build/package-mac"
name=OpenAdhd
version=1.0.0
mainClass=com.usuallycoding.openadhd.MainKt
jar=$root/build/tasks/_desktopApp_executableJarJvm/desktopApp-jvm-executable.jar

# The app is compiled for Java 25, so the bundled runtime must be 25 or newer. Use JAVA_HOME if it is
# one, otherwise the JDK the Kotlin toolchain downloads for itself.
is_new_enough() { [ -x "$1/bin/jpackage" ] && [ "$("$1/bin/java" -XshowSettings:properties -version 2>&1 | sed -n 's/.*java.specification.version = //p')" -ge 25 ]; }
jdk=""
if [ -n "${JAVA_HOME:-}" ] && is_new_enough "$JAVA_HOME"; then
  jdk=$JAVA_HOME
else
  for candidate in "$HOME"/Library/Caches/JetBrains/Kotlin/extract.cache/*OpenJDK25U*.d/*/Contents/Home; do
    if is_new_enough "$candidate"; then jdk=$candidate; fi
  done
fi
[ -n "$jdk" ] || { echo "No JDK 25 or newer with jpackage found: set JAVA_HOME to one" >&2; exit 1; }
jpackage=$jdk/bin/jpackage

cd "$root"
./kotlin package -m desktopApp -f executable-jar
[ -f "$jar" ] || { echo "Expected jar not found: $jar" >&2; exit 1; }

rm -rf "$work" "$dist/$name.app" "$dist/$name.dmg"
mkdir -p "$work/fat" "$work/input" "$dist"

# The executable jar bundles its libraries, but when two Maven groups ship a jar with the same file name
# (androidx and org.jetbrains.compose both have runtime-desktop-*.jar) it keeps the few-KB redirect stub
# instead of the real jar, and the app then fails with NoClassDefFoundError. Unpack it, put the real jar back
# in place of each stub, and let jpackage start MainKt directly.
unzip -q "$jar" -d "$work/fat"
cp "$work"/fat/BOOT-INF/lib/*.jar "$work/input/"
(cd "$work/input" && for lib in *.jar; do
  if [ "$(wc -c < "$lib")" -lt 20000 ]; then
    real=$(find "$HOME/Library/Caches/JetBrains/Kotlin/.m2.cache" -name "$lib" -size +20k 2>/dev/null | head -n 1)
    [ -n "$real" ] && cp "$real" "$lib" && echo "Replaced stub $lib"
  fi
done)
"$jdk/bin/jar" --create --file "$work/input/app.jar" -C "$work/fat/BOOT-INF/classes" .

# Icon from the rounded square the icon script wrote for the desktop; the app is built without one if this fails.
icon_args=""
iconset="$work/$name.iconset"
mkdir -p "$iconset"
src="$root/desktopApp/resources/app-icon.png"
if [ -f "$src" ] && for s in 16 32 128 256 512; do
  sips -z $s $s "$src" --out "$iconset/icon_${s}x${s}.png" >/dev/null 2>&1 || exit 1
  sips -z $((s * 2)) $((s * 2)) "$src" --out "$iconset/icon_${s}x${s}@2x.png" >/dev/null 2>&1 || exit 1
done && iconutil -c icns "$iconset" -o "$work/$name.icns" >/dev/null 2>&1; then
  icon_args="--icon $work/$name.icns"
else
  echo "Could not make an icon, building without one" >&2
fi

# shellcheck disable=SC2086
"$jpackage" --type app-image --name "$name" --app-version "$version" \
  --input "$work/input" --main-jar app.jar --main-class "$mainClass" \
  --java-options --enable-native-access=ALL-UNNAMED \
  --dest "$dist" $icon_args
# shellcheck disable=SC2086
"$jpackage" --type dmg --name "$name" --app-version "$version" \
  --app-image "$dist/$name.app" --dest "$dist"
mv "$dist/$name-$version.dmg" "$dist/$name.dmg"

echo "Built $dist/$name.app and $dist/$name.dmg"
