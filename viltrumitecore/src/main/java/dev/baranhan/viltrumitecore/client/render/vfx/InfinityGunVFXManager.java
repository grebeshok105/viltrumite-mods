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
import dev.baranhan.viltrumitecore.item.InfinityGunItem;
import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraftforge.client.event.RenderLivingEvent.Pre;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@EventBusSubscriber(
   modid = "viltrumitecore",
   bus = Bus.FORGE,
   value = {Dist.CLIENT}
)
public class InfinityGunVFXManager {
   @SubscribeEvent
   public static void onRenderLevel(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_LEVEL) {
         Minecraft client = Minecraft.getInstance();
         if (client.level != null) {
            for (Player player : client.level.players()) {
               ItemStack mainHand = player.getMainHandItem();
               if (mainHand.getItem() instanceof InfinityGunItem && InfinityGunItem.isFiring(mainHand) && player instanceof ViltrumiteCorePlayer) {
                  ViltrumiteCorePlayer corePlayer = (ViltrumiteCorePlayer)player;
                  float tickDelta = event.getPartialTick();
                  Vec3 handPos = corePlayer.getCalculatedHandPos();
                  float handYaw = corePlayer.getCalculatedHandYaw();
                  float handPitch = corePlayer.getCalculatedHandPitch();
                  if (player == client.player && client.options.getCameraType().isFirstPerson()) {
                     Vec3 localPos = corePlayer.getFirstPersonLocalHandPos();
                     if (localPos != null) {
                        Camera camera = event.getCamera();
                        Vector3f f = camera.getLookVector();
                        Vector3f u = camera.getUpVector();
                        Vector3f l = camera.getLeftVector();
                        Vec3 forward = new Vec3((double)f.x(), (double)f.y(), (double)f.z());
                        Vec3 up = new Vec3((double)u.x(), (double)u.y(), (double)u.z());
                        Vec3 left = new Vec3((double)l.x(), (double)l.y(), (double)l.z());
                        handPos = camera.getPosition()
                           .add(left.scale(-localPos.x))
                           .add(up.scale(localPos.y))
                           .add(forward.scale(-localPos.z));
                        handYaw = player.getViewYRot(tickDelta);
                        handPitch = player.getViewXRot(tickDelta);
                     }
                  }

                  if (handPos != null) {
                     int gunTimer = mainHand.getOrCreateTag().getInt("GunTimer");
                     if (gunTimer > 0 && gunTimer <= 14) {
                        float exactTicks = (float)gunTimer - tickDelta;
                        Camera camera = event.getCamera();
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
                        if (gunTimer > 10) {
                           float ringElapsed = 14.0F - exactTicks;
                           float ringProgress = Mth.clamp(ringElapsed / 4.0F, 0.0F, 1.0F);
                           PoseStack ringStack = new PoseStack();
                           ringStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
                           ringStack.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));
                           ringStack.translate(
                              handPos.x - camera.getPosition().x,
                              handPos.y - camera.getPosition().y,
                              handPos.z - camera.getPosition().z
                           );
                           ringStack.mulPose(Axis.YP.rotationDegrees(-handYaw));
                           ringStack.mulPose(Axis.XP.rotationDegrees(handPitch));
                           Matrix4f ringMatrix = ringStack.last().pose();
                           float radius = Mth.lerp(ringProgress, 0.0F, 1.2F);
                           float thicknessGrid = Mth.lerp(ringProgress, 4.0F, 0.0F);
                           float pixelWorldSize = 0.08F;
                           if (radius > 0.01F && thicknessGrid > 0.01F) {
                              float outerR = radius / pixelWorldSize;
                              float innerR = outerR - thicknessGrid;
                              if (innerR < 0.0F) {
                                 innerR = 0.0F;
                              }

                              int loopRadius = (int)outerR + 1;
                              float midR = (outerR + innerR) / 2.0F;
                              float safeHalfThickness = Math.max(thicknessGrid / 2.0F, 0.5F);
                              int alpha = 255;

                              for (int x = 0; x <= loopRadius; x++) {
                                 for (int y = 0; y <= loopRadius; y++) {
                                    float dist = (float)Math.sqrt((double)(x * x + y * y));
                                    if (dist <= outerR && dist >= innerR) {
                                       float distFromMid = Math.abs(dist - midR) / safeHalfThickness;
                                       distFromMid = Mth.clamp(distFromMid, 0.0F, 1.0F);
                                       int r = (int)Mth.lerp(distFromMid, 255.0F, 50.0F);
                                       int g = (int)Mth.lerp(distFromMid, 255.0F, 200.0F);
                                       int b = 255;
                                       drawPaintPixel(ringMatrix, buffer, x, y, pixelWorldSize, r, g, b, alpha);
                                       if (x != 0) {
                                          drawPaintPixel(ringMatrix, buffer, -x, y, pixelWorldSize, r, g, b, alpha);
                                       }

                                       if (y != 0) {
                                          drawPaintPixel(ringMatrix, buffer, x, -y, pixelWorldSize, r, g, b, alpha);
                                       }

                                       if (x != 0 && y != 0) {
                                          drawPaintPixel(ringMatrix, buffer, -x, -y, pixelWorldSize, r, g, b, alpha);
                                       }
                                    }
                                 }
                              }
                           }
                        }

                        PoseStack laserStack = new PoseStack();
                        laserStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
                        laserStack.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));
                        laserStack.translate(-camera.getPosition().x, -camera.getPosition().y, -camera.getPosition().z);
                        Vec3 lookDir = player.getViewVector(tickDelta);
                        HitResult hit = player.pick(128.0, tickDelta, false);
                        Vec3 endPoint = hit.getType() != Type.MISS ? hit.getLocation() : handPos.add(lookDir.scale(128.0));
                        float elapsed = 14.0F - exactTicks;
                        float laserFade = Mth.clamp(1.0F - elapsed / 5.0F, 0.0F, 1.0F);
                        float time = (float)player.tickCount + tickDelta;
                        float pulse = (float)Math.sin((double)(time * 0.9F)) * 0.02F;
                        float coreThickness = Math.max(0.0F, (0.09F + pulse) * laserFade);
                        float innerThickness = Math.max(0.0F, (0.18F - pulse * 0.5F) * laserFade);
                        float outerThickness = Math.max(0.0F, (0.36F + pulse) * laserFade);
                        if (laserFade > 0.01F) {
                           drawContinuousLaserBeam(laserStack, buffer, handPos, endPoint, lookDir, 0.0F, 0.3F, 1.0F, laserFade * 0.4F, outerThickness);
                           drawContinuousLaserBeam(laserStack, buffer, handPos, endPoint, lookDir, 0.2F, 0.8F, 1.0F, laserFade * 0.7F, innerThickness);
                           drawContinuousLaserBeam(
                              laserStack, buffer, handPos, endPoint, lookDir, 1.0F, 1.0F, 1.0F, Math.min(1.0F, laserFade * 1.5F), coreThickness
                           );
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

   private static void drawPaintPixel(Matrix4f matrix, BufferBuilder buffer, int gridX, int gridY, float pixelScale, int r, int g, int b, int alpha) {
      float cx = (float)gridX * pixelScale;
      float cy = (float)gridY * pixelScale;
      float half = pixelScale * 0.5F;
      buffer.vertex(matrix, cx - half, cy - half, 0.0F).color(r, g, b, alpha).endVertex();
      buffer.vertex(matrix, cx + half, cy - half, 0.0F).color(r, g, b, alpha).endVertex();
      buffer.vertex(matrix, cx + half, cy + half, 0.0F).color(r, g, b, alpha).endVertex();
      buffer.vertex(matrix, cx - half, cy + half, 0.0F).color(r, g, b, alpha).endVertex();
   }

   private static void drawContinuousLaserBeam(
      PoseStack poseStack, BufferBuilder buffer, Vec3 start, Vec3 end, Vec3 lookDir, float r, float g, float b, float alpha, float thickness
   ) {
      poseStack.pushPose();
      poseStack.translate(start.x, start.y, start.z);
      float yaw = (float)Math.atan2(lookDir.x, lookDir.z);
      float pitch = (float)(-Math.asin(lookDir.y));
      poseStack.mulPose(new Quaternionf().rotateY(yaw));
      poseStack.mulPose(new Quaternionf().rotateX(pitch));
      double length = start.distanceTo(end);
      Matrix4f posMatrix = poseStack.last().pose();
      float t = thickness;
      float zStart = 0.0F;
      float zEnd = (float)length;
      int planeCount = 8;

      for (int i = 0; i < planeCount; i++) {
         float angle = (float)((double)i * Math.PI / (double)planeCount);
         float dx = (float)Math.cos((double)angle) * t;
         float dy = (float)Math.sin((double)angle) * t;
         buffer.vertex(posMatrix, -dx, -dy, zStart).color(r, g, b, alpha).endVertex();
         buffer.vertex(posMatrix, -dx, -dy, zEnd).color(r, g, b, alpha).endVertex();
         buffer.vertex(posMatrix, dx, dy, zEnd).color(r, g, b, alpha).endVertex();
         buffer.vertex(posMatrix, dx, dy, zStart).color(r, g, b, alpha).endVertex();
      }

      poseStack.popPose();
   }

   @SubscribeEvent
   public static void onRenderLivingPre(Pre<?, ?> event) {
      LivingEntity entity = event.getEntity();
      ItemStack mainHand = entity.getMainHandItem();
      if (mainHand.getItem() instanceof InfinityGunItem && InfinityGunItem.isFiring(mainHand)) {
         entity.yBodyRot = entity.yHeadRot;
         entity.yBodyRotO = entity.yHeadRotO;
      }
   }
}
