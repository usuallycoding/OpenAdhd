#!/bin/sh
# Builds dist/OpenAdhd.apk. It is the debug build, signed with the Android debug key of this machine
# (~/.android/debug.keystore), so a newer APK installs over an older one and keeps its data.
set -eu

root=$(cd "$(dirname "$0")/.." && pwd)
dist="$root/dist"
apk=$root/build/tasks/_androidApp_buildAndroidDebug/gradle-project-debug.apk

cd "$root"
./kotlin build -m androidApp -p android -v debug
[ -f "$apk" ] || { echo "Expected APK not found: $apk" >&2; exit 1; }

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
