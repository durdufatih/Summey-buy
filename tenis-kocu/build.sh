#!/usr/bin/env bash
# Tenis Koçu APK'sını Gradle olmadan derler (Ubuntu/Debian Android araçlarıyla).
# Gerekli paketler: sudo apt-get install android-sdk-platform-23 aapt dalvik-exchange apksigner zipalign
# Ayrıca: JDK (javac), curl, unzip, python3
set -euo pipefail
cd "$(dirname "$0")"

ANDROID_JAR=${ANDROID_JAR:-/usr/lib/android-sdk/platforms/android-23/android.jar}
TFLITE_VER=2.16.1
MEDIAPIPE_VER=0.10.21
B=build
C=$B/cache
mkdir -p "$C"

# --- Bağımlılıklar (Maven Central + PyPI) ---
for a in tensorflow-lite tensorflow-lite-api; do
  if [ ! -f "$C/$a/classes.jar" ]; then
    curl -fsSL -o "$C/$a.aar" "https://repo1.maven.org/maven2/org/tensorflow/$a/$TFLITE_VER/$a-$TFLITE_VER.aar"
    rm -rf "$C/$a" && mkdir -p "$C/$a" && unzip -q -o "$C/$a.aar" -d "$C/$a"
  fi
done
if [ ! -f "$C/assets/pose_landmark_full.tflite" ]; then
  url=$(curl -fsSL https://pypi.org/simple/mediapipe/ | grep -o "https://[^\"#]*mediapipe-$MEDIAPIPE_VER-cp312-cp312-manylinux_2_28_x86_64.whl" | head -1)
  curl -fsSL -o "$C/mediapipe.whl" "$url"
  mkdir -p "$C/assets"
  unzip -q -o -j "$C/mediapipe.whl" \
    mediapipe/modules/pose_detection/pose_detection.tflite \
    mediapipe/modules/pose_landmark/pose_landmark_full.tflite -d "$C/assets"
  rm -f "$C/mediapipe.whl"
fi
LIBS="$C/tensorflow-lite-api/classes.jar:$C/tensorflow-lite/classes.jar"

# --- Kaynaklar + R.java ---
rm -rf $B/gen $B/classes $B/apk && mkdir -p $B/gen $B/classes $B/apk
aapt package -f -m -M AndroidManifest.xml -S res -I "$ANDROID_JAR" -J $B/gen

# --- Java derleme ---
javac -nowarn -Xlint:-options -encoding UTF-8 -source 8 -target 8 \
  -bootclasspath "$ANDROID_JAR" -cp "$LIBS" -d $B/classes \
  $(find src $B/gen -name '*.java')

# --- DEX ---
dalvik-exchange --dex --min-sdk-version=26 --output=$B/apk/classes.dex \
  $B/classes "$C/tensorflow-lite-api/classes.jar" "$C/tensorflow-lite/classes.jar"

# --- Paketleme ---
aapt package -f -M AndroidManifest.xml -S res -A "$C/assets" -I "$ANDROID_JAR" -0 tflite \
  -F $B/unsigned.apk
for abi in arm64-v8a armeabi-v7a; do
  mkdir -p $B/apk/lib/$abi
  cp "$C/tensorflow-lite/jni/$abi/libtensorflowlite_jni.so" $B/apk/lib/$abi/
done
(cd $B/apk && aapt add -f ../unsigned.apk classes.dex lib/arm64-v8a/libtensorflowlite_jni.so lib/armeabi-v7a/libtensorflowlite_jni.so >/dev/null)

zipalign -f 4 $B/unsigned.apk $B/aligned.apk
apksigner sign --ks tenis-debug.keystore --ks-pass pass:android --ks-key-alias tenisdebug \
  --key-pass pass:android --out $B/TenisKocu.apk $B/aligned.apk
mkdir -p apk && cp $B/TenisKocu.apk apk/TenisKocu.apk
echo "APK hazır: $(pwd)/apk/TenisKocu.apk ($(du -h apk/TenisKocu.apk | cut -f1))"
