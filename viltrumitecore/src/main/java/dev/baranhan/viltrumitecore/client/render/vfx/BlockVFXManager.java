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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
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
   value = {Dist.CLIENT},
   bus = Bus.FORGE
)
public class BlockVFXManager {
   private static final List<BlockVFXManager.BlockRing> ACTIVE_RINGS = new ArrayList<>();
   private static long lastBlockShakeTime = 0L;

   public static void spawnRing(Vec3 pos, float yaw, float pitch) {
      ACTIVE_RINGS.add(new BlockVFXManager.BlockRing(pos, yaw, pitch, System.currentTimeMillis()));
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.player != null && minecraft.player.distanceToSqr(pos) <= 36.0) {
         lastBlockShakeTime = System.currentTimeMillis();
      }
   }

   public static float getBlockShakeIntensity() {
      if (lastBlockShakeTime == 0L) {
         return 0.0F;
      } else {
         long elapsed = System.currentTimeMillis() - lastBlockShakeTime;
         if (elapsed > 250L) {
            return 0.0F;
         } else {
            float normalized = 1.0F - (float)elapsed / 250.0F;
            return normalized * normalized * 0.4F;
         }
      }
   }

   @SubscribeEvent
   public static void onRenderWorld(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_LEVEL) {
         if (!ACTIVE_RINGS.isEmpty()) {
            Camera camera = event.getCamera();
            long now = System.currentTimeMillis();
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
            Iterator<BlockVFXManager.BlockRing> it = ACTIVE_RINGS.iterator();
            boolean drawing = false;

            while (it.hasNext()) {
               BlockVFXManager.BlockRing ring = it.next();
               long age = now - ring.spawnTime;
               if (age >= 0L) {
                  if (age > 200L) {
                     it.remove();
                  } else {
                     if (!drawing) {
                        buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                        drawing = true;
                     }

                     float progress = (float)age / 200.0F;
                     float radius = Mth.lerp(progress, 0.5F, 3.5F);
                     int alpha = (int)(255.0 * Math.max(0.0, 1.0 - Math.pow((double)progress, 2.0)));
                     PoseStack poseStack = new PoseStack();
                     poseStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
                     poseStack.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));
                     poseStack.translate(
                        ring.pos.x - camera.getPosition().x,
                        ring.pos.y - camera.getPosition().y,
                        ring.pos.z - camera.getPosition().z
                     );
                     poseStack.mulPose(Axis.YP.rotationDegrees(-ring.yaw));
                     poseStack.mulPose(Axis.XP.rotationDegrees(ring.pitch));
                     Matrix4f matrix = poseStack.last().pose();
                     int r = 255;
                     int g = 235;
                     int b = 205;
                     float fPR = Mth.lerp(progress, 8.0F, 24.0F);
                     if (fPR < 1.0F) {
                        fPR = 1.0F;
                     }

                     float pixelWorldSize = radius / fPR;
                     float fCW = 1.2F;
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
                  }
               }
            }

            if (drawing) {
               tesselator.end();
            }

            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            modelViewStack.popPose();
            RenderSystem.applyModelViewMatrix();
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

   private static class BlockRing {
      public final Vec3 pos;
      public final float yaw;
      public final float pitch;
      public final long spawnTime;

      public BlockRing(Vec3 pos, float yaw, float pitch, long spawnTime) {
         this.pos = pos;
         this.yaw = yaw;
         this.pitch = pitch;
         this.spawnTime = spawnTime;
      }
   }
}
