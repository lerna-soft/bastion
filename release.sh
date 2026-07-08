#!/bin/bash
set -euo pipefail

# ============================================================
# Bastion Release Script
#   Bumps version → builds APK → creates GitHub release → 
#   updates latest.json for auto-update
#
# Usage: ./release.sh [patch|minor|major]
#   patch = 1.1.0 → 1.1.1  (default)
#   minor = 1.1.0 → 1.2.0
#   major = 1.1.0 → 2.0.0
# ============================================================

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
GRADLE_FILE="$PROJECT_DIR/app/build.gradle.kts"
JDK_DIR="$HOME/dev-tools/jdk-17.0.19+10"
SDK_DIR="$HOME/android-build-env/android-sdk"
OUT_DIR="$HOME/apk-share"
GITHUB_REPO="lerna-admin/bastion"
SERVER_UPDATE_URL="http://192.168.0.100:8765/update"

BUMP_TYPE="${1:-patch}"

# --- Read current version ---
CURRENT_VERSION=$(grep 'versionName' "$GRADLE_FILE" | sed 's/.*"\(.*\)".*/\1/')
CURRENT_CODE=$(grep 'versionCode' "$GRADLE_FILE" | grep -o '[0-9]\+')

echo "📦 Current version: $CURRENT_VERSION (code $CURRENT_CODE)"

# --- Bump version ---
IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT_VERSION"
case "$BUMP_TYPE" in
  major) MAJOR=$((MAJOR + 1)); MINOR=0; PATCH=0 ;;
  minor) MINOR=$((MINOR + 1)); PATCH=0 ;;
  patch) PATCH=$((PATCH + 1)) ;;
  *) echo "❌ Unknown bump type: $BUMP_TYPE"; exit 1 ;;
esac
NEW_VERSION="$MAJOR.$MINOR.$PATCH"
NEW_CODE=$((CURRENT_CODE + 1))

echo "🔖 New version: $NEW_VERSION (code $NEW_CODE)"
echo ""

# --- Update build.gradle.kts ---
sed -i "s/versionCode = $CURRENT_CODE/versionCode = $NEW_CODE/" "$GRADLE_FILE"
sed -i "s/versionName = \"$CURRENT_VERSION\"/versionName = \"$NEW_VERSION\"/" "$GRADLE_FILE"

git add "$GRADLE_FILE"
git commit -m "chore: bump version to v$NEW_VERSION (code $NEW_CODE)"

# --- Build APK ---
echo "🏗️  Building APK v$NEW_VERSION..."
docker run --rm \
    -v "$PROJECT_DIR:/src" \
    -v "$JDK_DIR:/opt/jdk17" \
    -v "$SDK_DIR:/opt/android-sdk" \
    bastion-builder 2>&1 | tail -5

APK_SRC="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
if [ ! -f "$APK_SRC" ]; then
    echo "❌ APK build failed — no APK generated"
    exit 1
fi

# --- Copy with versioned name ---
TIMESTAMP=$(date +%Y%m%d-%H%M)
APK_FILENAME="bastion-v${NEW_VERSION}.apk"
VERSIONED_NAME="bastion-v${NEW_VERSION}-${TIMESTAMP}.apk"

mkdir -p "$OUT_DIR"
cp "$APK_SRC" "$OUT_DIR/$APK_FILENAME"
cp "$APK_SRC" "$OUT_DIR/$VERSIONED_NAME"
FILESIZE=$(stat -c%s "$APK_SRC")

echo "✅ APK built: $OUT_DIR/$APK_FILENAME ($FILESIZE bytes)"
echo ""

# --- Create GitHub Release ---
echo "🏷️  Tagging v$NEW_VERSION..."
git tag -a "v$NEW_VERSION" -m "Bastion v$NEW_VERSION"
git push origin "v$NEW_VERSION"
git push origin master

echo "🚀 Creating GitHub release..."
RELEASE_URL=$(gh release create "v$NEW_VERSION" \
    --title "Bastion v$NEW_VERSION" \
    --notes "## Bastion v$NEW_VERSION\n\nSee [CHANGELOG](CHANGELOG.md) for details." \
    "$OUT_DIR/$APK_FILENAME#Bastion APK (debug)" \
    --repo "$GITHUB_REPO" 2>&1)

echo "✅ Release created: $RELEASE_URL"

# --- Update latest.json ---
DOWNLOAD_URL="https://github.com/$GITHUB_REPO/releases/download/v$NEW_VERSION/$APK_FILENAME"
cat > "$OUT_DIR/latest.json" << ENDJSON
{"update":true,"versionName":"${NEW_VERSION}","versionCode":${NEW_CODE},"downloadUrl":"${DOWNLOAD_URL}","fileName":"${APK_FILENAME}","timestamp":"${TIMESTAMP}","fileSize":${FILESIZE},"changelog":"Bastion v${NEW_VERSION} release"}
ENDJSON

# Push latest.json to server's update endpoint if the server supports it
# Otherwise it's already readable from $OUT_DIR/latest.json which serve.py serves at /update
echo "📝 latest.json updated:"
cat "$OUT_DIR/latest.json"
echo ""

# --- Restart log server to pick up new latest.json ---
fuser -k 8765/tcp 2>/dev/null || true
sleep 1
nohup python3 "$PROJECT_DIR/serve.py" > /dev/null 2>&1 &

echo "✅ Done! v$CURRENT_VERSION → v$NEW_VERSION"
echo "   APK:    $DOWNLOAD_URL"
echo "   Update: $SERVER_UPDATE_URL"
echo "   GitHub: $RELEASE_URL"
