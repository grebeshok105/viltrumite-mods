package dev.baranhan.viltrumitecore.client.ironman.hulkbuster;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.baranhan.viltrumitecore.client.anim.AnimCache;
import dev.baranhan.viltrumitecore.client.anim.geo.BakedGeoModel;
import dev.baranhan.viltrumitecore.client.anim.geo.GeoBone;
import dev.baranhan.viltrumitecore.client.anim.render.AnimRenderer;
import dev.baranhan.viltrumitecore.client.ironman.IronManView;
import dev.baranhan.viltrumitecore.client.ironman.veronica.PartFlight;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import dev.baranhan.viltrumitecore.hero.ironman.IronManVariant;
import dev.baranhan.viltrumitecore.hero.ironman.hulkbuster.HulkbusterLayer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

/**
 * Hulkbuster parts flying from Veronica (or the sky) onto the player during the
 * drop and the assembly (spec §12.4, plan Task 5). Groups that are not locked yet
 * fly on {@link PartFlight} paths and tumble until they reach the body; locked
 * groups are drawn on the player by {@link HulkbusterDocking}.
 */
@EventBusSubscriber(modid = "viltrumitecore", bus = Bus.FORGE, value = {Dist.CLIENT})
public final class HulkbusterFlight {
   private HulkbusterFlight() {
   }

   @SubscribeEvent
   public static void onRenderLevel(RenderLevelStageEvent event) {
      if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
         return;
      }

      Minecraft client = Minecraft.getInstance();
      ClientLevel level = client.level;
      BakedGeoModel geo = AnimCache.model(HulkbusterAssets.PARTS);
      if (level == null || geo == null) {
         return;
      }

      float partialTick = event.getPartialTick();
      Camera camera = event.getCamera();
      Vec3 cameraPos = camera.getPosition();
      PoseStack stack = event.getPoseStack();
      MultiBufferSource.BufferSource buffers = client.renderBuffers().bufferSource();
      for (Player player : level.players()) {
         HeroPublicSnapshot snapshot = IronManView.of(player);
         if (snapshot == null) {
            continue;
         }

         HulkbusterLayer.Phase phase = HulkbusterView.phase(snapshot);
         if (phase != HulkbusterLayer.Phase.DROPPING && phase != HulkbusterLayer.Phase.ASSEMBLING) {
            continue;
         }

         int mask = IronManVariant.hulkParts(snapshot.variant());
         Vec3 source = snapshot.actionTarget() != null ? snapshot.actionTarget() : player.position().add(0.0, 6.0, 0.0);
         double ticks = HulkbusterAssembly.dropTicks(phase.ordinal(), snapshot.actionElapsed() + partialTick);
         Vec3 feet = player.getPosition(partialTick);
         float bodyYaw = 180.0F - Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
         for (int group = 0; group < HulkbusterAssembly.GROUPS; group++) {
            if (HulkbusterAssembly.locked(mask, group)) {
               continue;
            }

            float progress = HulkbusterAssembly.flightProgress(group, ticks);
            double centre = HulkbusterAssembly.centreAboveFeet(group);
            Vec3 at = PartFlight.position(source, feet.add(0.0, centre, 0.0), progress);
            drawGroup(geo, group, stack, buffers, cameraPos, at, bodyYaw, PartFlight.tumble(progress), centre);
         }
      }

      buffers.endBatch();
   }

   private static void drawGroup(BakedGeoModel geo, int group, PoseStack stack, MultiBufferSource buffers, Vec3 cameraPos, Vec3 at, float bodyYaw,
      float tumble, double centre) {
      geo.resetBones();
      for (GeoBone bone : geo.allBones()) {
         bone.hidden = false;
      }

      stack.pushPose();
      stack.translate(at.x - cameraPos.x, at.y - cameraPos.y, at.z - cameraPos.z);
      stack.mulPose(Axis.YP.rotationDegrees(bodyYaw));
      stack.mulPose(Axis.XP.rotation(tumble * 1.4F));
      stack.mulPose(Axis.ZP.rotation(tumble * 1.1F));
      stack.translate(0.0, -centre, 0.0);
      VertexConsumer cutout = buffers.getBuffer(RenderType.entityCutoutNoCull(HulkbusterAssets.TEXTURE));
      VertexConsumer glow = buffers.getBuffer(RenderType.eyes(HulkbusterAssets.GLOW));
      for (String name : HulkbusterAssembly.BONES[group]) {
         GeoBone bone = geo.getBone(name);
         if (bone != null) {
            AnimRenderer.renderBone(bone, stack, cutout, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            AnimRenderer.renderBone(bone, stack, glow, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
         }
      }

      stack.popPose();
   }
}
