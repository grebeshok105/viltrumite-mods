#!/usr/bin/env bash
# Iron Man Stage 2 combat sounds: own synthesis with ffmpeg only (spec §17:
# no vanilla or third-party sounds). Regenerate:
#   tools/sfx/ironman_stage2.sh [OUT_DIR]
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

# Repulsor shot (0.4 s): bright energy zap with a falling whine and a puff.
render repulsor_shot 0.4 "
aevalsrc='0.55*sin(2*PI*(1900*t-2600*t*t))*exp(-9*t)+0.3*sin(2*PI*(950*t-1200*t*t))*exp(-11*t)':s=$SR:d=0.4[zap];
anoisesrc=c=white:a=0.7:s=$SR:d=0.4,bandpass=f=3000:w=2500,volume='exp(-22*t)':eval=frame[puff];
aevalsrc='0.5*sin(2*PI*120*t)*exp(-18*t)':s=$SR:d=0.4[thump];
[zap][puff][thump]amix=inputs=3:normalize=0,alimiter=limit=0.9[out]"

# Repulsor charge (1.0 s): rising whine that saturates.
render repulsor_charge 1.0 "
aevalsrc='0.3*sin(2*PI*(400*t+900*t*t))*min(1,t*3)+0.12*sin(2*PI*(800*t+1800*t*t))*min(1,t*2)':s=$SR:d=1.0[rise];
anoisesrc=c=pink:a=0.25:s=$SR:d=1.0,highpass=f=2000,volume='0.4*t':eval=frame[hiss];
[rise][hiss]amix=inputs=2:normalize=0,afade=t=out:st=0.9:d=0.1,alimiter=limit=0.85[out]"

# Repulsor volley (0.8 s): double heavy blast with a deep boom.
render repulsor_volley 0.8 "
aevalsrc='0.6*sin(2*PI*(1500*t-1500*t*t))*exp(-6*t)+0.4*sin(2*PI*(1100*(t-0.04))*gt(t,0.04))*exp(-7*t)':s=$SR:d=0.8[zap];
aevalsrc='0.9*sin(2*PI*(70*t-20*t*t))*exp(-5*t)':s=$SR:d=0.8[boom];
anoisesrc=c=brown:a=0.8:s=$SR:d=0.8,lowpass=f=1500,volume='exp(-7*t)':eval=frame[blast];
[zap][boom][blast]amix=inputs=3:normalize=0,afade=t=out:st=0.65:d=0.15,alimiter=limit=0.95[out]"

# Repulsor fizzle (0.35 s): weak sputter, electrical crackle.
render repulsor_fizzle 0.35 "
anoisesrc=c=white:a=0.5:s=$SR:d=0.35,bandpass=f=2500:w=1500,volume='0.6*(0.5+0.5*sin(2*PI*45*t))*exp(-8*t)':eval=frame[crackle];
aevalsrc='0.2*sin(2*PI*(600*t-700*t*t))*exp(-10*t)':s=$SR:d=0.35[drop];
[crackle][drop]amix=inputs=2:normalize=0,alimiter=limit=0.8[out]"

# Unibeam charge (1.0 s): deep reactor hum swelling up.
render unibeam_charge 1.0 "
aevalsrc='(0.35*sin(2*PI*(90*t+70*t*t))+0.2*sin(2*PI*(180*t+140*t*t))+0.1*sin(2*PI*(540*t+420*t*t)))*min(1,t*1.5)':s=$SR:d=1.0[hum];
anoisesrc=c=pink:a=0.3:s=$SR:d=1.0,bandpass=f=1500:w=1000,volume='0.5*t*t':eval=frame[air];
[hum][air]amix=inputs=2:normalize=0,afade=t=out:st=0.92:d=0.08,alimiter=limit=0.85[out]"

# Unibeam loop (2.0 s, seamless): thick roaring beam.
render unibeam_loop 2.0 "
anoisesrc=c=brown:a=0.8:s=$SR:d=2.0,lowpass=f=900,volume=1.3[roar];
aevalsrc='0.18*sin(2*PI*110*t)+0.12*sin(2*PI*220*t)+0.06*sin(2*PI*1320*t+0.5*sin(2*PI*6*t))':s=$SR:d=2.0[tone];
anoisesrc=c=white:a=0.2:s=$SR:d=2.0,highpass=f=4000,volume=0.4[sizzle];
[roar][tone][sizzle]amix=inputs=3:normalize=0,alimiter=limit=0.8[out]"

# Unibeam overheat (1.2 s): steam hiss and cooling ticks.
render unibeam_overheat 1.2 "
anoisesrc=c=white:a=0.6:s=$SR:d=1.2,highpass=f=2500,volume='0.7*exp(-2.2*t)':eval=frame[steam];
aevalsrc='0.25*sin(2*PI*(500*t-150*t*t))*exp(-3*t)':s=$SR:d=1.2[drop];
anoisesrc=c=white:a=0.6:s=$SR:d=1.2,highpass=f=5000,volume='0.5*gt(sin(2*PI*7*t),0.97)':eval=frame[ticks];
[steam][drop][ticks]amix=inputs=3:normalize=0,afade=t=out:st=1.0:d=0.2,alimiter=limit=0.85[out]"

# Overdraft sputter (1.0 s): beam breaking up, stutter and sparks.
render overdraft_sputter 1.0 "
anoisesrc=c=brown:a=0.9:s=$SR:d=1.0,lowpass=f=1200,volume='1.2*gt(sin(2*PI*9*t),0)':eval=frame[stutter];
anoisesrc=c=white:a=0.6:s=$SR:d=1.0,highpass=f=3500,volume='0.6*gt(sin(2*PI*23*t+3),0.6)':eval=frame[sparks];
aevalsrc='0.25*sin(2*PI*(300*t+400*t*t))':s=$SR:d=1.0[whine];
[stutter][sparks][whine]amix=inputs=3:normalize=0,alimiter=limit=0.9[out]"

# Core explosion (2.5 s): huge boom, debris and a falling ring.
render core_explosion 2.5 "
aevalsrc='1.0*sin(2*PI*(42*t-6*t*t))*exp(-1.8*t)+0.35*sin(2*PI*(880*t-300*t*t))*exp(-3*t)':s=$SR:d=2.5[boom];
anoisesrc=c=brown:a=1.0:s=$SR:d=2.5,lowpass=f=1800,volume='1.4*exp(-2.2*t)':eval=frame[blast];
anoisesrc=c=white:a=0.4:s=$SR:d=2.5,highpass=f=3000,volume='if(gt(t,0.1),0.5*exp(-3*(t-0.1)),0)':eval=frame[debris];
[boom][blast][debris]amix=inputs=3:normalize=0,afade=t=out:st=2.1:d=0.4,alimiter=limit=0.97[out]"

# Nanite form (0.5 s): quick rising metallic shimmer, click.
render nanite_form 0.5 "
aevalsrc='0.3*sin(2*PI*(700*t+2400*t*t))*(1-exp(-30*t))*exp(-2*t)':s=$SR:d=0.5[sweep];
anoisesrc=c=white:a=0.5:s=$SR:d=0.5,highpass=f=3000,volume='0.5*(0.5+0.5*sin(2*PI*60*t))*min(1,t*4)':eval=frame[scales];
aevalsrc='if(gt(t,0.42),0.6*sin(2*PI*1600*(t-0.42))*exp(-50*(t-0.42)),0)':s=$SR:d=0.5[click];
[sweep][scales][click]amix=inputs=3:normalize=0,alimiter=limit=0.9[out]"

# Nanite dissolve (0.5 s): falling shimmer, scattering grains.
render nanite_dissolve 0.5 "
aevalsrc='0.3*sin(2*PI*(2200*t-1800*t*t))*exp(-4*t)':s=$SR:d=0.5[sweep];
anoisesrc=c=white:a=0.5:s=$SR:d=0.5,highpass=f=3500,volume='0.5*(0.5+0.5*sin(2*PI*50*t))*max(0,1-t*2)':eval=frame[grains];
[sweep][grains]amix=inputs=2:normalize=0,alimiter=limit=0.9[out]"

# Nanite repair (0.8 s): soft crawling ticks rising to a chime.
render nanite_repair 0.8 "
anoisesrc=c=white:a=0.4:s=$SR:d=0.8,highpass=f=4000,volume='0.35*(0.5+0.5*sin(2*PI*35*t))':eval=frame[crawl];
aevalsrc='if(gt(t,0.6),0.3*sin(2*PI*1760*(t-0.6))*exp(-12*(t-0.6))+0.2*sin(2*PI*2640*(t-0.6))*exp(-14*(t-0.6)),0)':s=$SR:d=0.8[chime];
[crawl][chime]amix=inputs=2:normalize=0,alimiter=limit=0.85[out]"

# Blade slash (0.3 s): sharp swish with a metal edge.
render blade_slash 0.3 "
anoisesrc=c=white:a=0.8:s=$SR:d=0.3,bandpass=f=3500:w=3000,volume='sin(PI*min(1,t/0.25))*1.0':eval=frame[swish];
aevalsrc='0.2*sin(2*PI*(3000*t-2000*t*t))*exp(-10*t)':s=$SR:d=0.3[edge];
[swish][edge]amix=inputs=2:normalize=0,alimiter=limit=0.9[out]"

# Hammer hit (0.5 s): heavy metal impact.
render hammer_hit 0.5 "
aevalsrc='0.9*sin(2*PI*65*t)*exp(-10*t)+0.35*sin(2*PI*620*t)*exp(-9*t)+0.2*sin(2*PI*1240*t)*exp(-12*t)':s=$SR:d=0.5[impact];
anoisesrc=c=brown:a=0.9:s=$SR:d=0.5,lowpass=f=2000,volume='exp(-16*t)':eval=frame[crunch];
[impact][crunch]amix=inputs=2:normalize=0,alimiter=limit=0.95[out]"

# Hammer slam (1.0 s): ground crack and rumble.
render hammer_slam 1.0 "
aevalsrc='1.0*sin(2*PI*(50*t-10*t*t))*exp(-4*t)':s=$SR:d=1.0[boom];
anoisesrc=c=brown:a=1.0:s=$SR:d=1.0,lowpass=f=1000,volume='1.3*exp(-4.5*t)':eval=frame[rumble];
anoisesrc=c=white:a=0.4:s=$SR:d=1.0,highpass=f=2500,volume='0.5*exp(-10*t)':eval=frame[crack];
[boom][rumble][crack]amix=inputs=3:normalize=0,afade=t=out:st=0.85:d=0.15,alimiter=limit=0.95[out]"

# Shield open (0.35 s): plates snapping out.
render shield_open 0.35 "
anoisesrc=c=white:a=0.6:s=$SR:d=0.35,highpass=f=2500,volume='0.6*(0.5+0.5*sin(2*PI*80*t))*max(0,1-t*3.5)':eval=frame[plates];
aevalsrc='if(gt(t,0.22),0.5*sin(2*PI*1300*(t-0.22))*exp(-35*(t-0.22)),0)':s=$SR:d=0.35[lock];
[plates][lock]amix=inputs=2:normalize=0,alimiter=limit=0.9[out]"

# Shield hit (0.4 s): dull clang on plates.
render shield_hit 0.4 "
aevalsrc='0.6*sin(2*PI*180*t)*exp(-14*t)+0.3*sin(2*PI*820*t)*exp(-10*t)+0.15*sin(2*PI*1310*t)*exp(-12*t)':s=$SR:d=0.4[clang];
anoisesrc=c=pink:a=0.6:s=$SR:d=0.4,lowpass=f=3000,volume='exp(-25*t)':eval=frame[hit];
[clang][hit]amix=inputs=2:normalize=0,alimiter=limit=0.9[out]"

# Shield perfect (0.7 s): bright clang plus a rising chime.
render shield_perfect 0.7 "
aevalsrc='0.5*sin(2*PI*1050*t)*exp(-6*t)+0.35*sin(2*PI*1575*t)*exp(-5*t)+0.25*sin(2*PI*2100*t)*exp(-4*t)':s=$SR:d=0.7[chime];
anoisesrc=c=white:a=0.6:s=$SR:d=0.7,highpass=f=1500,volume='exp(-30*t)':eval=frame[hit];
[chime][hit]amix=inputs=2:normalize=0,afade=t=out:st=0.55:d=0.15,alimiter=limit=0.9[out]"

# Missile flaps (0.4 s): mechanical flaps opening.
render missile_flaps 0.4 "
aevalsrc='0.4*sin(2*PI*900*t)*exp(-40*t)+if(gt(t,0.12),0.4*sin(2*PI*1100*(t-0.12))*exp(-40*(t-0.12)),0)':s=$SR:d=0.4[clacks];
anoisesrc=c=pink:a=0.4:s=$SR:d=0.4,bandpass=f=1800:w=1200,volume='0.4*max(0,1-t*3)':eval=frame[servo];
[clacks][servo]amix=inputs=2:normalize=0,alimiter=limit=0.9[out]"

# Missile launch (0.9 s): rapid hissing rocket bursts.
render missile_launch 0.9 "
anoisesrc=c=white:a=0.8:s=$SR:d=0.9,bandpass=f=2500:w=3000,volume='0.8*(0.6+0.4*gt(sin(2*PI*9*t),0))*exp(-2*t)':eval=frame[hiss];
anoisesrc=c=brown:a=0.6:s=$SR:d=0.9,lowpass=f=600,volume='0.8*exp(-4*t)':eval=frame[thrust];
[hiss][thrust]amix=inputs=2:normalize=0,afade=t=out:st=0.7:d=0.2,alimiter=limit=0.9[out]"

# Missile explode (0.8 s): small sharp blast.
render missile_explode 0.8 "
aevalsrc='0.8*sin(2*PI*(80*t-30*t*t))*exp(-6*t)':s=$SR:d=0.8[boom];
anoisesrc=c=brown:a=1.0:s=$SR:d=0.8,lowpass=f=2500,volume='1.2*exp(-8*t)':eval=frame[blast];
[boom][blast]amix=inputs=2:normalize=0,afade=t=out:st=0.6:d=0.2,alimiter=limit=0.95[out]"

# JARVIS warning (0.8 s): two-tone HUD alert (voice comes in Stage 3).
render jarvis_warning 0.8 "
aevalsrc='0.35*sin(2*PI*880*t)*between(t,0,0.18)+0.35*sin(2*PI*660*t)*between(t,0.25,0.43)+0.35*sin(2*PI*880*t)*between(t,0.5,0.68)':s=$SR:d=0.8[beeps];
[beeps]afade=t=out:st=0.7:d=0.1,alimiter=limit=0.8[out]"

for f in "$TMP"/*.ogg; do
  name="$(basename "$f")"
  if [[ "$OUT" == *binassets* ]]; then
    base64 -w 76 "$f" > "$OUT/$name.b64"
  else
    cp "$f" "$OUT/$name"
  fi
done
echo "wrote $(ls "$TMP" | wc -l) sounds to $OUT"
