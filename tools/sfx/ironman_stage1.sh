#!/usr/bin/env bash
# Only the two flight loops (thruster_loop, thruster_sonic) are still synthesized here; every other
# Iron Man sound comes from CC0 recordings (tools/sfx/ironman_cc0.py).
# Iron Man Stage 1 sounds: own synthesis with ffmpeg only (spec §17: no
# vanilla or third-party sounds). Regenerate:
#   tools/sfx/ironman_stage1.sh [OUT_DIR]
# Default OUT_DIR writes base64 files for decodeBinaryAssets. Mono 44.1 kHz
# Ogg Vorbis (mono is needed for positional sounds in Minecraft).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="${1:-$ROOT/viltrumitecore/src/main/binassets/assets/viltrumitecore/sounds/ironman}"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
mkdir -p "$OUT"
SR=44100

render() { # name duration filter_complex (must end in [out])
  local name="$1" dur="$2" graph="$3"
  ffmpeg -hide_banner -loglevel error -y -filter_complex "$graph" -map "[out]" \
    -t "$dur" -ac 1 -ar "$SR" -c:a libvorbis -q:a 5 "$TMP/$name.ogg"
}

# Nano deploy (1.0 s): rising metallic shimmer, scale clicks, closing clack.
render thruster_loop 2.0 "
anoisesrc=c=brown:a=0.9:s=$SR:d=2.0,lowpass=f=700,volume=1.6[rumble];
anoisesrc=c=pink:a=0.35:s=$SR:d=2.0,bandpass=f=2200:w=1800,volume=0.5[hiss];
aevalsrc='0.06*sin(2*PI*110*t)+0.04*sin(2*PI*220*t)':s=$SR:d=2.0[hum];
[rumble][hiss][hum]amix=inputs=3:normalize=0,alimiter=limit=0.8[out]"

# Thruster sonic layer (2.0 s, seamless): high whine with fast flutter.
render thruster_sonic 2.0 "
anoisesrc=c=white:a=0.5:s=$SR:d=2.0,highpass=f=1800,lowpass=f=7000,volume='0.5*(0.8+0.2*sin(2*PI*16*t))':eval=frame[air];
aevalsrc='0.12*sin(2*PI*2000*t+0.6*sin(2*PI*8*t))+0.07*sin(2*PI*3000*t)':s=$SR:d=2.0[whine];
[air][whine]amix=inputs=2:normalize=0,alimiter=limit=0.8[out]"

# Soft landing (0.45 s): boot thump with a short metallic clink.