#!/usr/bin/env bash
# Iron Man Stage 3 helmet, scan and countermeasure sounds: own synthesis with
# ffmpeg only. JARVIS voice lines are not synthesised here: they are CC0
# recordings from Codex-Superheroes (see CREDITS.md), copied by
# tools/sfx/ironman_stage3.sh --jarvis CODEX_SOUNDS_DIR.
# Regenerate:
#   tools/sfx/ironman_stage3.sh [OUT_DIR]
#   tools/sfx/ironman_stage3.sh --jarvis /path/to/codex/assets/superheroes/sounds/ironman [OUT_DIR]
# Default OUT_DIR writes base64 files for decodeBinaryAssets. Mono 44.1 kHz
# Ogg Vorbis (mono is needed for positional sounds in Minecraft).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
JARVIS=""
if [[ "${1:-}" == "--jarvis" ]]; then
  JARVIS="$2"
  shift 2
fi
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

if [[ -n "$JARVIS" ]]; then
  # Re-encode to mono 44.1 kHz so all lines match; names follow sounds.json.
  for name in jarvis_detect jarvis_detect_excited jarvis_diagnostic jarvis_mark85_preset; do
    ffmpeg -hide_banner -loglevel error -y -i "$JARVIS/$name.ogg" -ac 1 -ar "$SR" \
      -c:a libvorbis -q:a 5 "$TMP/$name.ogg"
  done
else
# Helmet close (0.6 s): plates sliding in, a servo whir and a sealing clunk.
render helmet_close 0.6 "
aevalsrc='0.25*sin(2*PI*(500*t+900*t*t))*min(1,t*8)*max(0,1-t*2)':s=$SR:d=0.6[servo];
anoisesrc=c=white:a=0.5:s=$SR:d=0.6,highpass=f=2500,volume='0.45*(0.5+0.5*sin(2*PI*70*t))*max(0,1-t*2.2)':eval=frame[plates];
aevalsrc='if(gt(t,0.42),0.8*sin(2*PI*140*(t-0.42))*exp(-28*(t-0.42))+0.3*sin(2*PI*1200*(t-0.42))*exp(-45*(t-0.42)),0)':s=$SR:d=0.6[clunk];
[servo][plates][clunk]amix=inputs=3:normalize=0,alimiter=limit=0.9[out]"

# Helmet open (0.55 s): unlock hiss, then plates folding back.
render helmet_open 0.55 "
anoisesrc=c=white:a=0.6:s=$SR:d=0.55,highpass=f=3000,volume='0.6*exp(-14*t)':eval=frame[hiss];
aevalsrc='0.22*sin(2*PI*(1400*t-1100*t*t))*min(1,t*10)*exp(-3*t)':s=$SR:d=0.55[servo];
anoisesrc=c=white:a=0.4:s=$SR:d=0.55,highpass=f=2500,volume='if(gt(t,0.1),0.4*(0.5+0.5*sin(2*PI*65*t))*max(0,1-(t-0.1)*2.5),0)':eval=frame[plates];
[hiss][servo][plates]amix=inputs=3:normalize=0,alimiter=limit=0.9[out]"

# Scan loop (1.5 s, seamless): soft sweeping digital hum with ticks.
render scan_loop 1.5 "
aevalsrc='0.16*sin(2*PI*660*t+3*sin(2*PI*(2/1.5)*t))+0.08*sin(2*PI*1320*t)':s=$SR:d=1.5[hum];
aevalsrc='0.2*sin(2*PI*2400*t)*gt(sin(2*PI*8*t),0.95)':s=$SR:d=1.5[ticks];
[hum][ticks]amix=inputs=2:normalize=0,alimiter=limit=0.7[out]"

# Scan complete (0.6 s): rising three-note confirmation chime.
render scan_complete 0.6 "
aevalsrc='0.35*sin(2*PI*1046*t)*between(t,0,0.12)*exp(-8*t)+0.35*sin(2*PI*1318*t)*between(t,0.12,0.24)+0.4*sin(2*PI*1568*t)*gt(t,0.24)*exp(-6*(t-0.24))':s=$SR:d=0.6[chime];
[chime]afade=t=out:st=0.5:d=0.1,alimiter=limit=0.85[out]"

# Flare launch (0.6 s): burst of pops and a hiss.
render flare_launch 0.6 "
anoisesrc=c=white:a=0.8:s=$SR:d=0.6,bandpass=f=2000:w=2500,volume='0.8*gt(sin(2*PI*14*t),0.3)*exp(-3*t)':eval=frame[pops];
aevalsrc='0.5*sin(2*PI*(160*t-80*t*t))*exp(-12*t)':s=$SR:d=0.6[thump];
[pops][thump]amix=inputs=2:normalize=0,afade=t=out:st=0.45:d=0.15,alimiter=limit=0.9[out]"

# Flare burn (1.0 s): crackling fizz.
render flare_burn 1.0 "
anoisesrc=c=white:a=0.5:s=$SR:d=1.0,highpass=f=3000,volume='0.35+0.3*gt(sin(2*PI*31*t+2*sin(2*PI*3*t)),0.7)':eval=frame[fizz];
anoisesrc=c=pink:a=0.4:s=$SR:d=1.0,lowpass=f=1200,volume=0.3[body];
[fizz][body]amix=inputs=2:normalize=0,afade=t=in:d=0.05,afade=t=out:st=0.8:d=0.2,alimiter=limit=0.8[out]"
fi

for f in "$TMP"/*.ogg; do
  name="$(basename "$f")"
  if [[ "$OUT" == *binassets* ]]; then
    base64 -w 76 "$f" > "$OUT/$name.b64"
  else
    cp "$f" "$OUT/$name"
  fi
done
echo "wrote $(ls "$TMP" | wc -l) sounds to $OUT"
