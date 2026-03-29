#!/bin/bash
# ─────────────────────────────────────────────────────────────
#  GitLite VCS — Build & Run Script
#  Requires: Java 17+  (check with: java -version)
# ─────────────────────────────────────────────────────────────

set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC_DIR="$PROJECT_DIR/src/main/java"
OUT_DIR="$PROJECT_DIR/out"
MAIN_CLASS="gitlite.Main"

echo ""
echo "  ╔══════════════════════════════════════════╗"
echo "  ║     GitLite VCS  —  Build & Run          ║"
echo "  ╚══════════════════════════════════════════╝"
echo ""

# ── 1. Check Java ─────────────────────────────────────────────
if ! command -v javac &> /dev/null; then
    echo "  ✗  javac not found. Install JDK 17+:"
    echo "     https://adoptium.net"
    exit 1
fi

JAVA_VER=$(javac -version 2>&1 | awk '{print $2}' | cut -d'.' -f1)
echo "  ✓  Java $JAVA_VER detected."

# ── 2. Compile ────────────────────────────────────────────────
echo "  ⟳  Compiling..."
mkdir -p "$OUT_DIR"
find "$SRC_DIR" -name "*.java" > /tmp/sources.txt
javac --release 17 -d "$OUT_DIR" @/tmp/sources.txt
echo "  ✓  Compilation successful."
echo ""

# ── 3. Run ────────────────────────────────────────────────────
MODE="${1:-interactive}"

if [ "$MODE" = "demo" ]; then
    echo "  ▶  Running automated demo..."
    echo ""
    java -cp "$OUT_DIR" "$MAIN_CLASS" demo
else
    echo "  ▶  Starting interactive shell..."
    echo "     (run './build.sh demo' for automated demo)"
    echo ""
    java -cp "$OUT_DIR" "$MAIN_CLASS"
fi
