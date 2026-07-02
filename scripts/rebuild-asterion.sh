#!/bin/bash
#
# Rebuilds Asterion as a native Mac Catalyst app, signed to run locally (ad-hoc),
# and replaces /Applications/Asterion.app.
#
# Because the app is ad-hoc signed ("sign to run locally"), it does NOT expire —
# you only need to run this after you change the code and want to redeploy.
#
# Requires full Xcode:  sudo xcode-select -s /Applications/Xcode.app/Contents/Developer
# Run:                  bash scripts/rebuild-asterion.sh
#
set -euo pipefail

PROJECT_DIR="/Users/solenoid/Documents/Development/Personal/asterion-ios"
SCHEME="Asterion"
CONFIG="Release"
DERIVED="$PROJECT_DIR/build-catalyst"
LOG="$HOME/Library/Logs/asterion-rebuild.log"

mkdir -p "$(dirname "$LOG")"
exec >>"$LOG" 2>&1
echo ""
echo "=== $(date '+%Y-%m-%d %H:%M:%S') : starting Mac Catalyst rebuild ==="

cd "$PROJECT_DIR"

xcodebuild \
  -project Asterion.xcodeproj \
  -scheme "$SCHEME" \
  -configuration "$CONFIG" \
  -destination 'platform=macOS,variant=Mac Catalyst' \
  -derivedDataPath "$DERIVED" \
  CODE_SIGN_IDENTITY="-" CODE_SIGN_STYLE=Manual DEVELOPMENT_TEAM="" \
  PROVISIONING_PROFILE_SPECIFIER="" REGISTER_APP_GROUPS=NO \
  build

APP="$(/usr/bin/find "$DERIVED/Build/Products" -maxdepth 2 -name 'Asterion.app' -type d | head -1)"
if [ -z "$APP" ]; then
  echo "ERROR: built Asterion.app not found under $DERIVED/Build/Products"
  exit 1
fi

/usr/bin/osascript -e 'tell application "Asterion" to quit' 2>/dev/null || true
sleep 2
/bin/rm -rf "/Applications/Asterion.app"
/bin/cp -R "$APP" "/Applications/Asterion.app"

echo "Deployed: $APP -> /Applications/Asterion.app"
echo "=== $(date '+%Y-%m-%d %H:%M:%S') : done ==="
