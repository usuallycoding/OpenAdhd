#!/bin/sh
# Builds dist/OpenAdhd.ipa, the release app for a real iPhone with no signature. Nobody can install it as it
# is: AltStore, SideStore or Sideloadly sign it with the installer's own (free) Apple ID. See the README.
set -eu

root=$(cd "$(dirname "$0")/.." && pwd)
dist="$root/dist"
work="$root/build/package-ios"
name=OpenAdhd
plist=$root/iosApp/src/Info.plist
app=$work/Build/Products/Release-iphoneos/$name.app

fail() { echo "$1" >&2; exit 1; }

command -v xcodebuild >/dev/null 2>&1 || fail "xcodebuild not found: install Xcode and run: xcode-select -s /Applications/Xcode.app"
xcodebuild -showsdks 2>/dev/null | grep -q -- '-sdk iphoneos' || fail "The iOS SDK is not installed: add it in Xcode under Settings, Components."

# Signing is switched off here on the command line, so the committed project is left as it is.
cd "$root"
rm -rf "$app"
mkdir -p "$work"
log=$work/xcodebuild.log
xcodebuild -project iosApp/module.xcodeproj -scheme iosApp -configuration Release -sdk iphoneos \
  -derivedDataPath "$work" CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO CODE_SIGN_IDENTITY="" build \
  >"$log" 2>&1 || { tail -n 30 "$log" >&2; fail "xcodebuild failed, the full log is $log"; }
[ -f "$app/Info.plist" ] && [ -f "$app/$name" ] || fail "Expected app not found: $app"

rm -rf "$work/ipa" "$dist/$name.ipa"
mkdir -p "$work/ipa/Payload" "$dist"
cp -R "$app" "$work/ipa/Payload/"
(cd "$work/ipa" && zip -qry "$dist/$name.ipa" Payload)

version=$(plutil -extract CFBundleShortVersionString raw "$plist")
build=$(plutil -extract CFBundleVersion raw "$plist")
size=$(wc -c < "$dist/$name.ipa" | tr -d ' ')

echo "Built $dist/$name.ipa (unsigned, version $version, build $build)"
echo "For altstore/source.json:"
echo "  \"version\": \"$version\","
echo "  \"versionDate\": \"$(date +%Y-%m-%d)\","
echo "  \"downloadURL\": \"https://github.com/usuallycoding/OpenAdhd/releases/download/v$version/$name.ipa\","
echo "  \"size\": $size"
