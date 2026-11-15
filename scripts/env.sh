#!/usr/bin/env bash
# Source this file before any Gradle / adb / emulator command.
#   source scripts/env.sh
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export PROJECT_DIR

# The DSH sandbox makes ~/.gradle and ~/.android read-only, so every writable
# state directory lives inside the project instead.
export GRADLE_USER_HOME="$PROJECT_DIR/.gradle-home"
export ANDROID_USER_HOME="$PROJECT_DIR/.android-home"
export ANDROID_AVD_HOME="$PROJECT_DIR/.android-home/avd"

# Prefer the project-local SDK when it exists: the shared SDK in
# ~/Library/Android/sdk has an incomplete API 36 system image, so we install a
# working one under .android-sdk (tools are symlinked to the shared install).
if [ -x "$PROJECT_DIR/.android-sdk/emulator/emulator" ]; then
  export ANDROID_HOME="$PROJECT_DIR/.android-sdk"
else
  export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
fi
export ANDROID_SDK_ROOT="$ANDROID_HOME"

export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"

if [ -x "$PROJECT_DIR/gradlew" ]; then
  export GRADLE_CMD="$PROJECT_DIR/gradlew"
else
  GRADLE_CACHED="$(ls -d "$HOME"/.gradle/wrapper/dists/gradle-8.14-all/*/gradle-8.14/bin/gradle 2>/dev/null | head -1 || true)"
  export GRADLE_CMD="${GRADLE_CACHED:-gradle}"
fi

if [ -z "${JAVA_HOME:-}" ]; then
  export JAVA_HOME="$(/usr/libexec/java_home -v 17 2>/dev/null || true)"
fi
