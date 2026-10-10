package dev.baranhan.viltrumitecore.client.ironman.hulkbuster;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.baranhan.viltrumitecore.client.anim.AnimCache;
import dev.baranhan.viltrumitecore.client.anim.animation.Animation;
import dev.baranhan.viltrumitecore.client.anim.animation.AnimationController;
import dev.baranhan.viltrumitecore.client.anim.geo.BakedGeoModel;
import dev.baranhan.viltrumitecore.client.anim.geo.GeoBone;
import dev.baranhan.viltrumitecore.client.anim.render.AnimRenderer;
import dev.baranhan.viltrumitecore.client.anim.render.PlayerGeoLayer;
import dev.baranhan.viltrumitecore.client.ironman.IronManView;
import dev.baranhan.viltrumitecore.client.ironman.mark.Mark42Parts;
import dev.baranhan.viltrumitecore.hero.HeroAction;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import dev.baranhan.viltrumitecore.hero.ironman.IronManFlags;
import dev.baranhan.viltrumitecore.hero.ironman.combat.RightTool;
import dev.baranhan.viltrumitecore.hero.ironman.hulkbuster.HulkbusterLayer;
import dev.baranhan.viltrumiteflight.client.util.ShaderCompat;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

/**
 * Mark 48 on the player (spec §14, plan stage 5 Task 4). While ACTIVE the vanilla
 * body is cancelled and the big model is drawn at the player transform ×1.7; while
 * EXITING the shell opens its back and Tony steps out backwards. First person draws
 * the two big arms instead of the vanilla arms. Client only.
 */
@EventBusSubscriber(modid = "viltrumitecore", bus = Bus.FORGE, value = {Dist.CLIENT})
public final class HulkbusterRenderer {
   private static final float SCALE = HulkbusterLayer.SCALE * HulkbusterAssets.SIND_UNITS;
   /** Tony's step out of the open back, blocks (entity-local, backwards). */
   private static final float STEP_OUT_BLOCKS = 1.1F;
   private static final float STEP_OUT_START = 0.5F;
   private static final float STEP_OUT_TIME = 1.0F;
   private static final float HEAD_YAW_LIMIT = 70.0F;
   private static final float HEAD_PITCH_LIMIT = 45.0F;
   /** Ground contact time of an armed slam: when the smash clip starts (synced action, render clock). */
   private static final Map<AbstractClientPlayer, Double> GROUNDED_SINCE = new WeakHashMap<>();

   private HulkbusterRenderer() {
   }

   @SubscribeEvent
   public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
      if (!(event.getEntity() instanceof AbstractClientPlayer player) || player.isInvisible()) {
         return;
      }

      HeroPublicSnapshot snapshot = IronManView.of(player);
      if (snapshot == null) {
         return;
      }

      HulkbusterLayer.Phase phase = HulkbusterView.phase(snapshot);
      if (!HulkbusterView.big(phase)) {
         return;
      }

      float partialTick = event.getPartialTick();
      HulkbusterPoses.Pose pose = pose(player, snapshot, phase, partialTick);
      if (pose == null) {
         return;
      }

      PoseStack stack = event.getPoseStack();
      drawBody(player, pose, jackhammer(snapshot), stack, event.getMultiBufferSource(), event.getPackedLight(), partialTick);
      if (phase == HulkbusterLayer.Phase.ACTIVE) {
         event.setCanceled(true);
         return;
      }

      // Entity-local offset: applied before vanilla's body rotation, so it moves Tony backwards from the shell.
      float out = Mth.clamp(((float)pose.overlaySeconds() - STEP_OUT_START) / STEP_OUT_TIME, 0.0F, 1.0F);
      stack.translate(0.0, 0.0, STEP_OUT_BLOCKS * smooth(out));
   }

   @SubscribeEvent
   public static void onRenderArm(RenderArmEvent event) {
      AbstractClientPlayer player = event.getPlayer();
      HeroPublicSnapshot snapshot = IronManView.of(player);
      if (snapshot == null) {
         return;
      }

      HulkbusterLayer.Phase phase = HulkbusterView.phase(snapshot);
      if (!HulkbusterView.big(phase)) {
         return;
      }

      float partialTick = Minecraft.getInstance().getFrameTime();
      HulkbusterPoses.Pose pose = pose(player, snapshot, phase, partialTick);
      if (pose == null) {
         return;
      }

      PlayerRenderer renderer = (PlayerRenderer)Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
      PlayerModel<AbstractClientPlayer> model = renderer.getModel();
      ModelPart arm = event.getArm() == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
      float glow = pose.glow();
      PlayerGeoLayer.Part part = new PlayerGeoLayer.Part(HulkbusterAssets.FP_ARM,
         List.of(PlayerGeoLayer.Pass.cutout(HulkbusterAssets.TEXTURE), PlayerGeoLayer.Pass.glow(HulkbusterAssets.GLOW, glow, glow, glow)),
         geo -> applyClips(geo, pose));
      PlayerGeoLayer.renderArmPart(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), model, arm, part);
      event.setCanceled(true);
   }

   /** Sind arm switch: the jackhammer arm replaces the left arm unless the slow repulsors are selected. */
   private static boolean jackhammer(HeroPublicSnapshot snapshot) {
      return IronManView.tool(snapshot) != RightTool.HULK_REPULSOR;
   }

   private static void drawBody(AbstractClientPlayer player, HulkbusterPoses.Pose pose, boolean jackhammer, PoseStack stack, MultiBufferSource buffers,
      int light, float partialTick) {
      BakedGeoModel body = AnimCache.model(HulkbusterAssets.BODY);
      if (body == null) {
         return;
      }

      applyClips(body, pose);
      applyHead(body, player, partialTick);
      GeoBone leftArm = body.getBone(HulkbusterAssets.LEFT_ARM_BONE);
      if (leftArm != null) {
         leftArm.hidden = jackhammer;
      }

      stack.pushPose();
      stack.mulPose(Axis.YP.rotationDegrees(180.0F - Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot)));
      stack.scale(SCALE, SCALE, SCALE);
      AnimRenderer.render(body, stack, null, buffers.getBuffer(RenderType.entityCutoutNoCull(HulkbusterAssets.TEXTURE)), light, OverlayTexture.NO_OVERLAY,
         1.0F, 1.0F, 1.0F, 1.0F, null);
      if (!ShaderCompat.isShadowPass()) {
         float glow = pose.glow();
         AnimRenderer.render(body, stack, null, buffers.getBuffer(RenderType.eyes(HulkbusterAssets.GLOW)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
            glow, glow, glow, 1.0F, null);
      }

      BakedGeoModel arm = jackhammer ? AnimCache.model(HulkbusterAssets.JACKHAMMER) : null;
      if (arm != null) {
         applyClips(arm, pose);
         AnimRenderer.render(arm, stack, null, buffers.getBuffer(RenderType.entityCutoutNoCull(HulkbusterAssets.JACKHAMMER_TEXTURE)), light,
            OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F, null);
         if (!ShaderCompat.isShadowPass()) {
            AnimRenderer.render(arm, stack, null, buffers.getBuffer(RenderType.eyes(HulkbusterAssets.JACKHAMMER_GLOW)), LightTexture.FULL_BRIGHT,
               OverlayTexture.NO_OVERLAY, pose.glow(), pose.glow(), pose.glow(), 1.0F, null);
         }
      }

      if (pose.flames() > 0.01F && !ShaderCompat.isShadowPass()) {
         drawFire(jackhammer ? HulkbusterAssets.JACKHAMMER_FIRE : null, pose, stack, buffers, player.tickCount);
      }

      stack.popPose();
   }

   /** Sind fire models (feet, hands, shoulders) posed like the body; brightness = thruster strength. */
   private static void drawFire(@Nullable net.minecraft.resources.ResourceLocation extra, HulkbusterPoses.Pose pose, PoseStack stack, MultiBufferSource buffers,
      int tick) {
      float k = pose.flames();
      for (net.minecraft.resources.ResourceLocation location : new net.minecraft.resources.ResourceLocation[]{HulkbusterAssets.FIRE, extra}) {
         BakedGeoModel fire = location == null ? null : AnimCache.model(location);
         if (fire != null) {
            applyClips(fire, pose);
            AnimRenderer.render(fire, stack, null, buffers.getBuffer(RenderType.eyes(Mark42Parts.fireTexture(tick / 2))), LightTexture.FULL_BRIGHT,
               OverlayTexture.NO_OVERLAY, k, k, k, 1.0F, null);
         }
      }
   }

   /** Idle/walk blend by speed, then the overlay clip on the bones it animates (same for body and first-person arm). */
   static void applyClips(BakedGeoModel model, HulkbusterPoses.Pose pose) {
      Animation idle = AnimCache.animation(HulkbusterAssets.ANIMATIONS, HulkbusterPoses.Clip.IDLE.animation());
      Animation walk = AnimCache.animation(HulkbusterAssets.ANIMATIONS, HulkbusterPoses.Clip.WALK.animation());
      if (idle != null && walk != null) {
         AnimationController.blend(model, idle, pose.idleSeconds(), walk, pose.walkSeconds(), pose.walk());
      }

      HulkbusterPoses.Clip overlay = pose.overlay();
      if (overlay != null) {
         Animation clip = AnimCache.animation(HulkbusterAssets.ANIMATIONS, overlay.animation());
         if (clip != null) {
            AnimationController.overlay(clip, model, pose.overlaySeconds());
         }
      }
   }

   /** The head follows the look direction (vanilla yaw relative to the body, pitch); internal turns are negated. */
   private static void applyHead(BakedGeoModel body, AbstractClientPlayer player, float partialTick) {
      GeoBone head = body.getBone(HulkbusterAssets.HEAD_BONE);
      if (head == null) {
         return;
      }

      float yaw = Mth.clamp(Mth.wrapDegrees(player.yHeadRot - Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot)), -HEAD_YAW_LIMIT, HEAD_YAW_LIMIT);
      float pitch = Mth.clamp(Mth.lerp(partialTick, player.xRotO, player.getXRot()), -HEAD_PITCH_LIMIT, HEAD_PITCH_LIMIT);
      head.rotY += (float)Math.toRadians(-yaw);
      head.rotX += (float)Math.toRadians(-pitch);
   }

   @Nullable
   static HulkbusterPoses.Pose pose(AbstractClientPlayer player, HeroPublicSnapshot snapshot, HulkbusterLayer.Phase phase, float partialTick) {
      double wall = AnimRenderer.time(partialTick) / 20.0;
      HulkbusterPoses.Input input = new HulkbusterPoses.Input(phase.ordinal(), snapshot.actionId(), snapshot.actionElapsed(), snapshot.actionLength(),
         partialTick, IronManView.tool(snapshot), IronManView.flag(snapshot, IronManFlags.Field.SHOT_HAND) != 0, groundedSeconds(player, snapshot, wall),
         player.walkAnimation.speed(partialTick), player.walkAnimation.position(partialTick), wall);
      return HulkbusterPoses.select(input);
   }

   /** Seconds since an armed slam touched the ground after its jump (-1 otherwise). Render thread only. */
   private static double groundedSeconds(AbstractClientPlayer player, HeroPublicSnapshot snapshot, double wall) {
      boolean smashing = snapshot.actionId() == HeroAction.MISSILES.ordinal() && snapshot.actionElapsed() >= HulkbusterPoses.SLAM_LAND_TICKS && player.onGround();
      if (!smashing) {
         GROUNDED_SINCE.remove(player);
         return -1.0;
      }

      return wall - GROUNDED_SINCE.computeIfAbsent(player, p -> wall);
   }

   private static float smooth(float t) {
      return t * t * (3.0F - 2.0F * t);
   }
}
