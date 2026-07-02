#!/bin/bash
#
# Rebuilds Asterion (Designed for iPad, on Mac) with a freshly refreshed free
# provisioning profile and replaces /Applications/Asterion.app.
#
# Free Apple "Personal Team" signatures expire after 7 days; run this on a
# schedule (see com.solenoid.asterion.rebuild.plist) to keep the app launchable.
#
# One-time setup:
#   sudo xcode-select -s /Applications/Xcode.app/Contents/Developer
#   chmod +x scripts/rebuild-asterion.sh
#   bash scripts/rebuild-asterion.sh      # test it once, watch the log
#
set -euo pipefail

PROJECT_DIR="/Users/solenoid/Documents/Development/Personal/asterion-ios"
SCHEME="Asterion"
CONFIG="Release"
DERIVED="$PROJECT_DIR/build"
LOG="$HOME/Library/Logs/asterion-rebuild.log"

mkdir -p "$(dirname "$LOG")"
exec >>"$LOG" 2>&1
echo ""
echo "=== $(date '+%Y-%m-%d %H:%M:%S') : starting rebuild ==="

cd "$PROJECT_DIR"

# Build the "Designed for iPad" Mac variant, refreshing the (free) profile.
xcodebuild \
  -project Asterion.xcodeproj \
  -scheme "$SCHEME" \
  -configuration "$CONFIG" \
  -destination 'platform=macOS,variant=Designed for iPad' \
  -derivedDataPath "$DERIVED" \
  -allowProvisioningUpdates \
  build

APP="$(/usr/bin/find "$DERIVED/Build/Products" -maxdepth 2 -name 'Asterion.app' -type d | head -1)"
if [ -z "$APP" ]; then
  echo "ERROR: built Asterion.app not found under $DERIVED/Build/Products"
  exit 1
fi

# Quit the app if it's open, then replace the /Applications copy.
/usr/bin/osascript -e 'tell application "Asterion" to quit' 2>/dev/null || true
sleep 2
/bin/rm -rf "/Applications/Asterion.app"
/bin/cp -R "$APP" "/Applications/Asterion.app"

echo "Deployed: $APP -> /Applications/Asterion.app"
echo "=== $(date '+%Y-%m-%d %H:%M:%S') : done ==="
