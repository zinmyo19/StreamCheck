#!/bin/bash
# Manual APK build for StreamCheck v1 (no Gradle) — pure Java, zero deps
set -euo pipefail
export JAVA_HOME=$HOME/jdk17
export PATH=$JAVA_HOME/bin:$PATH

SDK=~/android-sdk
BT=$SDK/build-tools/34.0.0
AAPT2=$BT/aapt2
D8=$BT/d8
ZIPALIGN=$BT/zipalign
APKSIGNER=$BT/apksigner
ANDROID_JAR=$SDK/platforms/android-34/android.jar
KEYSTORE=~/workspace/shizuku-build/manual/debug.keystore

PROJ=~/workspace/stream-check
M=$PROJ/app/src/main
OUT=/tmp/stream-check-out
chmod -R u+w $OUT 2>/dev/null || true
rm -rf $OUT && mkdir -p $OUT/compiled $OUT/gen $OUT/classes $OUT/dex

VER_CODE=7
VER_NAME="1.6"
APK_NAME="StreamCheck-1.6.apk"

echo "=== [1/5] aapt2 compile + link ==="
$AAPT2 compile --dir "$M/res" -o "$OUT/compiled.zip"
$AAPT2 link -o "$OUT/base.apk" -I "$ANDROID_JAR" \
  --manifest "$M/AndroidManifest.xml" \
  "$OUT/compiled.zip" \
  --java "$OUT/gen" \
  --min-sdk-version 26 --target-sdk-version 34 \
  --version-code $VER_CODE --version-name "$VER_NAME"

echo "=== [2/5] javac ==="
find "$M/java" -name "*.java" > "$OUT/srcs.txt"
find "$OUT/gen" -name "*.java" >> "$OUT/srcs.txt"
javac -encoding UTF-8 -source 17 -target 17 -nowarn \
  -cp "$ANDROID_JAR" -d "$OUT/classes" @"$OUT/srcs.txt"

echo "=== [3/5] d8 ==="
$D8 --min-api 26 --lib "$ANDROID_JAR" --output "$OUT/dex" \
  $(find "$OUT/classes" -name "*.class")

echo "=== [4/5] package + align + sign ==="
cp "$OUT/base.apk" "$OUT/unsigned.apk"
(cd "$OUT/dex" && zip -q "$OUT/unsigned.apk" classes*.dex)
$ZIPALIGN -p -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"
OUT_APK=~/workspace/user/files/$APK_NAME
$APKSIGNER sign --ks "$KEYSTORE" --ks-pass pass:android \
  --ks-key-alias androiddebugkey --out "$OUT_APK" "$OUT/aligned.apk"

echo "=== [5/5] verify ==="
$ZIPALIGN -c -p 4 "$OUT_APK" && echo "zipalign OK"
$APKSIGNER verify --print-certs "$OUT_APK" | grep -E "Signer #1|SHA-256" | head -2
ls -la "$OUT_APK"
$BT/aapt dump badging "$OUT_APK" | head -2
echo "BUILD DONE: $APK_NAME"
