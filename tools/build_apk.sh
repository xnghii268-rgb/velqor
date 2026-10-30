#!/data/data/com.termuxpro.app/files/usr/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
BUILD_DIR="$PROJECT_ROOT/build"
ANDROID_JAR="$HOME/.android-sdk/platforms/android-34/android.jar"
KEYSTORE="$BUILD_DIR/debug.keystore"
APK_OUT="/sdcard/AntiGravity/velqor-v2.3.0.apk"

mkdir -p "$BUILD_DIR/aapt_res" "$BUILD_DIR/gen_r" "$BUILD_DIR/classes" "$BUILD_DIR/dex"

aapt2 compile --dir "$PROJECT_ROOT/app/src/main/res" -o "$BUILD_DIR/aapt_res/compiled.zip"

aapt2 link -I "$ANDROID_JAR" \
  --manifest "$PROJECT_ROOT/app/src/main/AndroidManifest.xml" \
  --min-sdk-version 26 \
  --target-sdk-version 34 \
  --version-code 20 \
  --version-name "2.3.0" \
  --java "$BUILD_DIR/gen_r" \
  -o "$BUILD_DIR/resources_v230.apk" \
  "$BUILD_DIR/aapt_res/compiled.zip" \
  --auto-add-overlay

rm -rf "$BUILD_DIR/classes"/*
javac -d "$BUILD_DIR/classes" \
  -cp "$ANDROID_JAR:$BUILD_DIR/classes:$BUILD_DIR/libs/*" \
  "$BUILD_DIR/gen_r/com/velqor/app/R.java" \
  "$PROJECT_ROOT/app/src/main/java/com/velqor/app"/*.java \
  "$PROJECT_ROOT/app/src/main/java/com/velqor/app/tokens"/*.java \
  "$PROJECT_ROOT/app/src/main/java/com/velqor/app/archive"/*.java \
  "$PROJECT_ROOT/app/src/main/java/com/velqor/app/files"/*.java \
  "$PROJECT_ROOT/app/src/main/java/com/velqor/app/ui"/*.java

rm -rf "$BUILD_DIR/dex"/*
find "$BUILD_DIR/classes" -name "*.class" | xargs d8 --release --min-api 26 --output "$BUILD_DIR/dex" "$BUILD_DIR/libs"/shizuku-*.jar

cp "$BUILD_DIR/resources_v230.apk" "$BUILD_DIR/velqor_unaligned_230.apk"
jar -uf "$BUILD_DIR/velqor_unaligned_230.apk" -C "$BUILD_DIR/dex" classes.dex

rm -f "$APK_OUT"
apksigner sign --ks "$KEYSTORE" --ks-pass pass:android --key-pass pass:android --out "$APK_OUT" "$BUILD_DIR/velqor_unaligned_230.apk"

cp -f "$APK_OUT" "/sdcard/AntiGravity/velqor-v2.2.0.apk"
cp -f "$APK_OUT" "/sdcard/AntiGravity/velqor-v2.1.2.apk"

echo "Build and signed: $APK_OUT"
