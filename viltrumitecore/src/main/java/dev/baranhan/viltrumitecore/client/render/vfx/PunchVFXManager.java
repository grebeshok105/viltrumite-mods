package dev.baranhan.viltrumitecore.client.render.vfx;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Axis;
import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
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
public class PunchVFXManager {
   @SubscribeEvent
   public static void onRenderLevel(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_LEVEL) {
         Minecraft client = Minecraft.getInstance();
         if (client.level != null) {
            for (Player player : client.level.players()) {
               if (player instanceof ViltrumiteCorePlayer) {
                  ViltrumiteCorePlayer corePlayer = (ViltrumiteCorePlayer)player;
                  int punchTicks = corePlayer.getPunchTicks();
                  if (punchTicks > 0) {
                     float tickDelta = event.getPartialTick();
                     float exactTicks = (float)punchTicks - tickDelta;
                     float time = (20.0F - exactTicks) / 20.0F;
                     time = Mth.clamp(time, 0.0F, 1.0F);
                     if (time >= 0.25F && time <= 0.65F) {
                        float shockwaveSpeed = 1.5F;
                        float progress = (time - 0.25F) / (0.4F / shockwaveSpeed);
                        if (!(progress > 1.0F)) {
                           float radius = Mth.lerp(progress, 0.5F, 6.5F);
                           int alpha = (int)(255.0F * (1.0F - progress));
                           Camera camera = event.getCamera();
                           PoseStack matrices = new PoseStack();
                           matrices.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
                           matrices.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));
                           double lerpedX = Mth.lerp((double)tickDelta, player.xo, player.getX());
                           double lerpedY = Mth.lerp((double)tickDelta, player.yo, player.getY());
                           double lerpedZ = Mth.lerp((double)tickDelta, player.zo, player.getZ());
                           Vec3 lookVec = player.getViewVector(tickDelta);
                           Vec3 impactCenter = new Vec3(lerpedX, lerpedY + (double)player.getEyeHeight(), lerpedZ).add(lookVec.scale(1.5));
                           matrices.translate(
                              impactCenter.x - camera.getPosition().x,
                              impactCenter.y - camera.getPosition().y,
                              impactCenter.z - camera.getPosition().z
                           );
                           matrices.mulPose(Axis.YP.rotationDegrees(-player.getViewYRot(tickDelta)));
                           matrices.mulPose(Axis.XP.rotationDegrees(player.getViewXRot(tickDelta)));
                           PoseStack modelViewStack = RenderSystem.getModelViewStack();
                           modelViewStack.pushPose();
                           modelViewStack.setIdentity();
                           RenderSystem.applyModelViewMatrix();
                           RenderSystem.enableBlend();
                           RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
                           RenderSystem.disableCull();
                           RenderSystem.enableDepthTest();
                           RenderSystem.depthMask(false);
                           RenderSystem.setShader(GameRenderer::getPositionColorShader);
                           Tesselator tesselator = Tesselator.getInstance();
                           BufferBuilder buffer = tesselator.getBuilder();
                           buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                           Matrix4f matrix = matrices.last().pose();
                           int r = 240;
                           int g = 250;
                           int b = 255;
                           float fPR = Math.max(1.0F, Mth.lerp(progress, 4.0F, 32.0F));
                           float pixelWorldSize = radius / fPR;
                           float fCW = 2.0F;
                           float outerR = fPR;
                           float innerR = Math.max(0.0F, fPR - fCW);
                           int loopRadius = (int)fPR + 1;

                           for (int x = 0; x <= loopRadius; x++) {
                              for (int y = 0; y <= loopRadius; y++) {
                                 float dist = (float)Math.sqrt((double)(x * x + y * y));
                                 if (dist <= outerR + 0.3F && dist >= innerR - 0.3F) {
                                    drawPaintPixel(matrix, buffer, x, y, pixelWorldSize, r, g, b, alpha);
                                    if (x != 0) {
                                       drawPaintPixel(matrix, buffer, -x, y, pixelWorldSize, r, g, b, alpha);
                                    }

                                    if (y != 0) {
                                       drawPaintPixel(matrix, buffer, x, -y, pixelWorldSize, r, g, b, alpha);
                                    }

                                    if (x != 0 && y != 0) {
                                       drawPaintPixel(matrix, buffer, -x, -y, pixelWorldSize, r, g, b, alpha);
                                    }
                                 }
                              }
                           }

                           tesselator.end();
                           RenderSystem.depthMask(true);
                           RenderSystem.enableCull();
                           RenderSystem.defaultBlendFunc();
                           RenderSystem.disableBlend();
                           modelViewStack.popPose();
                           RenderSystem.applyModelViewMatrix();
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static void drawPaintPixel(Matrix4f matrix, BufferBuilder buffer, int gridX, int gridY, float pixelScale, int r, int g, int b, int alpha) {
      float cx = (float)gridX * pixelScale;
      float cy = (float)gridY * pixelScale;
      float half = pixelScale * 0.5F;
      buffer.vertex(matrix, cx - half, cy - half, 0.0F).color(r, g, b, alpha).endVertex();
      buffer.vertex(matrix, cx + half, cy - half, 0.0F).color(r, g, b, alpha).endVertex();
      buffer.vertex(matrix, cx + half, cy + half, 0.0F).color(r, g, b, alpha).endVertex();
      buffer.vertex(matrix, cx - half, cy + half, 0.0F).color(r, g, b, alpha).endVertex();
   }
}
