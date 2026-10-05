package dev.baranhan.viltrumiteflight.client;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Axis;
import dev.baranhan.viltrumiteflight.util.ViltrumiteFlightPlayer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
   modid = "viltrumiteflight",
   bus = Bus.FORGE,
   value = {Dist.CLIENT}
)
public class MachDiskManager {
   private static final List<MachDiskManager.MachDisk> ACTIVE_DISKS = new ArrayList<>();
   private static final Map<UUID, Boolean> WAS_SONIC_MAP = new HashMap<>();

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent event) {
      if (event.phase == Phase.END) {
         Minecraft client = Minecraft.getInstance();
         if (client.level == null) {
            ACTIVE_DISKS.clear();
            WAS_SONIC_MAP.clear();
         } else {
            for (Player player : client.level.players()) {
               if (player instanceof ViltrumiteFlightPlayer flightPlayer) {
                  boolean isSonic = flightPlayer.getFlightThrottle() >= 0.6F;
                  boolean wasSonic = WAS_SONIC_MAP.getOrDefault(player.getUUID(), false);
                  if (isSonic && !wasSonic) {
                     Vec3 look = player.getLookAngle();
                     Vec3 center = player.position().add(0.0, (double)player.getBbHeight() * 0.5, 0.0);

                     for (int i = 0; i < 3; i++) {
                        Vec3 diskPos = center.subtract(look.scale((double)i * 4.0));
                        ACTIVE_DISKS.add(
                           new MachDiskManager.MachDisk(diskPos, player.getYRot(), player.getXRot(), System.currentTimeMillis() + (long)(i * 100))
                        );
                     }
                  }

                  WAS_SONIC_MAP.put(player.getUUID(), isSonic);
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onWorldRender(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_LEVEL) {
         if (!ACTIVE_DISKS.isEmpty()) {
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
            Iterator<MachDiskManager.MachDisk> it = ACTIVE_DISKS.iterator();
            boolean drawing = false;

            while (it.hasNext()) {
               MachDiskManager.MachDisk disk = it.next();
               long age = now - disk.spawnTime;
               if (age >= 0L) {
                  if (age > 1200L) {
                     it.remove();
                  } else {
                     if (!drawing) {
                        buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                        drawing = true;
                     }

                     float progress = (float)age / 1200.0F;
                     float radius = Mth.lerp(progress, 1.5F, 25.0F);
                     int alpha = (int)(255.0F * Math.max(0.0F, 1.0F - progress));
                     PoseStack matrices = new PoseStack();
                     matrices.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
                     matrices.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));
                     matrices.translate(
                        disk.pos.x - camera.getPosition().x,
                        disk.pos.y - camera.getPosition().y,
                        disk.pos.z - camera.getPosition().z
                     );
                     matrices.mulPose(Axis.YP.rotationDegrees(-disk.yaw));
                     matrices.mulPose(Axis.XP.rotationDegrees(disk.pitch));
                     Matrix4f matrix = matrices.last().pose();
                     int r = 220;
                     int g = 240;
                     int b = 255;
                     float fPR = Mth.lerp(progress, 12.0F, 64.0F);
                     if (fPR < 1.0F) {
                        fPR = 1.0F;
                     }

                     float pixelWorldSize = radius / fPR;
                     float fCW = 1.5F;
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

   private static class MachDisk {
      public final Vec3 pos;
      public final float yaw;
      public final float pitch;
      public final long spawnTime;

      public MachDisk(Vec3 pos, float yaw, float pitch, long spawnTime) {
         this.pos = pos;
         this.yaw = yaw;
         this.pitch = pitch;
         this.spawnTime = spawnTime;
      }
   }
}
