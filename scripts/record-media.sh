#!/usr/bin/env bash
# Record README GIFs from a physical device (not an emulator).
set -euo pipefail

DURATION="${DURATION:-7}"
WARMUP_SEC="${WARMUP_SEC:-4}"
# Skip leading black / splash after screenrecord starts (tune if needed).
TRIM_START_SEC="${TRIM_START_SEC:-0.35}"

DEVICE="${DEVICE:-}"
if [[ -z "$DEVICE" ]]; then
  DEVICE="$(adb devices -l | awk '/model:SM_/ {print $1; exit}')"
fi
if [[ -z "$DEVICE" ]] || [[ "$DEVICE" == emulator-* ]]; then
  echo "Connect a Samsung (or other physical) device. Set DEVICE=serial if several are attached."
  exit 1
fi

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PKG="ru.redbyte.clock3d/.MainActivity"
TMP="/tmp/clock_record"
MEDIA="$ROOT/docs/media"
mkdir -p "$TMP" "$MEDIA"

adb -s "$DEVICE" install -r "$ROOT/app/build/outputs/apk/debug/app-debug.apk"
adb -s "$DEVICE" shell input keyevent KEYCODE_WAKEUP 2>/dev/null || true

to_gif() {
  local raw=$1
  local gif=$2
  # Trim start (black frames), fixed length, drop tail by not reading past DURATION.
  ffmpeg -y -ss "$TRIM_START_SEC" -i "$raw" -t "$DURATION" \
    -vf "fps=12,scale=420:-2:flags=lanczos,split[s0][s1];[s0]palettegen=stats_mode=diff:max_colors=96[p];[s1][p]paletteuse=dither=bayer:bayer_scale=3" \
    "$gif"
}

record_mode() {
  local mode=$1
  local interact=${2:-}
  adb -s "$DEVICE" shell am force-stop ru.redbyte.clock3d
  sleep 0.8
  adb -s "$DEVICE" shell am start -n "$PKG" -e CLOCK_MODE "$mode"
  sleep "$WARMUP_SEC"
  if [[ -n "$interact" ]]; then
    eval "$interact" &
  fi
  # Slightly longer than needed; we trim to exactly DURATION in ffmpeg.
  local limit=$((DURATION + 2))
  adb -s "$DEVICE" shell screenrecord --time-limit "$limit" "/sdcard/clock_${mode}.mp4"
  adb -s "$DEVICE" pull "/sdcard/clock_${mode}.mp4" "$TMP/${mode}.mp4"
  to_gif "$TMP/${mode}.mp4" "$MEDIA/${mode}.gif"
  echo "Wrote $MEDIA/${mode}.gif (${DURATION}s)"
}

record_mode classic "(sleep 1.5; adb -s $DEVICE shell input tap 540 1100; sleep 2.5; adb -s $DEVICE shell input tap 540 1100)"
record_mode dali ""
record_mode spheres ""
record_mode ice "(sleep 1.2; for i in 1 2 3; do adb -s $DEVICE shell input swipe 700 1100 380 1100 500; sleep 0.8; adb -s $DEVICE shell input swipe 380 1100 700 1100 500; sleep 0.8; done)"

echo "Done. Device: $DEVICE"
