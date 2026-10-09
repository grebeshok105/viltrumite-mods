package dev.baranhan.viltrumitecore.client.ironman;

import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
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
 * flight pose. Also hides Tony's hat layer once the helmet starts to close.
 */
public final class IronManPoser {
   private static final WeakHashMap<LivingEntity, Weights> WEIGHTS = new WeakHashMap<>();
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
      if (IronManView.helmet(IronManView.frame(snapshot, partialTick))) {
         model.hat.visible = false;
      }

      ThrusterFlames.Mode mode = entity instanceof AbstractClientPlayer player ? ThrusterFlames.mode(player, snapshot) : ThrusterFlames.Mode.NONE;
      Weights w = WEIGHTS.computeIfAbsent(entity, e -> new Weights());
      w.advance(mode == ThrusterFlames.Mode.HOVER, IronManView.glide(snapshot) && !entity.onGround(), IronManView.heavyLanding(snapshot), 12.0F);
      if (w.hover < 0.001F && w.glide < 0.001F && w.kneel < 0.001F) {
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
