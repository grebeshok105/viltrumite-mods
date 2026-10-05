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
import java.util.Random;
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
   bus = Bus.FORGE,
   value = {Dist.CLIENT}
)
public class ChopBloodVFXManager {
   private static final List<ChopBloodVFXManager.HitEffect> ACTIVE_HITS = new ArrayList<>();

   public static void addHit(Vec3 hitPos, Vec3 chopDir) {
      ACTIVE_HITS.add(new ChopBloodVFXManager.HitEffect(hitPos, chopDir, true));
   }

   public static void addBleed(Vec3 bleedPos) {
      ACTIVE_HITS.add(new ChopBloodVFXManager.HitEffect(bleedPos, Vec3.ZERO, false));
   }

   @SubscribeEvent
   public static void onRenderLevel(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_PARTICLES) {
         if (!ACTIVE_HITS.isEmpty()) {
            Camera camera = event.getCamera();
            PoseStack modelViewStack = RenderSystem.getModelViewStack();
            modelViewStack.pushPose();
            modelViewStack.setIdentity();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.disableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder buffer = tesselator.getBuilder();
            buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            Iterator<ChopBloodVFXManager.HitEffect> it = ACTIVE_HITS.iterator();

            while (it.hasNext()) {
               ChopBloodVFXManager.HitEffect effect = it.next();
               if (effect.isInitialHit && effect.age <= 4) {
                  float sparkScale = 1.0F - (float)effect.age / 4.0F;
                  int sparkAlpha = (int)(255.0F * sparkScale);
                  PoseStack matrices = new PoseStack();
                  matrices.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
                  matrices.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));
                  matrices.translate(
                     effect.hitPos.x - camera.getPosition().x,
                     effect.hitPos.y - camera.getPosition().y,
                     effect.hitPos.z - camera.getPosition().z
                  );
                  matrices.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
                  matrices.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
                  matrices.mulPose(Axis.ZP.rotationDegrees((float)(effect.hitPos.hashCode() % 360)));
                  Matrix4f matrix = matrices.last().pose();
                  drawQuad(matrix, buffer, 0.0F, 0.0F, 1.5F * sparkScale, 0.15F * sparkScale, 255, 255, 255, sparkAlpha);
                  drawQuad(matrix, buffer, 0.0F, 0.0F, 0.15F * sparkScale, 1.5F * sparkScale, 255, 255, 255, sparkAlpha);
               }

               for (ChopBloodVFXManager.BloodPixel b : effect.bloods) {
                  b.pos = b.pos.add(b.vel);
                  if (effect.isInitialHit) {
                     b.vel = new Vec3(b.vel.x * 0.82, b.vel.y - 0.08, b.vel.z * 0.82);
                  } else {
                     b.vel = new Vec3(b.vel.x * 0.5, b.vel.y * 0.85 - 0.005, b.vel.z * 0.5);
                  }

                  double snapX = Math.floor(b.pos.x * 16.0) / 16.0;
                  double snapY = Math.floor(b.pos.y * 16.0) / 16.0;
                  double snapZ = Math.floor(b.pos.z * 16.0) / 16.0;
                  PoseStack matrices = new PoseStack();
                  matrices.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
                  matrices.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));
                  matrices.translate(snapX - camera.getPosition().x, snapY - camera.getPosition().y, snapZ - camera.getPosition().z);
                  matrices.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
                  matrices.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
                  Matrix4f matrix = matrices.last().pose();
                  int r = 160;
                  int g = 0;
                  int b_col = 0;
                  if (b.type == 1) {
                     r = 100;
                  } else if (b.type == 2) {
                     r = 200;
                     g = 10;
                     b_col = 10;
                  } else if (b.type == 3) {
                     r = 60;
                     g = 0;
                     b_col = 0;
                  }

                  int bloodAlpha = (int)(255.0F * (1.0F - (float)effect.age / (float)effect.maxAge));
                  if (bloodAlpha < 0) {
                     bloodAlpha = 0;
                  }

                  float pixelSize = (b.type == 0 ? 3.0F : 2.0F) / 16.0F;
                  drawQuad(matrix, buffer, 0.0F, 0.0F, pixelSize, pixelSize, r, g, b_col, bloodAlpha);
               }

               effect.age++;
               if (effect.age > effect.maxAge) {
                  it.remove();
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

   private static void drawQuad(Matrix4f matrix, BufferBuilder buffer, float cx, float cy, float width, float height, int r, int g, int b, int alpha) {
      float halfW = width * 0.5F;
      float halfH = height * 0.5F;
      buffer.vertex(matrix, cx - halfW, cy - halfH, 0.0F).color(r, g, b, alpha).endVertex();
      buffer.vertex(matrix, cx + halfW, cy - halfH, 0.0F).color(r, g, b, alpha).endVertex();
      buffer.vertex(matrix, cx + halfW, cy + halfH, 0.0F).color(r, g, b, alpha).endVertex();
      buffer.vertex(matrix, cx - halfW, cy + halfH, 0.0F).color(r, g, b, alpha).endVertex();
   }

   private static class BloodPixel {
      Vec3 pos;
      Vec3 vel;
      int type;

      public BloodPixel(Vec3 pos, Vec3 vel, int type) {
         this.pos = pos;
         this.vel = vel;
         this.type = type;
      }
   }

   private static class HitEffect {
      Vec3 hitPos;
      int age;
      int maxAge;
      boolean isInitialHit;
      List<ChopBloodVFXManager.BloodPixel> bloods = new ArrayList<>();

      public HitEffect(Vec3 hitPos, Vec3 chopDir, boolean isInitialHit) {
         this.hitPos = hitPos;
         this.age = 0;
         this.isInitialHit = isInitialHit;
         Random random = new Random();
         if (isInitialHit) {
            this.maxAge = 20;

            for (int i = 0; i < 60; i++) {
               double vx = chopDir.x * 0.7 + (random.nextDouble() - 0.5) * 1.5;
               double vy = (random.nextDouble() - 0.1) * 0.8 + 0.3;
               double vz = chopDir.z * 0.7 + (random.nextDouble() - 0.5) * 1.5;
               this.bloods.add(new ChopBloodVFXManager.BloodPixel(hitPos, new Vec3(vx, vy, vz), random.nextInt(4)));
            }
         } else {
            this.maxAge = 80;

            for (int i = 0; i < 25; i++) {
               double vx = (random.nextDouble() - 0.5) * 0.15;
               double vy = (random.nextDouble() - 0.5) * 0.1;
               double vz = (random.nextDouble() - 0.5) * 0.15;
               this.bloods.add(new ChopBloodVFXManager.BloodPixel(hitPos, new Vec3(vx, vy, vz), random.nextInt(4)));
            }
         }
      }
   }
}
