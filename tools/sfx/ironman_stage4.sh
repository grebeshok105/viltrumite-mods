#!/usr/bin/env bash
# Iron Man Stage 4 sounds (Veronica, suit parts, mark exit and entry, mark
# break, marks' signature weapons): own synthesis with ffmpeg only.
# Regenerate:
#   tools/sfx/ironman_stage4.sh [OUT_DIR]
# Default OUT_DIR writes base64 files for decodeBinaryAssets. Mono 44.1 kHz
# Ogg Vorbis. Each sound is scaled to TARGET_MEAN dB (the level of the stage 1-3
# sounds) and its decoded peak is kept at or below -1.5 dB (Vorbis overshoot).
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
render veronica_fall 2.5 "
aevalsrc='0.24*sin(2*PI*(400*t+700*2.5/PI*(1-cos(PI*t/2.5))))*min(1,t*4)*(0.5+0.5*t/2.5)':s=$SR:d=2.5[whistle];
anoisesrc=c=pink:a=0.8:s=$SR:d=2.5:seed=1,lowpass=f=700,volume='0.9*min(1,t*2)*(0.35+0.65*t/2.5)':eval=frame[rumble];
[whistle][rumble]amix=inputs=2:normalize=0,afade=t=out:st=2.1:d=0.4,alimiter=limit=0.9:level=disabled[out]"

# Veronica impact: deep boom with a falling sweep, debris crunch and metal clang (1.8 s).
render veronica_impact 1.8 "
aevalsrc='0.9*sin(2*PI*(38*t+20*(1-exp(-3*t))))*exp(-2.2*t)':s=$SR:d=1.8[boom];
anoisesrc=c=pink:a=0.7:s=$SR:d=1.8:seed=2,lowpass=f=900,volume='exp(-4*t)':eval=frame[crunch];
aevalsrc='0.35*sin(2*PI*880*t)*exp(-9*t)+0.25*sin(2*PI*1320*t)*exp(-12*t)+0.18*sin(2*PI*2093*t)*exp(-16*t)':s=$SR:d=1.8[clang];
[boom][crunch][clang]amix=inputs=3:normalize=0,alimiter=limit=0.9:level=disabled[out]"

# Veronica opens: hydraulic hiss, servo whine and a latch click (1.2 s).
render veronica_open 1.2 "
anoisesrc=c=white:a=0.5:s=$SR:d=1.2:seed=3,highpass=f=2500,volume='0.6*min(1,t*6)*max(0,1-(t-0.55)*1.6)':eval=frame[hiss];
aevalsrc='0.18*sin(2*PI*(900*t+250*t*t))*min(1,t*5)*max(0,1-(t-0.5)*2)':s=$SR:d=1.2[servo];
aevalsrc='if(gt(t,0.9),0.7*sin(2*PI*1600*(t-0.9))*exp(-60*(t-0.9))+0.3*sin(2*PI*700*(t-0.9))*exp(-40*(t-0.9)),0)':s=$SR:d=1.2[latch];
[hiss][servo][latch]amix=inputs=3:normalize=0,alimiter=limit=0.9:level=disabled[out]"

# Veronica leaves: rocket ignition, roaring thrust that fades as it climbs (2.5 s).
render veronica_leave 2.5 "
anoisesrc=c=pink:a=0.9:s=$SR:d=2.5:seed=4,lowpass=f=1500,volume='min(1,t*5)*(0.6+0.4*sin(2*PI*3*t))*exp(-0.55*t)':eval=frame[roar];
anoisesrc=c=white:a=0.4:s=$SR:d=2.5:seed=5,bandpass=f=1200:w=900,volume='0.5*min(1,t*3)*exp(-0.9*t)':eval=frame[whoosh];
aevalsrc='0.4*sin(2*PI*(90*t-10*t*t))*min(1,t*6)*exp(-1.1*t)':s=$SR:d=2.5[rumble];
[roar][whoosh][rumble]amix=inputs=3:normalize=0,afade=t=out:st=1.9:d=0.6,alimiter=limit=0.9:level=disabled[out]"

# Suit part flies in: short jet whoosh (0.5 s).
render part_fly 0.5 "
anoisesrc=c=white:a=0.9:s=$SR:d=0.5:seed=6,bandpass=f=1400:w=1200,volume='sin(PI*t/0.5)':eval=frame,afade=t=out:st=0.4:d=0.1,alimiter=limit=0.9:level=disabled[out]"

# Part clamps on a limb: metal plate click (0.25 s).
render part_clamp 0.25 "
anoisesrc=c=white:a=0.9:s=$SR:d=0.25:seed=7,highpass=f=2500,volume='exp(-70*t)':eval=frame[click];
aevalsrc='0.35*sin(2*PI*1850*t)*exp(-45*t)+0.25*sin(2*PI*3120*t)*exp(-60*t)+0.15*sin(2*PI*620*t)*exp(-30*t)':s=$SR:d=0.25[metal];
[click][metal]amix=inputs=2:normalize=0,alimiter=limit=0.9:level=disabled[out]"

# Helmet locks: servo whir, solid click, soft confirmation beep (0.7 s).
render helmet_lock 0.7 "
aevalsrc='0.2*sin(2*PI*(600*t+400*t*t))*between(t,0,0.45)*min(1,t*15)':s=$SR:d=0.7[servo];
aevalsrc='if(between(t,0.5,0.7),0.7*exp(-90*(t-0.5))*sin(2*PI*1500*(t-0.5)),0)':s=$SR:d=0.7[click];
aevalsrc='if(between(t,0.52,0.7),0.22*sin(2*PI*1000*(t-0.52))*exp(-7*(t-0.52)),0)':s=$SR:d=0.7[beep];
[servo][click][beep]amix=inputs=3:normalize=0,alimiter=limit=0.9:level=disabled[out]"

# Mark exit: hydraulic plates swing open with three clanks (1.2 s).
render mark_exit 1.2 "
anoisesrc=c=white:a=0.5:s=$SR:d=1.2:seed=8,bandpass=f=1800:w=1600,volume='0.5*(0.5+0.5*sin(2*PI*6*t))*min(1,t*4)':eval=frame[hyd];
aevalsrc='0.2*sin(2*PI*(220*t+90*t*t))*min(1,t*4)*max(0,1-(t-1.0)*5)':s=$SR:d=1.2[servo];
aevalsrc='0.5*sin(2*PI*1400*(t-0.3))*exp(-40*(t-0.3))*gt(t,0.3)*lt(t,0.5)+0.5*sin(2*PI*1400*(t-0.6))*exp(-40*(t-0.6))*gt(t,0.6)*lt(t,0.8)+0.5*sin(2*PI*1400*(t-0.9))*exp(-40*(t-0.9))*gt(t,0.9)*lt(t,1.1)':s=$SR:d=1.2[clank];
[hyd][servo][clank]amix=inputs=3:normalize=0,alimiter=limit=0.9:level=disabled[out]"

# Mark enter: plates close with four clanks and a heavy lock thump (1.2 s).
render mark_enter 1.2 "
anoisesrc=c=white:a=0.5:s=$SR:d=1.2:seed=9,bandpass=f=1600:w=1400,volume='0.45*(0.5+0.5*sin(2*PI*6*t))*max(0,1-t)':eval=frame[hyd];
aevalsrc='0.2*sin(2*PI*(420*t-120*t*t))*min(1,t*5)*max(0,1-(t-0.9)*5)':s=$SR:d=1.2[servo];
aevalsrc='0.45*sin(2*PI*1000*(t-0.25))*exp(-35*(t-0.25))*gt(t,0.25)*lt(t,0.45)+0.45*sin(2*PI*1000*(t-0.5))*exp(-35*(t-0.5))*gt(t,0.5)*lt(t,0.7)+0.45*sin(2*PI*1000*(t-0.75))*exp(-35*(t-0.75))*gt(t,0.75)*lt(t,0.95)+0.7*sin(2*PI*260*(t-1.02))*exp(-14*(t-1.02))*gt(t,1.02)*lt(t,1.2)':s=$SR:d=1.2[clank];
[hyd][servo][clank]amix=inputs=3:normalize=0,alimiter=limit=0.9:level=disabled[out]"

# Mark breaks apart: crash, sparks crackle and loose metal pieces (1.6 s).
render mark_break 1.6 "
anoisesrc=c=white:a=0.9:s=$SR:d=1.6:seed=10,highpass=f=600,volume='exp(-2.8*t)*min(1,t*60)':eval=frame[crash];
aevalsrc='0.22*sin(2*PI*5200*t)*gt(sin(2*PI*17*t)*sin(2*PI*29*t),0.85)*exp(-1.5*t)':s=$SR:d=1.6[sparks];
aevalsrc='0.9*sin(2*PI*(80*t-20*t*t))*exp(-6*t)':s=$SR:d=1.6[thump];
aevalsrc='0.4*sin(2*PI*800*(t-0.35))*exp(-18*(t-0.35))*gt(t,0.35)+0.4*sin(2*PI*800*(t-0.7))*exp(-18*(t-0.7))*gt(t,0.7)+0.4*sin(2*PI*800*(t-0.95))*exp(-18*(t-0.95))*gt(t,0.95)+0.4*sin(2*PI*800*(t-1.25))*exp(-18*(t-1.25))*gt(t,1.25)':s=$SR:d=1.6[pieces];
[crash][sparks][thump][pieces]amix=inputs=4:normalize=0,alimiter=limit=0.9:level=disabled[out]"

# Micro-laser: thin high whine with hiss, loops every 1 s. The hiss is a sum of
# sines at whole hertz, so the 1 s loop is exactly periodic; 6 ms fades hide the
# Vorbis edge frames at the loop point.
hiss_terms="$(awk 'BEGIN { for (f = 6500; f <= 8900; f += 60) { ph = (f * 37) % 628 / 100; printf "%s0.012*sin(2*PI*%d*t+%.2f)", (n++ ? "+" : ""), f, ph } }')"
render micro_laser 1.0 "
aevalsrc='0.14*sin(2*PI*2600*t+4*sin(2*PI*8*t))':s=$SR:d=1.0[tone];
aevalsrc='(${hiss_terms})*(0.7+0.3*sin(2*PI*8*t))':s=$SR:d=1.0[hiss];
[tone][hiss]amix=inputs=2:normalize=0,afade=t=in:d=0.006,afade=t=out:st=0.994:d=0.006[out]"

# Rocket fist launches: pop, thump and a short whoosh (0.6 s).
render rocket_fist_launch 0.6 "
anoisesrc=c=white:a=0.9:s=$SR:d=0.6:seed=12,highpass=f=1800,volume='exp(-28*t)':eval=frame[pop];
aevalsrc='0.6*sin(2*PI*(110*t-40*t*t))*exp(-9*t)':s=$SR:d=0.6[thump];
anoisesrc=c=pink:a=0.5:s=$SR:d=0.6:seed=13,bandpass=f=900:w=800,volume='0.8*min(1,t*4)*exp(-3*t)':eval=frame[whoosh];
[pop][thump][whoosh]amix=inputs=3:normalize=0,alimiter=limit=0.9:level=disabled[out]"

# Rocket fist hits: heavy metal punch with a crack and a ring (0.6 s).
render rocket_fist_hit 0.6 "
aevalsrc='0.9*sin(2*PI*(90*t-35*t*t))*exp(-10*t)':s=$SR:d=0.6[thump];
anoisesrc=c=white:a=0.8:s=$SR:d=0.6:seed=14,highpass=f=2000,volume='exp(-35*t)':eval=frame[crack];
aevalsrc='0.3*sin(2*PI*520*t)*exp(-12*t)+0.25*sin(2*PI*1150*t)*exp(-16*t)':s=$SR:d=0.6[metal];
[thump][crack][metal]amix=inputs=3:normalize=0,alimiter=limit=0.9:level=disabled[out]"

# Camouflage on: shimmering sweep up (1.0 s).
render camo_on 1.0 "
aevalsrc='0.22*sin(2*PI*(400*t+800*t*t))*(0.6+0.4*sin(2*PI*5*t))*min(1,t*5)*max(0,1-(t-0.9)*10)':s=$SR:d=1.0[sweep];
anoisesrc=c=white:a=0.5:s=$SR:d=1.0:seed=15,bandpass=f=6000:w=3000,volume='0.12*(0.5+0.5*sin(2*PI*(3*t+t*t*2)))*min(1,t*3)':eval=frame[shimmer];
[sweep][shimmer]amix=inputs=2:normalize=0,alimiter=limit=0.9:level=disabled[out]"

# Camouflage off: shimmering sweep down (1.0 s).
render camo_off 1.0 "
aevalsrc='0.22*sin(2*PI*(2000*t-800*t*t))*(0.6+0.4*sin(2*PI*5*t))*min(1,t*12)*max(0,1-t)':s=$SR:d=1.0[sweep];
anoisesrc=c=white:a=0.5:s=$SR:d=1.0:seed=16,bandpass=f=6000:w=3000,volume='0.12*(0.5+0.5*sin(2*PI*(3*t+t*t*2)))*max(0,1-t)':eval=frame[shimmer];
[sweep][shimmer]amix=inputs=2:normalize=0,alimiter=limit=0.9:level=disabled[out]"

# Starboost: big thruster burst with a sonic whine (1.5 s).
render starboost 1.5 "
aevalsrc='0.8*sin(2*PI*(70*t+25*(1-exp(-6*t))))*exp(-1.6*t)':s=$SR:d=1.5[boom];
anoisesrc=c=pink:a=0.9:s=$SR:d=1.5:seed=17,lowpass=f=2200,volume='min(1,t*12)*(0.7+0.3*sin(2*PI*9*t))*max(0,1-t/1.5)':eval=frame[roar];
aevalsrc='0.12*sin(2*PI*(900*t+500*t*t))*min(1,t*4)*max(0,1-t/1.5)':s=$SR:d=1.5[whine];
[boom][roar][whine]amix=inputs=3:normalize=0,alimiter=limit=0.9:level=disabled[out]"

# Pulse Unibeam: short charged energy zap, falling sweep (0.5 s).
render pulse_unibeam 0.5 "
aevalsrc='0.45*sin(2*PI*(300*t+150*(1-exp(-10*t))))*exp(-6*t)':s=$SR:d=0.5[zap];
anoisesrc=c=white:a=0.5:s=$SR:d=0.5:seed=18,highpass=f=4000,volume='0.4*exp(-12*t)':eval=frame[crackle];
aevalsrc='0.15*sin(2*PI*900*t)*exp(-4*t)':s=$SR:d=0.5[buzz];
[zap][crackle][buzz]amix=inputs=3:normalize=0,alimiter=limit=0.9:level=disabled[out]"

# Shoulder gun: three rounds of a burst, each with a thump and a rattle (0.5 s).
render shoulder_gun 0.5 "
anoisesrc=c=white:a=0.9:s=$SR:d=0.5:seed=19,highpass=f=700,volume='0.9*lt(t,0.18)*exp(-55*mod(t,0.06))':eval=frame[shots];
aevalsrc='0.6*sin(2*PI*(160*mod(t,0.06)-50*mod(t,0.06)*mod(t,0.06)))*exp(-30*mod(t,0.06))*lt(t,0.18)':s=$SR:d=0.5[thump];
anoisesrc=c=white:a=0.5:s=$SR:d=0.5:seed=20,bandpass=f=2600:w=2000,volume='0.3*lt(t,0.18)*exp(-25*mod(t,0.06))':eval=frame[rattle];
[shots][thump][rattle]amix=inputs=3:normalize=0,alimiter=limit=0.9:level=disabled[out]"

# Ground slam: huge impact with a falling boom, a rumbling crash and debris (2.0 s).
render slam_impact 2.0 "
aevalsrc='0.95*sin(2*PI*(30*t+40*(1-exp(-5*t))))*exp(-1.8*t)':s=$SR:d=2.0[boom];
anoisesrc=c=pink:a=0.9:s=$SR:d=2.0:seed=21,lowpass=f=1000,volume='exp(-2.2*t)':eval=frame[crash];
anoisesrc=c=white:a=0.6:s=$SR:d=2.0:seed=22,highpass=f=3000,volume='0.35*exp(-6*t)*gt(sin(2*PI*23*t)*sin(2*PI*31*t),0.6)':eval=frame[debris];
[boom][crash][debris]amix=inputs=3:normalize=0,alimiter=limit=0.9:level=disabled[out]"

for f in "$TMP"/*.ogg; do
  name="$(basename "$f")"
  if [[ "$OUT" == *binassets* ]]; then
    base64 -w 76 "$f" > "$OUT/$name.b64"
  else
    cp "$f" "$OUT/$name"
  fi
done
echo "wrote $(ls "$TMP"/*.ogg | wc -l) sounds to $OUT"
