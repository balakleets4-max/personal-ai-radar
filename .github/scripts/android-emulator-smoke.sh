#!/usr/bin/env bash
set -euo pipefail

mkdir -p smoke-results

echo "apk=${APK_PATH:-}" > smoke-results/environment.txt
adb devices -l | tee -a smoke-results/environment.txt
adb shell getprop ro.build.version.sdk | tee -a smoke-results/environment.txt

adb logcat -c
adb install -r "$APK_PATH"
adb shell monkey -p com.personalradar.app -c android.intent.category.LAUNCHER 1
sleep 12

adb shell pidof com.personalradar.app > smoke-results/app.pid || true
adb shell screencap -p /sdcard/smoke-screenshot.png || true
adb pull /sdcard/smoke-screenshot.png smoke-results/smoke-screenshot.png || true
adb logcat -d > smoke-results/logcat.txt || true

if [ ! -s smoke-results/app.pid ]; then
  echo "App process is not running after launch" | tee smoke-results/failure.txt
  grep -E "FATAL EXCEPTION|Process: com\.personalradar\.app|CRASH: com\.personalradar\.app|Exception|Error" smoke-results/logcat.txt > smoke-results/crash-snippet.txt || true
  exit 1
fi

# Do not treat generic AndroidRuntime entries as crashes: the monkey launcher itself logs
# AndroidRuntime lines even when the app starts normally. Fail only on app-specific crash
# signatures.
if grep -E "FATAL EXCEPTION|Process: com\.personalradar\.app|CRASH: com\.personalradar\.app" smoke-results/logcat.txt > smoke-results/crash-snippet.txt; then
  echo "App-specific crash signature found in logcat" | tee -a smoke-results/failure.txt
  exit 1
fi

echo "Smoke test passed: app installed, launched and process is running." | tee smoke-results/summary.txt
