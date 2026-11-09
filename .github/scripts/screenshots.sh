#!/usr/bin/env bash
# Takes the release screenshots on a running emulator.
#
# Usage: screenshots.sh <debug.apk> <output dir>
#
# Opens each screen with `am start` and saves PNGs. The emulator's logcat is saved to
# build-logs/screenshots/ for debugging, and on any error the current screen and activity
# stack are captured too. (No TikTok videos are captured: they'd need the network and
# show other people's content.)

set -Eeuo pipefail

APK="$1"
OUT="$2"
PKG="com.toktiklite"
LOGS="build-logs/screenshots"
mkdir -p "$OUT" "$LOGS"

on_error() {
  local line=$1 cmd=$2 code=$3
  echo "::error title=Screenshot script failed::line ${line}: ${cmd} (exit ${code})"
  adb exec-out screencap -p > "$LOGS/failure-screen.png" 2>/dev/null || true
  adb shell dumpsys activity activities > "$LOGS/failure-activities.txt" 2>/dev/null || true
  {
    echo "### ❌ Screenshots failed"
    echo "Line ${line}: \`${cmd}\` (exit ${code})."
    echo "The **screenshots-logs** artifact has logcat, the screen at the time of failure, and the activity stack."
  } >> "${GITHUB_STEP_SUMMARY:-/dev/null}"
}
trap 'on_error "${LINENO}" "${BASH_COMMAND}" "$?"' ERR

echo "::group::Wait for boot"
adb wait-for-device
for _ in $(seq 1 90); do
  [[ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]] && break
  sleep 2
done
[[ "$(adb shell getprop sys.boot_completed | tr -d '\r')" == "1" ]]
adb shell getprop ro.build.version.release
adb shell wm size
echo "::endgroup::"

adb logcat -c || true
adb logcat -v time > "$LOGS/logcat.txt" 2>&1 &
LOGCAT_PID=$!
trap 'kill "$LOGCAT_PID" 2>/dev/null || true' EXIT

echo "::group::Install"
adb install -r -g "$APK"
echo "::endgroup::"

echo "::group::Clean status bar"
adb shell settings put global sysui_demo_allowed 1 || true
adb shell am broadcast -a com.android.systemui.demo -e command enter > /dev/null || true
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0700 > /dev/null || true
adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false > /dev/null || true
adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 > /dev/null || true
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false > /dev/null || true
echo "::endgroup::"

clear_dialogs() {
  adb shell input keyevent KEYCODE_WAKEUP || true
  adb shell wm dismiss-keyguard || true
  adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS > /dev/null || true
}

launch() {
  local activity=$1; shift
  clear_dialogs
  adb shell am force-stop "$PKG"
  adb shell am start -W -n "$PKG/$activity" "$@"
  sleep 3
}

shot() {
  local name=$1
  local focus=""
  for attempt in 1 2 3 4; do
    focus="$(adb shell dumpsys window | grep -m1 mCurrentFocus | tr -d '\r' || true)"
    echo "Focus: $focus"
    [[ "$focus" == *"$PKG"* ]] && break
    echo "Not in front yet (attempt $attempt); clearing dialogs and waiting."
    clear_dialogs
    sleep 3
  done
  if [[ "$focus" != *"$PKG"* ]]; then
    echo "::error title=Wrong screen::$name: expected $PKG in focus, got: $focus"
    return 1
  fi
  adb exec-out screencap -p > "$OUT/$name.png"
  local size
  size=$(stat -c %s "$OUT/$name.png")
  if (( size < 10000 )); then
    echo "::error title=Bad screenshot::$name.png is only $size bytes"
    return 1
  fi
  echo "Captured $name.png ($size bytes)"
}

echo "::group::Home"
launch .HomeActivity
shot 01-home
echo "::endgroup::"

echo "Screenshots:"
ls -la "$OUT"
