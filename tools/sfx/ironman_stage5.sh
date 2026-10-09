#!/usr/bin/env bash
# Iron Man Stage 5 sounds (Hulkbuster Mark 48: drop, assembly, exit, break,
# steps, servos, punches, jackhammer, grab, throw, slam, hop): own synthesis.
# Regenerate:
#   tools/sfx/ironman_stage5.sh [OUT_DIR]
# Default OUT_DIR writes base64 files for decodeBinaryAssets. Mono 44.1 kHz
# Ogg Vorbis. Each sound is scaled to TARGET_MEAN dB (the level of the stage 1-3
# sounds) and its decoded peak is kept at or below -1.5 dB (Vorbis overshoot).
# alimiter runs with level=disabled (its default auto-level changes the mix) and
# delays the output by its 5 ms look-ahead, so fades that must reach zero go last.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="${1:-$ROOT/viltrumitecore/src/main/binassets/assets/viltrumitecore/sounds/ironman}"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
mkdir -p "$OUT" "$TMP/raw"
SR=44100
TARGET_MEAN=-15

normalise() { # in out: mean at TARGET_MEAN, decoded peak at or below -1.5 dB
  local stats mean peak gain decoded extra
  stats="$(ffmpeg -hide_banner -i "$1" -af volumedetect -f null - 2>&1)"
  mean="$(sed -n 's/.*mean_volume: \(-\{0,1\}[0-9.]*\) dB.*/\1/p' <<<"$stats")"
  peak="$(sed -n 's/.*max_volume: \(-\{0,1\}[0-9.]*\) dB.*/\1/p' <<<"$stats")"
  gain="$(awk -v m="$mean" -v p="$peak" -v t="$TARGET_MEAN" 'BEGIN { g = t - m; if (g > -2 - p) g = -2 - p; printf "%.2f", g }')"
  ffmpeg -hide_banner -loglevel error -y -i "$1" -af "volume=${gain}dB" -c:a libvorbis -q:a 5 "$2.try.ogg"
  decoded="$(ffmpeg -hide_banner -i "$2.try.ogg" -af volumedetect -f null - 2>&1 | sed -n 's/.*max_volume: \(-\{0,1\}[0-9.]*\) dB.*/\1/p')"
  extra="$(awk -v d="$decoded" 'BEGIN { e = -1.5 - d; if (e > 0) e = 0; printf "%.2f", e }')"
  ffmpeg -hide_banner -loglevel error -y -i "$1" -af "volume=$(awk -v g="$gain" -v e="$extra" 'BEGIN { printf "%.2f", g + e }')dB" -c:a libvorbis -q:a 5 "$2"
  rm -f "$2.try.ogg"
}

render() { # name duration filter_complex (must end in [out])
  local name="$1" dur="$2" graph="$3"
  ffmpeg -hide_banner -loglevel error -y -filter_complex "$graph" -map "[out]" \
    -t "$dur" -ac 1 -ar "$SR" -c:a libvorbis -q:a 5 "$TMP/raw/$name.ogg"
  normalise "$TMP/raw/$name.ogg" "$TMP/$name.ogg"
}

# Veronica pod: whistle rises then falls, rumble grows as it comes closer (2.5 s).

# Parts drop: heavy metal chunks falling with a whoosh and a landing clank (1.5 s).
render hulkbuster_drop 1.5 "
anoisesrc=c=pink:a=0.7:s=$SR:d=1.5:seed=31,bandpass=f=500:width_type=o:w=2,volume='min(1,t*3)*max(0,1-(t-0.9)*2)':eval=frame[whoosh];
aevalsrc='if(gt(t,1.0),0.8*sin(2*PI*(55*(t-1)+30*(1-exp(-4*(t-1)))))*exp(-5*(t-1))+0.3*sin(2*PI*620*(t-1))*exp(-14*(t-1)),0)':s=$SR:d=1.5[land];
[whoosh][land]amix=inputs=2:normalize=0,afade=t=out:st=1.35:d=0.15,alimiter=limit=0.9:level=disabled[out]"

# Assembly: four big clanks locking together over a servo whine (1.5 s).
render hulkbuster_assemble 1.5 "
aevalsrc='0.12*sin(2*PI*(220*t+80*t*t))*min(1,t*4)':s=$SR:d=1.5[servo];
aevalsrc='0.75*(gt(t,0.1)*sin(2*PI*90*(t-0.1))*exp(-18*(t-0.1))+gt(t,0.45)*sin(2*PI*80*(t-0.45))*exp(-18*(t-0.45))+gt(t,0.8)*sin(2*PI*95*(t-0.8))*exp(-18*(t-0.8))+gt(t,1.15)*sin(2*PI*70*(t-1.15))*exp(-12*(t-1.15)))':s=$SR:d=1.5[clanks];
aevalsrc='0.22*(gt(t,0.1)*sin(2*PI*1180*(t-0.1))*exp(-30*(t-0.1))+gt(t,0.45)*sin(2*PI*1040*(t-0.45))*exp(-30*(t-0.45))+gt(t,0.8)*sin(2*PI*1260*(t-0.8))*exp(-30*(t-0.8))+gt(t,1.15)*sin(2*PI*980*(t-1.15))*exp(-25*(t-1.15)))':s=$SR:d=1.5[ring];
[servo][clanks][ring]amix=inputs=3:normalize=0,afade=t=out:st=1.4:d=0.1,alimiter=limit=0.9:level=disabled[out]"

# Exit: hydraulic hiss and the back hatch swinging open (1.2 s).
render hulkbuster_exit 1.2 "
anoisesrc=c=white:a=0.6:s=$SR:d=1.2:seed=33,highpass=f=1800,volume='0.6*min(1,t*5)*max(0,1-(t-0.6)*1.6)':eval=frame[hiss];
aevalsrc='0.2*sin(2*PI*(140*t+40*t*t))*min(1,t*3)*max(0,1-(t-0.7)*2)':s=$SR:d=1.2[hinge];
aevalsrc='if(gt(t,0.95),0.6*sin(2*PI*120*(t-0.95))*exp(-20*(t-0.95)),0)':s=$SR:d=1.2[stop];
[hiss][hinge][stop]amix=inputs=3:normalize=0,afade=t=out:st=1.1:d=0.1,alimiter=limit=0.9:level=disabled[out]"

# Break: a huge metal collapse with sparks and falling plates (2.0 s).
render hulkbuster_break 2.0 "
aevalsrc='0.9*sin(2*PI*(35*t+30*(1-exp(-3*t))))*exp(-1.6*t)':s=$SR:d=2.0[boom];
anoisesrc=c=pink:a=0.8:s=$SR:d=2.0:seed=34,lowpass=f=1400,volume='exp(-1.8*t)':eval=frame[crash];
anoisesrc=c=white:a=0.5:s=$SR:d=2.0:seed=35,highpass=f=4000,volume='0.4*exp(-3*t)*gt(sin(2*PI*17*t)*sin(2*PI*29*t),0.5)':eval=frame[sparks];
aevalsrc='0.25*(gt(t,0.5)*sin(2*PI*700*(t-0.5))*exp(-12*(t-0.5))+gt(t,0.9)*sin(2*PI*560*(t-0.9))*exp(-12*(t-0.9))+gt(t,1.3)*sin(2*PI*830*(t-1.3))*exp(-12*(t-1.3)))':s=$SR:d=2.0[plates];
[boom][crash][sparks][plates]amix=inputs=4:normalize=0,alimiter=limit=0.9:level=disabled[out]"

# Step: very heavy metal footstep (0.45 s).
render hulkbuster_step 0.45 "
aevalsrc='0.9*sin(2*PI*(48*t+25*(1-exp(-12*t))))*exp(-11*t)':s=$SR:d=0.45[thud];
anoisesrc=c=pink:a=0.5:s=$SR:d=0.45:seed=36,lowpass=f=600,volume='exp(-16*t)':eval=frame[grit];
aevalsrc='0.12*sin(2*PI*410*t)*exp(-20*t)':s=$SR:d=0.45[metal];
[thud][grit][metal]amix=inputs=3:normalize=0,alimiter=limit=0.9:level=disabled[out]"

# Servo: short heavy servo whine (0.5 s).
render hulkbuster_servo 0.5 "
aevalsrc='0.3*sin(2*PI*(180*t+260*t*t))*min(1,t*12)*max(0,1-(t-0.35)*6)+0.08*sin(2*PI*(540*t+780*t*t))*min(1,t*12)*max(0,1-(t-0.35)*6)':s=$SR:d=0.5[out]"

# Punch: huge deep metal impact (0.7 s).
render hulkbuster_punch 0.7 "
aevalsrc='0.95*sin(2*PI*(42*t+40*(1-exp(-10*t))))*exp(-6*t)':s=$SR:d=0.7[body];
anoisesrc=c=pink:a=0.8:s=$SR:d=0.7:seed=37,lowpass=f=1600,volume='exp(-14*t)':eval=frame[smack];
aevalsrc='0.2*sin(2*PI*520*t)*exp(-16*t)+0.12*sin(2*PI*1150*t)*exp(-22*t)':s=$SR:d=0.7[ring];
[body][smack][ring]amix=inputs=3:normalize=0,alimiter=limit=0.9:level=disabled[out]"

# Jackhammer: one hard piston hit, played every 4 ticks (0.2 s).
render hulkbuster_jackhammer 0.2 "
aevalsrc='0.9*sin(2*PI*85*t)*exp(-30*t)+0.3*sin(2*PI*640*t)*exp(-45*t)':s=$SR:d=0.2[hit];
anoisesrc=c=white:a=0.5:s=$SR:d=0.2:seed=38,bandpass=f=2500:width_type=o:w=1.5,volume='exp(-40*t)':eval=frame[clack];
[hit][clack]amix=inputs=2:normalize=0,alimiter=limit=0.9:level=disabled[out]"

# Grab: metal clamp closing with a servo groan (0.7 s).
render hulkbuster_grab 0.7 "
aevalsrc='0.25*sin(2*PI*(120*t+60*t*t))*min(1,t*8)*max(0,1-(t-0.3)*3)':s=$SR:d=0.7[groan];
aevalsrc='if(gt(t,0.28),0.75*sin(2*PI*150*(t-0.28))*exp(-22*(t-0.28))+0.25*sin(2*PI*900*(t-0.28))*exp(-35*(t-0.28)),0)':s=$SR:d=0.7[clamp];
[groan][clamp]amix=inputs=2:normalize=0,alimiter=limit=0.9:level=disabled[out]"

# Throw: heavy swing whoosh with a servo lurch (0.8 s).
render hulkbuster_throw 0.8 "
anoisesrc=c=pink:a=0.8:s=$SR:d=0.8:seed=39,bandpass=f=400:width_type=o:w=2,volume='sin(PI*min(1,t/0.6))':eval=frame[whoosh];
aevalsrc='0.18*sin(2*PI*(160*t+220*t*t))*exp(-3*t)':s=$SR:d=0.8[servo];
[whoosh][servo]amix=inputs=2:normalize=0,afade=t=out:st=0.7:d=0.1,alimiter=limit=0.9:level=disabled[out]"

# Slam: massive ground impact with a long rumble (2.0 s).
render hulkbuster_slam 2.0 "
aevalsrc='0.95*sin(2*PI*(26*t+35*(1-exp(-4*t))))*exp(-1.5*t)':s=$SR:d=2.0[boom];
anoisesrc=c=brown:a=0.9:s=$SR:d=2.0:seed=40,lowpass=f=500,volume='exp(-1.4*t)':eval=frame[rumble];
anoisesrc=c=pink:a=0.6:s=$SR:d=2.0:seed=41,lowpass=f=2000,volume='exp(-5*t)':eval=frame[crack];
[boom][rumble][crack]amix=inputs=3:normalize=0,alimiter=limit=0.9:level=disabled[out]"

# Hop: short heavy thruster burst (0.8 s).
render hulkbuster_hop 0.8 "
anoisesrc=c=brown:a=0.9:s=$SR:d=0.8:seed=42,lowpass=f=900,volume='min(1,t*14)*max(0,1-(t-0.45)*2.8)':eval=frame[thrust];
anoisesrc=c=white:a=0.4:s=$SR:d=0.8:seed=43,highpass=f=3000,volume='0.5*min(1,t*14)*exp(-4*t)':eval=frame[hiss];
aevalsrc='0.4*sin(2*PI*60*t)*exp(-6*t)':s=$SR:d=0.8[pop];
[thrust][hiss][pop]amix=inputs=3:normalize=0,alimiter=limit=0.9:level=disabled[out]"

for f in "$TMP"/*.ogg; do
  name="$(basename "$f")"
  if [[ "$OUT" == *binassets* ]]; then
    base64 -w 76 "$f" > "$OUT/$name.b64"
  else
    cp "$f" "$OUT/$name"
  fi
done
echo "wrote $(ls "$TMP"/*.ogg | wc -l) sounds to $OUT"
