#!/bin/bash
set -euo pipefail

XTERM_CORE_VERSION="6.0.0"
XTERM_ADDON_FIT_VERSION="0.11.0"
ASSETS_DIR="app/src/main/assets/terminal"
mkdir -p "$ASSETS_DIR"

echo "⬇️  Downloading xterm.js v${XTERM_CORE_VERSION}..."

CORE_URL="https://unpkg.com/@xterm/xterm@${XTERM_CORE_VERSION}"
curl -sL "${CORE_URL}/lib/xterm.js" -o "${ASSETS_DIR}/xterm.js"
curl -sL "${CORE_URL}/css/xterm.css" -o "${ASSETS_DIR}/xterm.css"
curl -sL "${CORE_URL}/lib/xterm.js.map" -o "${ASSETS_DIR}/xterm.js.map" 2>/dev/null || true

FIT_URL="https://unpkg.com/@xterm/addon-fit@${XTERM_ADDON_FIT_VERSION}"
curl -sL "${FIT_URL}/lib/addon-fit.js" -o "${ASSETS_DIR}/xterm-addon-fit.js"
curl -sL "${FIT_URL}/lib/addon-fit.js.map" -o "${ASSETS_DIR}/xterm-addon-fit.js.map" 2>/dev/null || true

echo "✅  xterm.js assets downloaded to ${ASSETS_DIR}"
ls -la "$ASSETS_DIR"
