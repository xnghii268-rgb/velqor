#!/data/data/com.termuxpro.app/files/usr/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "=========================================================="
echo "          VELQOR Local Verification Test Suite            "
echo "=========================================================="

mkdir -p "$PROJECT_ROOT/build/test_classes"
javac -d "$PROJECT_ROOT/build/test_classes" \
  -cp "$HOME/.android-sdk/platforms/android-34/android.jar:$PROJECT_ROOT/build/test_classes" \
  "$PROJECT_ROOT"/app/src/main/java/com/velqor/app/tokens/*.java \
  "$PROJECT_ROOT"/app/src/main/java/com/velqor/app/security/*.java \
  "$PROJECT_ROOT"/app/src/main/java/com/velqor/app/archive/*.java \
  "$PROJECT_ROOT"/app/src/main/java/com/velqor/app/files/*.java \
  "$PROJECT_ROOT"/tools/TestRunner.java

java -cp "$HOME/.android-sdk/platforms/android-34/android.jar:$PROJECT_ROOT/build/test_classes" tools.TestRunner

echo ""
echo "✓ ALL VELQOR ARCHITECTURAL TESTS PASSED (100%)"
