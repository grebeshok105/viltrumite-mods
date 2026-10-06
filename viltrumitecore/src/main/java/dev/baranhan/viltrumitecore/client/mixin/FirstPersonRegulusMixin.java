package dev.baranhan.viltrumitecore.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.baranhan.viltrumitecore.client.regulus.RegulusPoseTiming;
import dev.baranhan.viltrumitecore.client.render.animation.RegulusAnimationManager;
import dev.baranhan.viltrumitecore.hero.HeroAction;
import dev.baranhan.viltrumitecore.hero.HeroId;
import dev.baranhan.viltrumitecore.hero.HeroPlayer;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * First-person Regulus arm poses (spec 13.3). Drives the main hand from the
 * same snapshot actions as RegulusModelMixin; continuous states share the
 * weight manager (this view is the only caller while first person, so the
 * weight advances once per frame per entity).
 */
@Mixin(
   value = {ItemInHandRenderer.class},
   priority = 1520
)
public abstract class FirstPersonRegulusMixin {
   @Inject(
      method = {"renderArmWithItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
         shift = Shift.AFTER
      )}
   )
   private void regulusRenderArmWithItem(
      AbstractClientPlayer player,
      float partialTicks,
      float pitch,
      InteractionHand hand,
      float swingProgress,
      ItemStack stack,
      float equipProgress,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int combinedLight,
      CallbackInfo ci
   ) {
      if (!(player instanceof HeroPlayer heroPlayer)) {
         return;
      }

      HeroPublicSnapshot snapshot = heroPlayer.getHeroSnapshot();
      if (snapshot == null || snapshot.heroId() != HeroId.REGULUS) {
         return;
      }

      HumanoidArm currentArm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
      boolean mainArm = currentArm == player.getMainArm();
      float m = player.getMainArm() == HumanoidArm.LEFT ? -1.0F : 1.0F;

      // Continuous states apply to whichever arm renders the action pose.
      float ritualWeight = RegulusAnimationManager.calculateWeight(
         player, RegulusAnimationManager.Pose.RITUAL, snapshot.ritualTicks() >= 0
      );
      if (ritualWeight > 0.01F && mainArm) {
         this.applyFirstPerson(poseStack, ritualWeight, -0.12F * m, 0.08F, -0.06F, -18.0F, 32.0F * m, -12.0F * m);
      }

      float channelWeight = RegulusAnimationManager.calculateWeight(
         player, RegulusAnimationManager.Pose.CHANNEL, snapshot.controlTargetId() >= 0
      );
      if (channelWeight > 0.01F && mainArm) {
         this.applyFirstPerson(poseStack, channelWeight, 0.0F, 0.02F, -0.3F, -42.0F, -14.0F * m, 0.0F);
      }

      float lionWeight = RegulusAnimationManager.calculateWeight(
         player, RegulusAnimationManager.Pose.LION, snapshot.lionActive()
      );
      if (lionWeight > 0.01F && mainArm) {
         this.applyFirstPerson(poseStack, lionWeight, 0.06F * m, -0.04F, -0.02F, 6.0F, 8.0F * m, -6.0F * m);
      }

      HeroAction action = HeroAction.byId(snapshot.actionId());
      if (action == null || !mainArm) {
         return;
      }

      RegulusPoseTiming.Timing timing = RegulusPoseTiming.timing(action);
      boolean eventPassed = RegulusPoseTiming.eventPassed(action, snapshot.actionElapsed(), partialTicks);
      float eventTick = (float)timing.eventTick();
      float elapsed = (float)snapshot.actionElapsed() + partialTicks;
      switch (action) {
         case LIONS_HEART:
            // Hand presses to the chest, clench at the event tick.
            if (elapsed < 8.0F) {
               float t = elapsed / 8.0F;
               this.applyFirstPerson(poseStack, t, 0.16F * m, 0.1F, -0.12F, -22.0F, 30.0F * m, -18.0F * m);
            } else {
               float clench = eventPassed ? 1.0F - Mth.clamp(elapsed - eventTick, 0.0F, 1.0F) : 1.0F;
               this.applyFirstPerson(poseStack, clench, 0.16F * m, 0.1F, -0.12F, -22.0F, 30.0F * m, (-18.0F - 10.0F * clench) * m);
            }
            break;
         case DEBRIS_KICK: {
            // Counter-swing dip while the leg stomps.
            float riseTick = (float)RegulusPoseTiming.DEBRIS_RISE_TICK;
            float dip;
            if (elapsed < riseTick) {
               dip = elapsed / riseTick;
            } else if (!eventPassed) {
               dip = 1.0F + (elapsed - riseTick) / (eventTick - riseTick) * 0.2F;
            } else {
               dip = Math.max(0.0F, 1.0F - (elapsed - eventTick) / 18.0F);
            }

            this.applyFirstPerson(poseStack, dip * 0.8F, 0.02F * m, -0.14F, 0.02F, -26.0F, -8.0F * m, 4.0F * m);
            break;
         }
         case MANIA: {
            // Arm reaches forward; slight extra reach + clench at the event tick.
            float reach = elapsed < 17.0F ? elapsed / 17.0F : 1.0F;
            float clench = eventPassed ? 1.0F : 0.0F;
            this.applyFirstPerson(poseStack, reach, 0.0F, 0.05F, -0.34F, -52.0F - 6.0F * clench, -18.0F * m, -4.0F * clench * m);
            break;
         }
         case GREEDS_EMBRACE: {
            // Arm rises, opens at the event tick, lowers through the recover tail.
            float lockTick = (float)RegulusPoseTiming.EMBRACE_LOCK_TICK;
            float recoverTicks = (float)(timing.unlockTick() - timing.eventTick());
            if (elapsed < lockTick) {
               float t = elapsed / lockTick;
               this.applyFirstPerson(poseStack, t, 0.04F * m, 0.22F, -0.18F, -96.0F, -12.0F * m, 8.0F * m);
            } else if (!eventPassed) {
               this.applyFirstPerson(poseStack, 1.0F, 0.04F * m, 0.22F, -0.18F, -96.0F, -12.0F * m, 8.0F * m);
            } else {
               float t = Mth.clamp((elapsed - eventTick) / recoverTicks, 0.0F, 1.0F);
               this.applyFirstPerson(poseStack, 1.0F - t, 0.04F * m, 0.22F, -0.18F, -96.0F, -12.0F * m, 8.0F * m);
            }
            break;
         }
         case COUNTER: {
            // Raise with the lift, slam down at the event tick.
            float liftTick = (float)RegulusPoseTiming.COUNTER_LIFT_TICKS;
            if (elapsed < liftTick) {
               float t = elapsed / liftTick;
               this.applyFirstPerson(poseStack, t, 0.02F * m, 0.18F, -0.15F, -80.0F, -10.0F * m, 6.0F * m);
            } else if (!eventPassed) {
               float t = (elapsed - liftTick) / (eventTick - liftTick);
               float down = t * t;
               this.applyFirstPerson(poseStack, 1.0F, 0.02F * m, 0.18F - 0.5F * down, -0.15F + 0.25F * down, -80.0F + 125.0F * down, -10.0F * m, 6.0F * m);
            } else {
               this.applyFirstPerson(poseStack, Math.max(0.0F, 1.0F - (elapsed - eventTick)), 0.02F * m, -0.32F, 0.1F, 45.0F, -10.0F * m, 6.0F * m);
            }
            break;
         }
         default:
            break;
      }
   }

   @Unique
   private void applyFirstPerson(PoseStack poseStack, float weight, float x, float y, float z, float rx, float ry, float rz) {
      poseStack.translate(x * weight, y * weight, z * weight);
      poseStack.mulPose(new Quaternionf().rotateX((float)Math.toRadians((double)(rx * weight))));
      poseStack.mulPose(new Quaternionf().rotateY((float)Math.toRadians((double)(ry * weight))));
      poseStack.mulPose(new Quaternionf().rotateZ((float)Math.toRadians((double)(rz * weight))));
   }
}
