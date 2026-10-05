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
public class BarrageVFXManager {
   private static final List<BarrageVFXManager.BarrageSpark> SPARKS = new ArrayList<>();

   public static void addSpark(Vec3 pos) {
      SPARKS.add(new BarrageVFXManager.BarrageSpark(pos));
   }

   @SubscribeEvent
   public static void onWorldRender(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_PARTICLES) {
         if (!SPARKS.isEmpty()) {
            Camera camera = event.getCamera();
            PoseStack matrices = event.getPoseStack();
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
            RenderSystem.disableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            Tesselator tessellator = Tesselator.getInstance();
            BufferBuilder buffer = tessellator.getBuilder();
            buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            Iterator<BarrageVFXManager.BarrageSpark> sparkIt = SPARKS.iterator();

            while (sparkIt.hasNext()) {
               BarrageVFXManager.BarrageSpark spark = sparkIt.next();
               float progress = (float)spark.age / (float)spark.maxAge;
               float sparkScale = 1.0F - progress * progress;
               int sparkAlpha = (int)(255.0F * (1.0F - progress));
               matrices.pushPose();
               matrices.translate(
                  spark.pos.x - camera.getPosition().x,
                  spark.pos.y - camera.getPosition().y,
                  spark.pos.z - camera.getPosition().z
               );
               matrices.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
               matrices.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
               matrices.mulPose(Axis.ZP.rotationDegrees(spark.rotation));
               Matrix4f matrix = matrices.last().pose();
               int sr = 255;
               int sg = 255;
               int sb = 220;
               if (sparkAlpha > 0) {
                  float sparkLength = 0.6F * sparkScale;
                  float sparkThickness = 0.05F * sparkScale;
                  drawQuad(matrix, buffer, 0.0F, 0.0F, sparkLength, sparkThickness, sr, sg, sb, sparkAlpha);
                  drawQuad(matrix, buffer, 0.0F, 0.0F, sparkThickness, sparkLength, sr, sg, sb, sparkAlpha);
               }

               matrices.popPose();
               spark.age++;
               if (spark.age > spark.maxAge) {
                  sparkIt.remove();
               }
            }

            tessellator.end();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
         }
      }
   }

   private static void drawQuad(Matrix4f matrix, BufferBuilder buffer, float cx, float cy, float width, float height, int r, int g, int b, int alpha) {
      float halfW = width * 0.5F;
      float halfH = height * 0.5F;
      buffer.vertex(matrix, cx - halfW, cy - halfH, 0.0F).color(r, g, b, alpha).endVertex();
      buffer.vertex(matrix, cx + halfW, cy - halfH, 0.0F).color(r, g, b, alpha).endVertex();
      buffer.vertex(matrix, cx + halfW, cy + halfH, 0.0F).color(r, g, b, alpha).endVertex();
      buffer.vertex(matrix, cx - halfW, cy + halfH, 0.0F).color(r, g, b, alpha).endVertex();
   }

   private static class BarrageSpark {
      Vec3 pos;
      int age;
      int maxAge = 2;
      float rotation;

      public BarrageSpark(Vec3 pos) {
         this.pos = pos;
         this.age = 0;
         this.rotation = (float)(Math.random() * 360.0);
      }
   }
}
