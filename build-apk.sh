#!/bin/bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
JDK_DIR="$HOME/dev-tools/jdk-17.0.19+10"
SDK_DIR="$HOME/android-build-env/android-sdk"
OUT_DIR="$HOME/apk-share"

if [ ! -d "$JDK_DIR" ]; then
    echo "❌ JDK no encontrado en $JDK_DIR"
    exit 1
fi

if [ ! -d "$SDK_DIR" ]; then
    echo "❌ Android SDK no encontrado en $SDK_DIR"
    exit 1
fi

mkdir -p "$OUT_DIR"

echo "🔧 Construyendo imagen Docker..."
docker build -t bastion-builder "$PROJECT_DIR"

echo "🏗️  Compilando APK..."
docker run --rm \
    -v "$PROJECT_DIR:/src" \
    -v "$JDK_DIR:/opt/jdk17" \
    -v "$SDK_DIR:/opt/android-sdk" \
    bastion-builder

APK_SRC="$PROJECT_DIR/platforms/android/build/outputs/apk/release/android-release.apk"
if [ -f "$APK_SRC" ]; then
    # Extract version from build.gradle.kts
    VERSION=$(grep 'versionName' "$PROJECT_DIR/platforms/android/build.gradle.kts" | sed 's/.*"\(.*\)".*/\1/')
    VERSIONCODE=$(grep 'versionCode' "$PROJECT_DIR/platforms/android/build.gradle.kts" | grep -o '[0-9]\+')
    TIMESTAMP=$(date +%Y%m%d-%H%M)
    VERSIONED_NAME="bastion-v${VERSION}-${TIMESTAMP}.apk"

    cp "$APK_SRC" "$OUT_DIR/bastion-debug.apk"
    cp "$APK_SRC" "$OUT_DIR/$VERSIONED_NAME"

    # Generate latest.json for auto-update
    DOWNLOAD_URL="http://192.168.0.100:8765/apk-share/${VERSIONED_NAME}"
    FILESIZE=$(stat -c%s "$APK_SRC" 2>/dev/null || stat -f%z "$APK_SRC" 2>/dev/null)
    cat > "$OUT_DIR/latest.json" << ENDJSON
{"update":true,"versionName":"${VERSION}","versionCode":${VERSIONCODE},"downloadUrl":"${DOWNLOAD_URL}","fileName":"${VERSIONED_NAME}","timestamp":"${TIMESTAMP}","fileSize":${FILESIZE},"changelog":"Bug fixes and improvements"}
ENDJSON

    echo "✅ APK generado:"
    echo "   $OUT_DIR/bastion-debug.apk"
    echo "   $OUT_DIR/$VERSIONED_NAME"
    echo "   $OUT_DIR/latest.json"
    ls -la "$OUT_DIR/bastion-debug.apk"
else
    echo "❌ No se encontró el APK en $OUT_DIR/bastion-debug.apk"
    exit 1
fi

# Start log server (kill previous instance first)
fuser -k 8765/tcp 2>/dev/null || true
nohup python3 "$PROJECT_DIR/serve.py" > /dev/null 2>&1 &
echo "📡 Log server started on port 8765"
