package dev.baranhan.viltrumitecore.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Axis;
import dev.baranhan.viltrumitecore.client.TargetLockManager;
import dev.baranhan.viltrumitecore.config.ViltrumiteCameraConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.joml.Matrix4f;

@EventBusSubscriber(
   modid = "viltrumitecore",
   bus = Bus.FORGE,
   value = {Dist.CLIENT}
)
public class ThirdPersonCrosshairRenderer {
   @SubscribeEvent
   public static void onRenderLevel(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_PARTICLES) {
         Minecraft client = Minecraft.getInstance();
         if (client.player != null && client.level != null) {
            if (!client.options.getCameraType().isFirstPerson()) {
               if (TargetLockManager.lockedTarget == null) {
                  if (ViltrumiteCameraConfig.INSTANCE.enableThirdPersonCrosshair) {
                     float tickDelta = event.getPartialTick();
                     Camera camera = event.getCamera();
                     Vec3 eye = client.player.getEyePosition(tickDelta);
                     Vec3 look = client.player.getViewVector(tickDelta);
                     float distance = 5.0F;
                     Vec3 targetPos = eye.add(look.scale((double)distance));
                     PoseStack poseStack = event.getPoseStack();
                     poseStack.pushPose();
                     Vec3 cameraPos = camera.getPosition();
                     poseStack.translate(
                        targetPos.x - cameraPos.x, targetPos.y - cameraPos.y, targetPos.z - cameraPos.z
                     );
                     poseStack.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
                     poseStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
                     float scale = 0.225F;
                     poseStack.scale(scale, scale, scale);
                     RenderSystem.enableBlend();
                     RenderSystem.defaultBlendFunc();
                     RenderSystem.disableCull();
                     RenderSystem.disableDepthTest();
                     RenderSystem.setShader(GameRenderer::getPositionColorShader);
                     Tesselator tesselator = Tesselator.getInstance();
                     BufferBuilder buffer = tesselator.getBuilder();
                     buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                     Matrix4f matrix = poseStack.last().pose();
                     int r = 255;
                     int g = 255;
                     int b = 255;
                     int a = 255;
                     float thick = 0.08F;
                     float len = 0.6F;
                     buffer.vertex(matrix, -len, -thick, 0.0F).color(r, g, b, a).endVertex();
                     buffer.vertex(matrix, len, -thick, 0.0F).color(r, g, b, a).endVertex();
                     buffer.vertex(matrix, len, thick, 0.0F).color(r, g, b, a).endVertex();
                     buffer.vertex(matrix, -len, thick, 0.0F).color(r, g, b, a).endVertex();
                     buffer.vertex(matrix, -thick, -len, 0.0F).color(r, g, b, a).endVertex();
                     buffer.vertex(matrix, thick, -len, 0.0F).color(r, g, b, a).endVertex();
                     buffer.vertex(matrix, thick, len, 0.0F).color(r, g, b, a).endVertex();
                     buffer.vertex(matrix, -thick, len, 0.0F).color(r, g, b, a).endVertex();
                     tesselator.end();
                     RenderSystem.enableDepthTest();
                     RenderSystem.enableCull();
                     RenderSystem.disableBlend();
                     poseStack.popPose();
                  }
               }
            }
         }
      }
   }
}
