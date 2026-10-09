#!/usr/bin/env bash
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
render nano_deploy 1.0 "
aevalsrc='0.35*sin(2*PI*(260*t+700*t*t))*(1-exp(-12*t))*exp(-1.2*t)+0.18*sin(2*PI*(520*t+1400*t*t))*exp(-2*t)':s=$SR:d=1.0[sweep];
anoisesrc=c=white:a=0.6:s=$SR:d=1.0,highpass=f=2500,lowpass=f=9000,volume='0.6*(0.5+0.5*sin(2*PI*38*t))*if(lt(t,0.85),t/0.85,0)':eval=frame[clicks];
aevalsrc='if(gt(t,0.86),0.7*sin(2*PI*1150*(t-0.86))*exp(-40*(t-0.86))+0.5*sin(2*PI*160*(t-0.86))*exp(-25*(t-0.86)),0)':s=$SR:d=1.0[clack];
[sweep][clicks][clack]amix=inputs=3:normalize=0,afade=t=out:st=0.94:d=0.06,alimiter=limit=0.9[out]"

# Nano retract (0.9 s): falling shimmer flowing back, soft thump into the reactor.
render nano_retract 0.9 "
aevalsrc='0.35*sin(2*PI*(1500*t-650*t*t))*(1-exp(-20*t))*exp(-1.5*t)':s=$SR:d=0.9[sweep];
anoisesrc=c=white:a=0.5:s=$SR:d=0.9,highpass=f=3000,lowpass=f=8000,volume='0.5*(0.5+0.5*sin(2*PI*30*t))*max(0,1-t/0.8)':eval=frame[clicks];
aevalsrc='if(gt(t,0.78),0.6*sin(2*PI*90*(t-0.78))*exp(-30*(t-0.78)),0)':s=$SR:d=0.9[thump];
[sweep][clicks][thump]amix=inputs=3:normalize=0,afade=t=out:st=0.84:d=0.06,alimiter=limit=0.9[out]"

# Thruster loop (2.0 s, seamless): roaring low jet + hiss, stationary noise.
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
render landing_soft 0.45 "
aevalsrc='0.8*sin(2*PI*70*t)*exp(-18*t)+0.25*sin(2*PI*1800*t)*exp(-30*t)+0.15*sin(2*PI*2650*t)*exp(-35*t)':s=$SR:d=0.45[a];
anoisesrc=c=brown:a=0.6:s=$SR:d=0.45,lowpass=f=400,volume='exp(-14*t)':eval=frame[b];
[a][b]amix=inputs=2:normalize=0,alimiter=limit=0.9[out]"

# Heavy (superhero) landing (1.3 s): deep boom, debris crash, armor ring.
render landing_heavy 1.3 "
aevalsrc='0.9*sin(2*PI*(55*t-8*t*t))*exp(-4*t)+0.3*sin(2*PI*740*t)*exp(-6*t)+0.2*sin(2*PI*1110*t)*exp(-7*t)':s=$SR:d=1.3[boom];
anoisesrc=c=brown:a=0.9:s=$SR:d=1.3,lowpass=f=1200,volume='1.2*exp(-5*t)':eval=frame[crash];
anoisesrc=c=white:a=0.3:s=$SR:d=1.3,highpass=f=3000,volume='if(gt(t,0.05),0.4*exp(-9*(t-0.05)),0)':eval=frame[debris];
[boom][crash][debris]amix=inputs=3:normalize=0,afade=t=out:st=1.1:d=0.2,alimiter=limit=0.95[out]"

# Air strike (1.1 s): falling whoosh, then the impact boom.
render air_strike 1.1 "
anoisesrc=c=pink:a=0.7:s=$SR:d=1.1,bandpass=f=900:w=1200,volume='if(lt(t,0.35),t/0.35,max(0,1-(t-0.35)*8))':eval=frame[whoosh];
aevalsrc='if(gt(t,0.35),1.0*sin(2*PI*(48*(t-0.35)))*exp(-5*(t-0.35))+0.25*sin(2*PI*620*(t-0.35))*exp(-8*(t-0.35)),0)':s=$SR:d=1.1[boom];
anoisesrc=c=brown:a=0.9:s=$SR:d=1.1,lowpass=f=900,volume='if(gt(t,0.35),1.1*exp(-6*(t-0.35)),0)':eval=frame[crash];
[whoosh][boom][crash]amix=inputs=3:normalize=0,afade=t=out:st=0.95:d=0.15,alimiter=limit=0.95[out]"

# Fly-by hit (0.35 s): armored shoulder slam, metal ring.
render flyby_hit 0.35 "
anoisesrc=c=white:a=0.8:s=$SR:d=0.35,lowpass=f=3500,volume='exp(-25*t)':eval=frame[smack];
aevalsrc='0.5*sin(2*PI*95*t)*exp(-16*t)+0.3*sin(2*PI*930*t)*exp(-12*t)+0.18*sin(2*PI*1395*t)*exp(-14*t)':s=$SR:d=0.35[ring];
[smack][ring]amix=inputs=2:normalize=0,alimiter=limit=0.9[out]"

for f in "$TMP"/*.ogg; do
  name="$(basename "$f")"
  if [[ "$OUT" == *binassets* ]]; then
    base64 -w 76 "$f" > "$OUT/$name.b64"
  else
    cp "$f" "$OUT/$name"
  fi
done
echo "wrote $(ls "$TMP" | wc -l) sounds to $OUT"
