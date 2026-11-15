#!/usr/bin/env bash
# Install the debug APK, launch the app, grab a screenshot and surface crashes.
set -euo pipefail
source "$(dirname "$0")/env.sh"

APK="${1:-$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk}"
OUT="${2:-$PROJECT_DIR/docs/screenshots/smoke.png}"

adb wait-for-device
while [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" != "1" ]; do
  sleep 2
done

adb install -r "$APK"
adb shell pm grant app.shiguang.ccd android.permission.CAMERA || true
adb shell am force-stop app.shiguang.ccd || true
adb shell am start -n app.shiguang.ccd/.MainActivity
sleep 4
mkdir -p "$(dirname "$OUT")"
adb exec-out screencap -p > "$OUT"
echo "screenshot: $OUT"

if adb logcat -d | grep -q "FATAL EXCEPTION"; then
  echo "--- crash log ---"
  adb logcat -d | grep -A 20 "FATAL EXCEPTION" | tail -40
  exit 1
fi
echo "no fatal exception"
