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

APK_SRC="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_SRC" ]; then
    cp "$APK_SRC" "$OUT_DIR/bastion-debug.apk"
    echo "✅ APK generado: $OUT_DIR/bastion-debug.apk"
    ls -la "$OUT_DIR/bastion-debug.apk"
else
    echo "❌ No se encontró el APK en $APK_SRC"
    exit 1
fi
