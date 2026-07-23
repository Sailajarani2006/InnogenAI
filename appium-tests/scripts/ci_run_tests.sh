#!/bin/bash
set -e

echo "[CI] Starting Appium Test Runner Script..."

# Inject GITHUB_PATH into current PATH so node binaries work properly
if [ -f "$GITHUB_PATH" ]; then
    while IFS= read -r line; do
        export PATH="$line:$PATH"
    done < "$GITHUB_PATH"
fi

# The path to the built APK is passed as an env var or argument
APK_PATH=${APK_PATH:-"../app/build/outputs/apk/debug/app-debug.apk"}

if [ ! -f "$APK_PATH" ]; then
    echo "ERROR: APK not found at $APK_PATH"
    # Execute fallback if APK doesn't exist
    node utils/generateFallbackReport.js
    exit 1
fi

echo "[CI] Installing APK to Emulator: $APK_PATH"
adb install -r "$APK_PATH"

echo "[CI] Starting Appium Server..."
# Run appium in the background
npx appium --log-level warn > /tmp/appium.log 2>&1 &
APPIUM_PID=$!

echo "[CI] Waiting for Appium server to boot on port 4723..."
timeout 60 bash -c 'until curl -s http://localhost:4723/status > /dev/null; do sleep 1; done' || (echo "Appium failed to start"; cat /tmp/appium.log; exit 1)

echo "[CI] Appium is ready! Executing WebDriverIO Massive Test Suite..."
# If WDIO crashes, we catch the exit code and run the fallback report script
set +e
node node_modules/@wdio/cli/bin/wdio.js run wdio.conf.js
WDIO_EXIT=$?
set -e

if [ $WDIO_EXIT -ne 0 ]; then
    echo "[CI] WebdriverIO returned non-zero exit code ($WDIO_EXIT). Ensuring fallback artifacts exist."
    node utils/generateFallbackReport.js
fi

echo "[CI] Killing Appium..."
kill $APPIUM_PID || true

echo "[CI] Script Completed."
exit $WDIO_EXIT
