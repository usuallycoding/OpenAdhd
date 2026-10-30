#!/bin/sh
# Builds dist/OpenAdhd.apk. It is the release build, signed with the key in secret/ (see the README for how to
# create it), so a newer APK installs over an older one from any machine that has that key.
set -eu

root=$(cd "$(dirname "$0")/.." && pwd)
dist="$root/dist"
secret="$root/secret"
properties="$secret/android-signing.properties"
apk=$root/build/tasks/_androidApp_buildAndroidRelease/gradle-project-release.apk

fail() { echo "$1" >&2; exit 1; }

# The toolchain only warns when the key is missing and then builds an unsigned APK, so check it here.
[ -f "$properties" ] || fail "Missing $properties. Create the release key first: see Packaging in the README."
for key in storeFile storePassword keyAlias keyPassword; do
  grep -q "^$key=." "$properties" || fail "$properties has no value for $key."
done
store=$(sed -n 's/^storeFile=//p' "$properties")
# storeFile is relative to androidApp/, where module.yaml lives.
case "$store" in /*) ;; *) store="$root/androidApp/$store" ;; esac
[ -f "$store" ] || fail "Keystore not found: $store"

cd "$root"
rm -f "$apk"
./kotlin build -m androidApp -p android -v release
[ -f "$apk" ] || fail "Expected signed APK not found: $apk"

mkdir -p "$dist"
cp "$apk" "$dist/OpenAdhd.apk"

# Check the signature when the Android SDK build tools are around.
sdk=${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}
apksigner=$(ls "$sdk"/build-tools/*/apksigner 2>/dev/null | sort | tail -n 1 || true)
if [ -n "$apksigner" ]; then
  "$apksigner" verify --print-certs "$dist/OpenAdhd.apk" | sed -n 1,2p
else
  echo "apksigner not found, skipped the signature check" >&2
fi

echo "Built $dist/OpenAdhd.apk (install with: adb install -r $dist/OpenAdhd.apk)"
