package dev.baranhan.viltrumitecore.client.ironman;

import dev.baranhan.viltrumitecore.hero.HeroAction;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import dev.baranhan.viltrumitecore.client.ironman.mark.MarkState;
import dev.baranhan.viltrumitecore.hero.ironman.IronManFlags;
import dev.baranhan.viltrumiteflight.client.util.ShaderCompat;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * Iron Man poses at setupAnim TAIL (animation-system §3, §6), applied after
 * the flight pose mixins: hover (palms down, arms out, idle sway), glide
 * (arms spread) and the heavy-landing kneel (fist into the ground). Each is a
 * weight-blended layer towards an absolute target; CRUISE/SONIC keep the
 * flight pose. Stage 2 combat layers on top (synced timelines only): palm
 * forward to the crosshair for repulsors (recoil kick, both palms for a
 * volley), Unibeam chest out with arms back, missile stance, blade slash,
 * hammer overhead swing / wind-up, blade dash lunge and the shield guard.
 * Also hides Tony's hat layer once the helmet starts to close.
 */
public final class IronManPoser {
   private static final WeakHashMap<LivingEntity, Weights> WEIGHTS = new WeakHashMap<>();
   /** Arms-out weight while the suit parts fly and wrap (0..1). */
   private static final WeakHashMap<LivingEntity, float[]> EQUIP = new WeakHashMap<>();
   /** Kneel geometry (model pixels): body lean and hip height above the ground. */
   private static final float KNEEL_LEAN = 0.7F;
   private static final float KNEEL_HIP = 4.0F;

   private IronManPoser() {
   }

   public static void poseThirdPerson(PlayerModel<?> model, LivingEntity entity) {
      HeroPublicSnapshot snapshot = IronManView.of(entity);
      if (snapshot == null) {
         WEIGHTS.remove(entity);
         return;
      }

      Minecraft minecraft = Minecraft.getInstance();
      boolean localFirstPerson = entity == minecraft.player && minecraft.options.getCameraType().isFirstPerson();
      if (localFirstPerson && !ShaderCompat.isShadowPass()) {
         return;
      }

      float partialTick = minecraft.getFrameTime();
      if (IronManView.helmet(IronManPartsProvider.helmetFrame(entity, snapshot, partialTick))) {
         model.hat.visible = false;
      }

      // Stage 3: hand to the face while the helmet folds / unfolds (spec §10).
      float toggle = HelmetAnim.toggleProgress(entity);
      float gesture = toggle < 0.0F ? 0.0F : Mth.sin(toggle * Mth.PI);

      ThrusterFlames.Mode mode = entity instanceof AbstractClientPlayer player ? ThrusterFlames.mode(player, snapshot) : ThrusterFlames.Mode.NONE;
      Weights w = WEIGHTS.computeIfAbsent(entity, e -> new Weights());
      w.advance(mode == ThrusterFlames.Mode.HOVER, IronManView.glide(snapshot) && !entity.onGround(), IronManView.heavyLanding(snapshot), 12.0F);
      Combat combat = Combat.of(snapshot, partialTick);
      w.advanceCombat(combat, 18.0F);
      MarkState mark = MarkState.of(snapshot);
      float[] equip = EQUIP.computeIfAbsent(entity, e -> new float[1]);
      boolean arming = mark.markOn() && (mark.equipping() || mark.equipPhase() == IronManFlags.EQUIP_PARTIAL);
      equip[0] = Mth.lerp(0.25F, equip[0], arming ? 1.0F : 0.0F);
      if (w.hover < 0.001F && w.glide < 0.001F && w.kneel < 0.001F && w.combatIdle() && gesture < 0.001F && equip[0] < 0.001F) {
         return;
      }

      float time = entity.tickCount + partialTick;
      if (w.hover > 0.001F) {
         hover(model, w.hover, time);
      }

      if (w.glide > 0.001F) {
         glide(model, w.glide);
      }

      if (w.kneel > 0.001F) {
         kneel(model, w.kneel);
      }

      combat(model, w, combat, time);
      if (equip[0] > 0.001F) {
         // Arms slightly out while the parts fly and wrap, like Mark 42 (spec §12.4).
         rotate(model.rightArm, equip[0], 0.0F, 0.0F, 0.22F);
         rotate(model.leftArm, equip[0], 0.0F, 0.0F, -0.22F);
      }

      if (gesture > 0.001F) {
         rotate(model.leftArm, gesture, -2.2F + model.head.xRot, 0.35F + model.head.yRot * 0.5F, 0.15F);
      }

      model.hat.copyFrom(model.head);
      model.jacket.copyFrom(model.body);
      model.rightSleeve.copyFrom(model.rightArm);
      model.leftSleeve.copyFrom(model.leftArm);
      model.rightPants.copyFrom(model.rightLeg);
      model.leftPants.copyFrom(model.leftLeg);
   }

   /** Palms down, arms slightly out and moving, feet stabilizing (spec §7.5). */
   private static void hover(PlayerModel<?> model, float w, float time) {
      float sway = 0.06F * Mth.sin(time * 0.11F);
      float drift = 0.04F * Mth.sin(time * 0.07F + 1.0F);
      rotate(model.rightArm, w, -0.12F + sway, 0.0F, 0.32F + drift);
      rotate(model.leftArm, w, -0.12F - sway, 0.0F, -0.32F - drift);
      rotate(model.rightLeg, w, 0.08F + 0.5F * sway, 0.0F, 0.07F);
      rotate(model.leftLeg, w, -0.04F - 0.5F * sway, 0.0F, -0.07F);
   }

   /** Unpowered glide: arms spread like wings, legs together. */
   private static void glide(PlayerModel<?> model, float w) {
      rotate(model.rightArm, w, 0.0F, 0.0F, 1.25F);
      rotate(model.leftArm, w, 0.0F, 0.0F, -1.25F);
      rotate(model.rightLeg, w, 0.05F, 0.0F, 0.03F);
      rotate(model.leftLeg, w, 0.05F, 0.0F, -0.03F);
   }

   /**
    * Superhero landing: body leaned around the neck, the whole model lowered
    * so the hips sit KNEEL_HIP above the ground, right knee forward, left leg
    * back along the ground, right fist down, left arm back and out.
    */
   private static void kneel(PlayerModel<?> model, float w) {
      float cos = Mth.cos(KNEEL_LEAN);
      float sin = Mth.sin(KNEEL_LEAN);
      float hipY = 24.0F - KNEEL_HIP;
      float neckY = hipY - 12.0F * cos;
      float hipZ = 12.0F * sin;
      model.body.xRot = Mth.lerp(w, model.body.xRot, KNEEL_LEAN);
      model.body.y = Mth.lerp(w, model.body.y, neckY);
      model.head.y = Mth.lerp(w, model.head.y, neckY);
      model.head.xRot = Mth.lerp(w, model.head.xRot, -0.35F);
      float shoulderY = neckY + 2.0F * cos;
      float shoulderZ = 2.0F * sin;
      model.rightArm.y = Mth.lerp(w, model.rightArm.y, shoulderY);
      model.leftArm.y = Mth.lerp(w, model.leftArm.y, shoulderY);
      model.rightArm.z = Mth.lerp(w, model.rightArm.z, shoulderZ);
      model.leftArm.z = Mth.lerp(w, model.leftArm.z, shoulderZ);
      rotate(model.rightArm, w, -0.25F, 0.0F, 0.08F);
      rotate(model.leftArm, w, 0.65F, 0.0F, -0.55F);
      model.rightLeg.y = Mth.lerp(w, model.rightLeg.y, hipY);
      model.leftLeg.y = Mth.lerp(w, model.leftLeg.y, hipY);
      model.rightLeg.z = Mth.lerp(w, model.rightLeg.z, hipZ);
      model.leftLeg.z = Mth.lerp(w, model.leftLeg.z, hipZ);
      rotate(model.rightLeg, w, -1.3F, 0.0F, 0.1F);
      rotate(model.leftLeg, w, 1.25F, 0.0F, -0.12F);
   }

   /** Which combat pose runs this frame, and its 0..1 progress (pure on the snapshot). */
   record Combat(boolean aimRight, boolean aimLeft, float recoil, boolean unibeam, boolean missiles,
                 float slash, float swing, float windup, boolean lunge, boolean guard) {
      static Combat of(HeroPublicSnapshot snapshot, float partialTick) {
         int flags = snapshot.heroFlags();
         if (!IronManView.worn(snapshot)) {
            return new Combat(false, false, 0.0F, false, false, -1.0F, -1.0F, -1.0F, false, false);
         }

         dev.baranhan.viltrumitecore.hero.ironman.combat.RightTool tool = IronManView.tool(snapshot);
         boolean recoilOn = IronManFlags.is(flags, IronManFlags.Field.RECOIL);
         boolean rightShot = IronManFlags.is(flags, IronManFlags.Field.SHOT_HAND);
         boolean rmb = IronManView.channel(snapshot, HeroAction.SECONDARY_USE);
         float rmbProgress = IronManView.progress(snapshot, HeroAction.SECONDARY_USE, partialTick);
         boolean repulsorCharge = rmb && tool == dev.baranhan.viltrumitecore.hero.ironman.combat.RightTool.REPULSOR;
         boolean fullCharge = repulsorCharge && rmbProgress >= 0.999F;
         // Charging aims the hand that fires next (the other one than the last shot).
         boolean aimRight = repulsorCharge ? !rightShot || fullCharge : recoilOn && rightShot;
         boolean aimLeft = repulsorCharge ? rightShot || fullCharge : recoilOn && !rightShot;
         float slash = -1.0F;
         float swing = -1.0F;
         if (IronManView.channel(snapshot, HeroAction.PRIMARY_ATTACK)) {
            float t = IronManView.progress(snapshot, HeroAction.PRIMARY_ATTACK, partialTick);
            if (tool == dev.baranhan.viltrumitecore.hero.ironman.combat.RightTool.NANO_HAMMER) {
               swing = t;
            } else {
               slash = t;
            }
         }

         float windup = rmb && tool == dev.baranhan.viltrumitecore.hero.ironman.combat.RightTool.NANO_HAMMER ? rmbProgress : -1.0F;
         boolean lunge = rmb && tool == dev.baranhan.viltrumitecore.hero.ironman.combat.RightTool.NANO_BLADE;
         int phase = IronManView.unibeamPhase(snapshot);
         return new Combat(aimRight, aimLeft, recoilOn ? 1.0F : 0.0F, phase == 1 || phase == 2,
            IronManView.channel(snapshot, HeroAction.MISSILES), slash, swing, windup, lunge, IronManFlags.is(flags, IronManFlags.Field.SHIELD_UP));
      }
   }

   private static void combat(PlayerModel<?> model, Weights w, Combat c, float time) {
      float headX = model.head.xRot;
      float headY = model.head.yRot;
      if (w.unibeam > 0.001F) {
         // Chest pushed out, arms swept back and down, slight shake while firing.
         float shake = 0.02F * Mth.sin(time * 3.1F);
         model.body.xRot = Mth.lerp(w.unibeam, model.body.xRot, -0.12F + shake);
         rotate(model.rightArm, w.unibeam, 0.55F, 0.0F, 0.45F);
         rotate(model.leftArm, w.unibeam, 0.55F, 0.0F, -0.45F);
      }

      if (w.missiles > 0.001F) {
         // Shoulders squared, forearms slightly forward, palms out.
         rotate(model.rightArm, w.missiles, -0.35F, -0.15F, 0.35F);
         rotate(model.leftArm, w.missiles, -0.35F, 0.15F, -0.35F);
      }

      float kick = 0.35F * w.recoil;
      if (w.aimRight > 0.001F) {
         rotate(model.rightArm, w.aimRight, -Mth.HALF_PI + headX - kick, headY - 0.08F, 0.0F);
      }

      if (w.aimLeft > 0.001F) {
         rotate(model.leftArm, w.aimLeft, -Mth.HALF_PI + headX - kick, headY + 0.08F, 0.0F);
      }

      if (w.guard > 0.001F) {
         // Left forearm across the front, shield plate facing forward.
         rotate(model.leftArm, w.guard, -1.45F + headX * 0.5F, 0.75F + headY, -0.15F);
      }

      if (c.slash() >= 0.0F) {
         // Wide horizontal slash right → left.
         float t = easeOut(c.slash());
         rotate(model.rightArm, 1.0F, -1.45F, Mth.lerp(t, -0.9F, 0.9F) + headY, Mth.lerp(t, 0.25F, -0.25F));
         model.body.yRot = Mth.lerp(t, -0.25F, 0.3F);
      } else if (c.swing() >= 0.0F) {
         // Hammer overhead: raise high, smash down in front.
         float t = c.swing();
         float x = t < 0.35F ? Mth.lerp(t / 0.35F, -1.6F, -2.9F) : Mth.lerp(easeOut((t - 0.35F) / 0.65F), -2.9F, -0.35F);
         rotate(model.rightArm, 1.0F, x, headY * 0.5F, 0.1F);
         model.body.xRot = Mth.lerp(t, 0.0F, 0.25F);
      } else if (w.windup > 0.001F) {
         // Hammer charge: drawn back over the shoulder, trembling at full charge.
         float shake = c.windup() >= 0.999F ? 0.04F * Mth.sin(time * 4.0F) : 0.0F;
         rotate(model.rightArm, w.windup, -2.4F + shake, -0.5F, 0.35F);
         model.body.yRot = Mth.lerp(w.windup, model.body.yRot, -0.35F);
      } else if (w.lunge > 0.001F) {
         // Blade dash: body forward, blade arm thrust out.
         model.body.xRot = Mth.lerp(w.lunge, model.body.xRot, 0.35F);
         rotate(model.rightArm, w.lunge, -1.5F + headX, headY, 0.0F);
         rotate(model.leftArm, w.lunge, 0.6F, 0.0F, -0.2F);
      }
   }

   /** easeOutCubic for the fast part of a strike. */
   private static float easeOut(float t) {
      float u = 1.0F - Mth.clamp(t, 0.0F, 1.0F);
      return 1.0F - u * u * u;
   }

   private static void rotate(ModelPart part, float w, float x, float y, float z) {
      part.xRot = Mth.lerp(w, part.xRot, x);
      part.yRot = Mth.lerp(w, part.yRot, y);
      part.zRot = Mth.lerp(w, part.zRot, z);
   }

   /** Weight manager per entity (animation-system §6). Render thread only. */
   private static final class Weights {
      float hover;
      float glide;
      float kneel;
      long lastNanos = System.nanoTime();

      float aimRight;
      float aimLeft;
      float recoil;
      float unibeam;
      float missiles;
      float windup;
      float lunge;
      float guard;
      long lastCombatNanos = System.nanoTime();

      boolean combatIdle() {
         return this.aimRight < 0.001F && this.aimLeft < 0.001F && this.unibeam < 0.001F && this.missiles < 0.001F
            && this.windup < 0.001F && this.lunge < 0.001F && this.guard < 0.001F && this.strike;
      }

      /** True when no strike timeline runs (strikes are absolute, not weighted). */
      boolean strike = true;

      void advanceCombat(Combat c, float k) {
         long now = System.nanoTime();
         float dt = Math.min(0.1F, (now - this.lastCombatNanos) / 1.0E9F);
         this.lastCombatNanos = now;
         float a = 1.0F - (float)Math.exp(-k * dt);
         this.aimRight = Mth.lerp(a, this.aimRight, c.aimRight() ? 1.0F : 0.0F);
         this.aimLeft = Mth.lerp(a, this.aimLeft, c.aimLeft() ? 1.0F : 0.0F);
         this.recoil = Mth.lerp(a * 1.5F > 1.0F ? 1.0F : a * 1.5F, this.recoil, c.recoil());
         this.unibeam = Mth.lerp(a, this.unibeam, c.unibeam() ? 1.0F : 0.0F);
         this.missiles = Mth.lerp(a, this.missiles, c.missiles() ? 1.0F : 0.0F);
         this.windup = Mth.lerp(a, this.windup, c.windup() >= 0.0F ? 1.0F : 0.0F);
         this.lunge = Mth.lerp(a, this.lunge, c.lunge() ? 1.0F : 0.0F);
         this.guard = Mth.lerp(a, this.guard, c.guard() ? 1.0F : 0.0F);
         this.strike = c.slash() < 0.0F && c.swing() < 0.0F;
      }

      void advance(boolean hoverOn, boolean glideOn, boolean kneelOn, float k) {
         long now = System.nanoTime();
         float dt = Math.min(0.1F, (now - this.lastNanos) / 1.0E9F);
         this.lastNanos = now;
         float a = 1.0F - (float)Math.exp(-k * dt);
         this.hover = Mth.lerp(a, this.hover, hoverOn ? 1.0F : 0.0F);
         this.glide = Mth.lerp(a, this.glide, glideOn ? 1.0F : 0.0F);
         this.kneel = Mth.lerp(a, this.kneel, kneelOn ? 1.0F : 0.0F);
      }
   }
}
