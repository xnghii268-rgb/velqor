#!/data/data/com.termuxpro.app/files/usr/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "=========================================================="
echo "          VELQOR Local Verification Test Suite            "
echo "=========================================================="

mkdir -p "$PROJECT_ROOT/build/test_classes"
javac -d "$PROJECT_ROOT/build/test_classes" "$PROJECT_ROOT/tools/TestRunner.java"

java -cp "$PROJECT_ROOT/build/test_classes" tools.TestRunner

echo ""
echo "✓ ALL VELQOR ARCHITECTURAL TESTS PASSED (100%)"
