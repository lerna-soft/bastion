#!/bin/bash
set -euo pipefail

# ============================================================
# Bastion Release Script
#   Bumps version → builds APK → tags GitHub → 
#   updates latest.json on local server (auto-update)
#
# Usage: ./release.sh [patch|minor|major]
#   patch = 1.1.0 → 1.1.1  (default)
#   minor = 1.1.0 → 1.2.0
#   major = 1.1.0 → 2.0.0
#
# Prerequisites:
#   - Docker image 'bastion-builder' built
#   - JDK at $HOME/dev-tools/jdk-17.0.19+10
#   - Android SDK at $HOME/android-build-env/android-sdk
#   - gh CLI authenticated
#   - git remote 'origin' set to lerna-admin/bastion
# ============================================================

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
GRADLE_FILE="$PROJECT_DIR/platforms/android/build.gradle.kts"
JDK_DIR="$HOME/dev-tools/jdk-17.0.19+10"
SDK_DIR="$HOME/android-build-env/android-sdk"
OUT_DIR="$HOME/apk-share"
GITHUB_REPO="lerna-admin/bastion"

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

# --- Build APK (release: debuggable=false, para que Android no marque la app como
#     'de desarrollo' al instalarla fuera de Play Store; firmada con el mismo keystore) ---
echo "🏗️  Rebuilding bastion-builder image..."
docker build -t bastion-builder "$PROJECT_DIR" > /dev/null

echo "🏗️  Building APK v$NEW_VERSION (release)..."
docker run --rm \
    -v "$PROJECT_DIR:/src" \
    -v "$JDK_DIR:/opt/jdk17" \
    -v "$SDK_DIR:/opt/android-sdk" \
    bastion-builder 2>&1 | tail -5

APK_SRC="$PROJECT_DIR/platforms/android/build/outputs/apk/release/app-release.apk"
if [ ! -f "$APK_SRC" ]; then
    echo "❌ APK build failed — no APK generated"
    exit 1
fi

# --- Copy with versioned name ---
TIMESTAMP=$(date +%Y%m%d-%H%M)
APK_FILENAME="bastion-v${NEW_VERSION}.apk"

mkdir -p "$OUT_DIR"
cp "$APK_SRC" "$OUT_DIR/$APK_FILENAME"
cp "$APK_SRC" "$OUT_DIR/bastion-debug.apk"
FILESIZE=$(stat -c%s "$APK_SRC")

echo "✅ APK built: $OUT_DIR/$APK_FILENAME ($FILESIZE bytes)"
echo ""

# --- Tag & Push to GitHub (NO APK upload — repo is private) ---
echo "🏷️  Tagging v$NEW_VERSION..."
git tag -a "v$NEW_VERSION" -m "Bastion v$NEW_VERSION"
git push origin "v$NEW_VERSION"
git push origin master

echo "🚀 Creating GitHub release (notes only)..."
gh release create "v$NEW_VERSION" \
    --title "Bastion v$NEW_VERSION" \
    --notes "## Bastion v${NEW_VERSION}\n\n### Changelog\n- See commits: https://github.com/${GITHUB_REPO}/compare/v${CURRENT_VERSION}...v${NEW_VERSION}\n\n### Download\nAPK disponible en servidor local: http://192.168.0.100:8765/apk-share/${APK_FILENAME}" \
    --repo "$GITHUB_REPO" 2>&1

echo "✅ Release created: https://github.com/$GITHUB_REPO/releases/tag/v$NEW_VERSION"

# --- Update latest.json (served by serve.py at /update) ---
DOWNLOAD_URL="http://192.168.0.100:8765/apk-share/${APK_FILENAME}"
cat > "$OUT_DIR/latest.json" << ENDJSON
{"update":true,"versionName":"${NEW_VERSION}","versionCode":${NEW_CODE},"downloadUrl":"${DOWNLOAD_URL}","fileName":"${APK_FILENAME}","timestamp":"${TIMESTAMP}","fileSize":${FILESIZE},"changelog":"Bastion v${NEW_VERSION} — see GitHub release notes"}
ENDJSON

echo "📝 latest.json updated:"
cat "$OUT_DIR/latest.json"
echo ""

# --- Restart local server ---
fuser -k 8765/tcp 2>/dev/null || true
sleep 1
nohup python3 "$PROJECT_DIR/serve.py" > /dev/null 2>&1 &

echo ""
echo "✅ Release complete: v$CURRENT_VERSION → v$NEW_VERSION"
echo "   APK:    $DOWNLOAD_URL"
echo "   GitHub: https://github.com/$GITHUB_REPO/releases/tag/v$NEW_VERSION"
echo "   Update: http://192.168.0.100:8765/update"
