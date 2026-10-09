package dev.baranhan.viltrumitecore.client.ironman.mark.sig;

import dev.baranhan.viltrumiteflight.client.util.ShaderCompat;
import dev.baranhan.viltrumitecore.client.ironman.IronManView;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import dev.baranhan.viltrumitecore.hero.ironman.IronManFlags;
import dev.baranhan.viltrumitecore.hero.ironman.IronManVariant;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkId;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * Third-person arm poses of the signatures (animation-system §3): micro-laser
 * arm raised, rocket fist launch, shoulder gun brace, slam jump (arms up) and
 * dive (arms forward). Weight-blended towards the target, applied after the
 * Iron Man hover pose. First-person arm poses are not done yet.
 */
public final class SignaturePoser {
   private static final WeakHashMap<LivingEntity, Blend> BLENDS = new WeakHashMap<>();
   private static final float BLEND_RATE = 0.25F;

   private SignaturePoser() {
   }

   enum Kind {
      NONE,
      LASER,
      FIST,
      GUN,
      SLAM_JUMP,
      SLAM_DIVE
   }

   private static final class Blend {
      Kind kind = Kind.NONE;
      float weight;
   }

   public static void poseThirdPerson(PlayerModel<?> model, LivingEntity entity) {
      Minecraft minecraft = Minecraft.getInstance();
      boolean localFirstPerson = entity == minecraft.player && minecraft.options.getCameraType().isFirstPerson();
      if (localFirstPerson && !ShaderCompat.isShadowPass()) {
         return;
      }

      HeroPublicSnapshot snapshot = IronManView.of(entity);
      Kind target = snapshot == null ? Kind.NONE : kind(snapshot);
      Blend blend = BLENDS.computeIfAbsent(entity, e -> new Blend());
      if (target != Kind.NONE) {
         blend.kind = target;
      }

      blend.weight = Mth.lerp(BLEND_RATE, blend.weight, target == Kind.NONE ? 0.0F : 1.0F);
      if (blend.weight < 0.01F) {
         if (target == Kind.NONE) {
            BLENDS.remove(entity);
         }

         return;
      }

      apply(model, blend.kind, blend.weight);
      model.hat.copyFrom(model.head);
      model.jacket.copyFrom(model.body);
      model.rightSleeve.copyFrom(model.rightArm);
      model.leftSleeve.copyFrom(model.leftArm);
      model.rightPants.copyFrom(model.rightLeg);
      model.leftPants.copyFrom(model.leftLeg);
   }

   static Kind kind(HeroPublicSnapshot snapshot) {
      int flags = snapshot.heroFlags();
      if (!IronManView.worn(snapshot) || !IronManFlags.is(flags, IronManFlags.Field.SIGNATURE_ACTIVE)) {
         return Kind.NONE;
      }

      MarkId mark = IronManVariant.mark(snapshot.variant());
      if (mark == null) {
         return Kind.NONE;
      }

      return switch (mark) {
         case MARK_7 -> Kind.LASER;
         case MARK_42 -> Kind.FIST;
         case WAR_MACHINE_MK2 -> Kind.GUN;
         case IRON_HEART_MK3 -> IronManFlags.is(flags, IronManFlags.Field.SIGNATURE_AUX) ? Kind.SLAM_DIVE : Kind.SLAM_JUMP;
         default -> Kind.NONE;
      };
   }

   private static void apply(PlayerModel<?> model, Kind kind, float w) {
      switch (kind) {
         case LASER -> {
            rotate(model.rightArm, w, -1.45F, -0.12F, 0.0F);
            rotate(model.leftArm, w, -0.5F, 0.25F, 0.0F);
         }
         case FIST -> rotate(model.rightArm, w, -1.4F, -0.1F, 0.0F);
         case GUN -> {
            rotate(model.rightArm, w, -1.2F, -0.3F, 0.0F);
            rotate(model.leftArm, w, -1.0F, 0.45F, 0.0F);
         }
         case SLAM_JUMP -> {
            rotate(model.rightArm, w, -2.9F, 0.0F, -0.15F);
            rotate(model.leftArm, w, -2.9F, 0.0F, 0.15F);
         }
         case SLAM_DIVE -> {
            rotate(model.rightArm, w, -1.6F, 0.0F, -0.1F);
            rotate(model.leftArm, w, -1.6F, 0.0F, 0.1F);
         }
         default -> {
         }
      }
   }

   private static void rotate(ModelPart part, float w, float x, float y, float z) {
      part.xRot = Mth.lerp(w, part.xRot, x);
      part.yRot = Mth.lerp(w, part.yRot, y);
      part.zRot = Mth.lerp(w, part.zRot, z);
   }
}
