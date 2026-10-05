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
   bus = Bus.FORGE,
   value = {Dist.CLIENT}
)
public class MeteorImpactVFXManager {
   private static final List<MeteorImpactVFXManager.ImpactVFX> ACTIVE_IMPACTS = new ArrayList<>();

   public static void playImpactVFX(Vec3 pos, float yaw, float pitch) {
      ACTIVE_IMPACTS.add(new MeteorImpactVFXManager.ImpactVFX(pos));
   }

   @SubscribeEvent
   public static void onWorldRender(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_LEVEL) {
         if (!ACTIVE_IMPACTS.isEmpty()) {
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
            Tesselator tessellator = Tesselator.getInstance();
            BufferBuilder buffer = tessellator.getBuilder();
            Iterator<MeteorImpactVFXManager.ImpactVFX> it = ACTIVE_IMPACTS.iterator();
            boolean drawing = false;

            while (it.hasNext()) {
               MeteorImpactVFXManager.ImpactVFX impact = it.next();
               long age = now - impact.spawnTime;
               if (age >= 0L) {
                  if (age > 600L) {
                     it.remove();
                  } else {
                     if (!drawing) {
                        buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                        drawing = true;
                     }

                     float progress = (float)age / 600.0F;
                     float easeOut = (float)Math.sin((double)progress * Math.PI / 2.0);
                     float currentRadius = Mth.lerp(easeOut, 2.0F, 60.0F);
                     float thickness = Mth.lerp(progress, 4.0F, 0.0F);
                     float riseOffset = Mth.lerp(easeOut, 0.0F, 15.0F);
                     int alpha = (int)(255.0 * (1.0 - Math.pow((double)progress, 1.5)));
                     if (alpha < 0) {
                        alpha = 0;
                     }

                     if (alpha != 0 && !(thickness <= 0.01F)) {
                        PoseStack matrices = new PoseStack();
                        matrices.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
                        matrices.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));
                        matrices.translate(
                           impact.origin.x - camera.getPosition().x,
                           impact.origin.y + (double)riseOffset - camera.getPosition().y,
                           impact.origin.z - camera.getPosition().z
                        );
                        matrices.mulPose(Axis.XP.rotationDegrees(90.0F));
                        Matrix4f matrix = matrices.last().pose();
                        int r = 240;
                        int g = 250;
                        int b = 255;
                        float pixelWorldSize = 2.0F;
                        float outerR = currentRadius / pixelWorldSize;
                        float innerR = (currentRadius - thickness) / pixelWorldSize;
                        if (innerR < 0.0F) {
                           innerR = 0.0F;
                        }

                        int loopRadius = (int)outerR + 1;

                        for (int x = 0; x <= loopRadius; x++) {
                           for (int y = 0; y <= loopRadius; y++) {
                              float dist = (float)Math.sqrt((double)(x * x + y * y));
                              if (dist <= outerR && dist >= innerR) {
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
            }

            if (drawing) {
               tessellator.end();
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

   public static float getMeteorShakeIntensity(Vec3 cameraPos) {
      float maxShake = 0.0F;
      long now = System.currentTimeMillis();

      for (MeteorImpactVFXManager.ImpactVFX impact : ACTIVE_IMPACTS) {
         long age = now - impact.spawnTime;
         if (age >= 0L && age < 2500L) {
            float progress = (float)age / 2500.0F;
            float decay = (1.0F - progress) * (1.0F - progress);
            double dist = cameraPos.distanceTo(impact.origin);
            float distFactor = (float)Math.max(0.0, 1.0 - dist / 100.0);
            float shake = decay * distFactor * 2.0F;
            maxShake = Math.max(maxShake, shake);
         }
      }

      return maxShake;
   }

   private static class ImpactVFX {
      public final Vec3 origin;
      public final long spawnTime;

      public ImpactVFX(Vec3 origin) {
         this.origin = origin;
         this.spawnTime = System.currentTimeMillis();
      }
   }
}
