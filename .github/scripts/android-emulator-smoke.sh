#!/usr/bin/env bash
set -euo pipefail

mkdir -p smoke-results

PACKAGE_NAME="com.personalradar.app"
MAIN_ACTIVITY="com.personalradar.app/.MainActivity"

echo "apk=${APK_PATH:-}" > smoke-results/environment.txt
adb devices -l | tee -a smoke-results/environment.txt
adb shell getprop ro.build.version.sdk | tee -a smoke-results/environment.txt
adb shell getprop ro.product.model | tee -a smoke-results/environment.txt

adb logcat -c
adb install -r "$APK_PATH" | tee smoke-results/install.txt
adb shell cmd package resolve-activity --brief "$PACKAGE_NAME" | tee smoke-results/resolve-activity.txt || true
adb shell am start -W -n "$MAIN_ACTIVITY" | tee smoke-results/launch.txt || true

for i in $(seq 1 30); do
  if adb shell pidof "$PACKAGE_NAME" > smoke-results/app.pid; then
    break
  fi
  sleep 1
done

adb shell screencap -p /sdcard/smoke-screenshot.png || true
adb pull /sdcard/smoke-screenshot.png smoke-results/smoke-screenshot.png || true
adb shell dumpsys activity activities > smoke-results/activity-dump.txt || true
adb shell dumpsys window windows > smoke-results/window-dump.txt || true
adb logcat -d > smoke-results/logcat.txt || true

if [ ! -s smoke-results/app.pid ]; then
  echo "App process is not running after deterministic MainActivity launch" | tee smoke-results/failure.txt
  grep -E "FATAL EXCEPTION|Process: ${PACKAGE_NAME}|CRASH: ${PACKAGE_NAME}|AndroidRuntime|ActivityTaskManager|Exception|Error" smoke-results/logcat.txt > smoke-results/crash-snippet.txt || true
  cat smoke-results/crash-snippet.txt || true
  exit 1
fi

# Fail only on crashes that explicitly belong to the app process. Generic emulator,
# launcher or monkey AndroidRuntime/FATAL lines are noisy on CI and must not fail the
# smoke test while the app process is alive.
if grep -E "Process: ${PACKAGE_NAME}|CRASH: ${PACKAGE_NAME}" smoke-results/logcat.txt > smoke-results/crash-snippet.txt; then
  echo "App-specific crash signature found in logcat" | tee -a smoke-results/failure.txt
  cat smoke-results/crash-snippet.txt || true
  exit 1
fi

echo "Smoke test passed: app installed, MainActivity launched and process is running." | tee smoke-results/summary.txt
