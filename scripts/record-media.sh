#!/usr/bin/env bash
# Record README GIFs from a physical device (not an emulator).
set -euo pipefail

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

record_mode() {
  local mode=$1
  local interact=${2:-}
  adb -s "$DEVICE" shell am force-stop ru.redbyte.clock3d
  sleep 1
  adb -s "$DEVICE" shell am start -n "$PKG" -e CLOCK_MODE "$mode"
  sleep 2.5
  if [[ -n "$interact" ]]; then
    eval "$interact" &
  fi
  adb -s "$DEVICE" shell screenrecord --time-limit 4 "/sdcard/clock_${mode}.mp4"
  adb -s "$DEVICE" pull "/sdcard/clock_${mode}.mp4" "$TMP/${mode}.mp4"
  ffmpeg -y -i "$TMP/${mode}.mp4" -t 4 \
    -vf "fps=12,scale=420:-2:flags=lanczos,split[s0][s1];[s0]palettegen=stats_mode=diff:max_colors=96[p];[s1][p]paletteuse=dither=bayer:bayer_scale=3" \
    "$MEDIA/${mode}.gif"
  echo "Wrote $MEDIA/${mode}.gif"
}

record_mode classic "(sleep 1.2; adb -s $DEVICE shell input tap 540 1100)"
record_mode dali ""
record_mode spheres ""
record_mode ice "(sleep 1.0; adb -s $DEVICE shell input swipe 700 1100 380 1100 400; sleep 0.5; adb -s $DEVICE shell input swipe 380 1100 700 1100 400)"

echo "Done. Device: $DEVICE"
