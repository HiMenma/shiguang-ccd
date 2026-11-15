#!/usr/bin/env bash
# Create (first run) and boot the project-local emulator.
set -euo pipefail
source "$(dirname "$0")/env.sh"

AVD_NAME="${AVD_NAME:-shiguang}"
SYSTEM_IMAGE="system-images;android-36;google_apis;arm64-v8a"

if ! "$ANDROID_HOME/emulator/emulator" -list-avds | grep -qx "$AVD_NAME"; then
  echo "no" | avdmanager create avd -n "$AVD_NAME" -k "$SYSTEM_IMAGE" -d pixel_6
fi

exec "$ANDROID_HOME/emulator/emulator" \
  -avd "$AVD_NAME" \
  -no-snapshot \
  -no-audio \
  -no-boot-anim \
  -no-window \
  -gpu swiftshader_indirect
