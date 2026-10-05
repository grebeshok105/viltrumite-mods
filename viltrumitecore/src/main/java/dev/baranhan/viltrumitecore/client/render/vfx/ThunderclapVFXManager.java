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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.joml.Matrix4f;

@EventBusSubscriber(
   modid = "viltrumitecore",
   bus = Bus.FORGE,
   value = {Dist.CLIENT}
)
public class ThunderclapVFXManager {
   public static final List<ThunderclapVFXManager.ClapVFX> ACTIVE_CLAPS = new ArrayList<>();
   private static final Map<UUID, Integer> previousTicks = new HashMap<>();

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent event) {
      if (event.phase == Phase.END) {
         Minecraft client = Minecraft.getInstance();
         if (client.level != null) {
            for (Player player : client.level.players()) {
               if (player instanceof ViltrumiteCorePlayer) {
                  ViltrumiteCorePlayer corePlayer = (ViltrumiteCorePlayer)player;
                  int currentTicks = corePlayer.getThunderclapTicks();
                  int prev = previousTicks.getOrDefault(player.getUUID(), 0);
                  if (prev > 11 && currentTicks <= 11) {
                     Vec3 eyePos = new Vec3(player.getX(), player.getY() + (double)player.getEyeHeight() - 0.2, player.getZ());
                     ACTIVE_CLAPS.add(new ThunderclapVFXManager.ClapVFX(eyePos, player.getYRot(), player.getXRot()));
                  }

                  previousTicks.put(player.getUUID(), currentTicks);
               }
            }

            ACTIVE_CLAPS.removeIf(clap -> {
               clap.age++;
               return clap.age > 11;
            });
         }
      }
   }

   @SubscribeEvent
   public static void onRenderLevel(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_LEVEL) {
         if (!ACTIVE_CLAPS.isEmpty()) {
            float tickDelta = event.getPartialTick();
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
            Tesselator tessellator = Tesselator.getInstance();
            BufferBuilder buffer = tessellator.getBuilder();
            buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

            for (ThunderclapVFXManager.ClapVFX clap : ACTIVE_CLAPS) {
               float exactAge = (float)clap.age + tickDelta;
               if (exactAge > 11.0F) {
                  exactAge = 11.0F;
               }

               float progress = exactAge / 11.0F;
               float forwardOffset = Mth.lerp(progress, 0.5F, 30.0F);
               float easeOut = (float)Math.sin((double)progress * Math.PI / 2.0);
               float currentRadius = Mth.lerp(easeOut, 0.5F, 15.0F);
               float thickness = Mth.lerp(progress, 2.5F, 0.0F);
               int alpha = (int)(255.0 * (1.0 - Math.pow((double)progress, 1.5)));
               if (alpha < 0) {
                  alpha = 0;
               }

               if (alpha != 0 && !(thickness <= 0.01F)) {
                  PoseStack matrices = new PoseStack();
                  matrices.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
                  matrices.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));
                  matrices.translate(
                     clap.origin.x - camera.getPosition().x,
                     clap.origin.y - camera.getPosition().y,
                     clap.origin.z - camera.getPosition().z
                  );
                  matrices.mulPose(Axis.YP.rotationDegrees(-clap.yaw));
                  matrices.translate(0.0F, 0.0F, forwardOffset);
                  Matrix4f matrix = matrices.last().pose();
                  int r = 240;
                  int g = 250;
                  int b = 255;
                  float pixelWorldSize = 0.5F;
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

            tessellator.end();
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

   public static class ClapVFX {
      public Vec3 origin;
      public float yaw;
      public float pitch;
      public int age;

      public ClapVFX(Vec3 origin, float yaw, float pitch) {
         this.origin = origin;
         this.yaw = yaw;
         this.pitch = pitch;
         this.age = 0;
      }
   }
}
