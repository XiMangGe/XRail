#!/usr/bin/env bash
# ============================================================
#  XRail build script (Linux/macOS)
#  Version rule: yy.MM.dd.<daily index>, auto-increment per build.
#  The version is synced into build.sh (VERSION=) and
#  plugin.yml (version:) automatically.
#  Set XRAIL_VERSION to force a specific version (used by CI
#  when building from a vX.Y.Z.W tag).
# ============================================================
set -e

PLUGIN_NAME=XRail
VERSION=26.10.31.1

# ---- compute version ----
if [ -n "${XRAIL_VERSION:-}" ]; then
    VERSION="$XRAIL_VERSION"
else
    VERSION_DATE=$(date +%y.%m.%d)
    VERSION_FILE=.version
    BUILD_NUM=0
    if [ -f "$VERSION_FILE" ]; then
        read -r CDATE CNUM < "$VERSION_FILE" 2>/dev/null || true
        if [ "${CDATE:-}" = "$VERSION_DATE" ]; then
            BUILD_NUM="${CNUM:-0}"
        fi
    fi
    BUILD_NUM=$((BUILD_NUM + 1))
    echo "$VERSION_DATE $BUILD_NUM" > "$VERSION_FILE"
    VERSION="$VERSION_DATE.$BUILD_NUM"
fi

# ---- sync version into plugin.yml and this script ----
# Only numeric literal lines (^VERSION=[0-9.] and ^version: [0-9.])
# are replaced, so the computed assignment VERSION="$VERSION_DATE.$BUILD_NUM"
# above is never touched. Keep comments ASCII-only: build.bat rewrites
# this file with -Encoding Ascii.
if sed --version >/dev/null 2>&1; then
    sed -i "s/^version: [0-9][0-9.]*$/version: $VERSION/" src/main/resources/plugin.yml
    sed -i "s/^VERSION=[0-9][0-9.]*$/VERSION=$VERSION/" build.sh
else
    sed -i '' "s/^version: [0-9][0-9.]*$/version: $VERSION/" src/main/resources/plugin.yml
    sed -i '' "s/^VERSION=[0-9][0-9.]*$/VERSION=$VERSION/" build.sh
fi

echo "========================================"
echo " XRail Builder v$VERSION"
echo "========================================"

# ---- toolchain ----
command -v javac >/dev/null 2>&1 || { echo "[ERROR] javac not found (need JDK 21+)"; exit 1; }
JAR_TOOL="${JDK_BIN:-$JAVA_HOME/bin}/jar"
[ -x "$JAR_TOOL" ] || JAR_TOOL=jar

# ---- dependency classpath (local maven repo, offline friendly) ----
M2="${M2_REPO:-$HOME/.m2/repository}"
PAPER="${PAPER_API:-$M2/io/papermc/paper/paper-api/1.20.4-R0.1-SNAPSHOT/paper-api-1.20.4-R0.1-SNAPSHOT.jar}"
if [ ! -f "$PAPER" ]; then
    echo "[ERROR] paper-api jar not found: $PAPER"
    exit 1
fi
CP="$PAPER"
for a in \
    net/kyori/adventure-api/4.16.0/adventure-api-4.16.0.jar \
    net/kyori/adventure-key/4.16.0/adventure-key-4.16.0.jar \
    net/kyori/examination-api/1.3.0/examination-api-1.3.0.jar \
    net/kyori/option/1.0.0/option-1.0.0.jar \
    net/kyori/adventure-text-serializer-legacy/4.16.0/adventure-text-serializer-legacy-4.16.0.jar \
    org/jetbrains/annotations/24.1.0/annotations-24.1.0.jar \
    com/google/guava/guava/32.1.2-jre/guava-32.1.2-jre.jar \
    com/google/code/gson/gson/2.10.1/gson-2.10.1.jar \
    "net/md-5/bungeecord-chat/1.20-R0.2-deprecated+build.18/bungeecord-chat-1.20-R0.2-deprecated+build.18.jar"; do
    CP="$CP:$M2/$a"
done

# ---- compile & package ----
mkdir -p build/classes dist
find src/main/java -name "*.java" > build/sources.txt
javac --release 17 -encoding UTF-8 -cp "$CP" -d build/classes @build/sources.txt
"$JAR_TOOL" --create --file "build/${PLUGIN_NAME}-${VERSION}.jar" -C build/classes . -C src/main/resources .
cp "build/${PLUGIN_NAME}-${VERSION}.jar" dist/

echo ""
echo "[OK] Built: dist/${PLUGIN_NAME}-${VERSION}.jar"
