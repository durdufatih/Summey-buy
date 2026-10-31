#!/usr/bin/env bash
# Android Studio/Gradle olmadan APK derleme (Ubuntu paketleriyle).
# Gerekenler: sudo apt install aapt apksigner zipalign android-sdk-platform-23 dalvik-exchange
#             + derleme sınıf yolu için android.jar (API 30+), ör. Robolectric android-all:
#   ANDROID_JAR=android-all-14-robolectric-10818077.jar ./build-apk-manual.sh
set -euo pipefail
cd "$(dirname "$0")"
: "${ANDROID_JAR:?API 30+ android.jar yolunu ANDROID_JAR ile verin}"
PLAT=/usr/lib/android-sdk/platforms/android-23/android.jar
B=$(mktemp -d)
mkdir -p "$B/gen" "$B/classes" "$B/dex"
sed 's#<manifest xmlns:android="http://schemas.android.com/apk/res/android">#<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.okumakahramani.zombi">#' \
  app/src/main/AndroidManifest.xml > "$B/AndroidManifest.xml"
aapt2 compile --dir app/src/main/res -o "$B/res.zip"
aapt2 link -I "$PLAT" --manifest "$B/AndroidManifest.xml" --min-sdk-version 24 --target-sdk-version 34 \
  --version-code 2 --version-name 1.1 -o "$B/unsigned.apk" --java "$B/gen" "$B/res.zip"
javac -nowarn -encoding UTF-8 --release 8 -cp "$ANDROID_JAR" -d "$B/classes" \
  $(find app/src/main/java -name '*.java') "$B/gen/com/okumakahramani/zombi/R.java"
dalvik-exchange --dex --min-sdk-version=24 --output="$B/dex/classes.dex" "$B/classes"
(cd "$B/dex" && zip -q "$B/unsigned.apk" classes.dex)
zipalign -f 4 "$B/unsigned.apk" "$B/aligned.apk"
mkdir -p apk
apksigner sign --ks debug.keystore --ks-pass pass:android --key-pass pass:android \
  --out apk/zombi-okuma.apk "$B/aligned.apk"
echo "Hazır: apk/zombi-okuma.apk"
