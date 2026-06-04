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
  grep -E "FATAL EXCEPTION|Process: com\.personalradar\.app|CRASH: com\.personalradar\.app|AndroidRuntime|ActivityTaskManager|Exception|Error" smoke-results/logcat.txt > smoke-results/crash-snippet.txt || true
  exit 1
fi

# Do not treat generic AndroidRuntime entries as crashes: the launcher itself may log
# unrelated AndroidRuntime lines even when the app starts normally. Fail only on
# app-specific crash signatures.
if grep -E "FATAL EXCEPTION|Process: com\.personalradar\.app|CRASH: com\.personalradar\.app" smoke-results/logcat.txt > smoke-results/crash-snippet.txt; then
  echo "App-specific crash signature found in logcat" | tee -a smoke-results/failure.txt
  exit 1
fi

echo "Smoke test passed: app installed, MainActivity launched and process is running." | tee smoke-results/summary.txt
