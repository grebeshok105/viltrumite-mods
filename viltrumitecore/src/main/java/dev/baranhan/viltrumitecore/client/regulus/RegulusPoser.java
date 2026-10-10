package dev.baranhan.viltrumitecore.client.regulus;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.baranhan.viltrumitecore.client.anim.pose.PoseRig;
import dev.baranhan.viltrumitecore.hero.HeroId;
import dev.baranhan.viltrumitecore.hero.HeroPlayer;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import dev.baranhan.viltrumiteflight.client.util.ShaderCompat;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Regulus entry of the shared pose rig ({@link PoseRig}, moved out of this
 * class when Iron Man started to use it): the Regulus snapshot gates plus
 * thin delegates, so the per-ability Regulus mixins keep their calls.
 */
public final class RegulusPoser {
   private RegulusPoser() {
   }

   /** Regulus snapshot or null; non-Regulus players are never touched. */
   @Nullable
   public static HeroPublicSnapshot regulus(LivingEntity entity) {
      if (entity instanceof HeroPlayer heroPlayer) {
         HeroPublicSnapshot snapshot = heroPlayer.getHeroSnapshot();
         if (snapshot != null && snapshot.heroId() == HeroId.REGULUS) {
            return snapshot;
         }
      }
      return null;
   }

   /**
    * Snapshot for a third-person model pass: null for non-Regulus and for the
    * local first-person player outside the shader shadow pass.
    */
   @Nullable
   public static HeroPublicSnapshot thirdPerson(LivingEntity entity) {
      HeroPublicSnapshot snapshot = regulus(entity);
      if (snapshot == null) {
         return null;
      }
      Minecraft minecraft = Minecraft.getInstance();
      boolean localFirstPerson = entity == minecraft.player && minecraft.options.getCameraType().isFirstPerson();
      return localFirstPerson && !ShaderCompat.isShadowPass() ? null : snapshot;
   }

   /** Undo pivot offsets the vanilla setupAnim never resets (head/body x,z, leg x). */
   public static void restorePivots(PlayerModel<?> model) {
      PoseRig.restorePivots(model);
   }

   /** Per-frame reset done once, by the lowest-priority Regulus layer. */
   public static void beginFrame(PlayerModel<?> model) {
      PoseRig.beginFrame(model);
   }

   public static PoseRig rig(PlayerModel<?> model, LivingEntity entity, @Nullable ModelPart cloak) {
      return PoseRig.begin(model, entity, cloak);
   }

   /** Rig mirrored by an explicit side (+1 right, -1 left), e.g. the book hand. */
   public static PoseRig rig(PlayerModel<?> model, LivingEntity entity, float side, @Nullable ModelPart cloak) {
      return PoseRig.begin(model, entity, side, cloak);
   }

   /** +1 for a right-side hand, -1 for left. */
   public static float handSide(Player player, InteractionHand hand) {
      return PoseRig.handSide(player, hand);
   }

   /** First-person arm/item transform, see {@link PoseRig#firstPerson}. */
   public static void firstPerson(PoseStack poseStack, float[][] keys, float elapsed, float weight, float side) {
      PoseRig.firstPerson(poseStack, keys, elapsed, weight, side);
   }

   public static void firstPersonRaw(PoseStack poseStack, float pitch, float yaw, float roll, float x, float y, float z) {
      PoseRig.firstPersonRaw(poseStack, pitch, yaw, roll, x, y, z);
   }
}
