#!/usr/bin/env bash
# Stop the headless emulator started by scripts/emulator.sh.
set -euo pipefail
source "$(dirname "$0")/env.sh"

AVD_NAME="${AVD_NAME:-shiguang}"

# `adb emu kill` authenticates with ~/.emulator_console_auth_token, but this
# project boots the emulator with HOME=$PROJECT/.home, so the tokens differ and
# the console command fails with "authentication token does not match".
adb -s emulator-5554 emu kill >/dev/null 2>&1 || true

# Reliable fallback: signal the QEMU process directly.
pkill -f "qemu-system-aarch64-headless.*-avd ${AVD_NAME}" 2>/dev/null || true

for _ in $(seq 1 20); do
  if ! pgrep -f "qemu-system-aarch64-headless.*-avd ${AVD_NAME}" >/dev/null 2>&1; then
    echo "emulator stopped"
    exit 0
  fi
  sleep 0.5
done

echo "emulator still running; sending SIGKILL"
pkill -9 -f "qemu-system-aarch64-headless.*-avd ${AVD_NAME}" 2>/dev/null || true
