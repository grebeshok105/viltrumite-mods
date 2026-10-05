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
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.joml.Matrix4f;

@EventBusSubscriber(
   modid = "viltrumitecore",
   bus = Bus.FORGE,
   value = {Dist.CLIENT}
)
public class DashVFXManager {
   private static final List<DashVFXManager.SpeedLine> ACTIVE_LINES = new ArrayList<>();

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent event) {
      if (event.phase == Phase.END && event.player.level().isClientSide()) {
         if (event.player instanceof ViltrumiteCorePlayer corePlayer && corePlayer.isDashing()) {
            Vec3 deltaPos = new Vec3(
               event.player.getX() - event.player.xo,
               event.player.getY() - event.player.yo,
               event.player.getZ() - event.player.zo
            );
            if (deltaPos.lengthSqr() > 0.05) {
               Vec3 moveInv = deltaPos.normalize().scale(-1.0);

               for (int i = 0; i < 5; i++) {
                  Vec3 offsetPos = event.player
                     .position()
                     .add((Math.random() - 0.5) * 3.5, (double)event.player.getBbHeight() * (Math.random() * 1.5 - 0.2), (Math.random() - 0.5) * 3.5);
                  ACTIVE_LINES.add(new DashVFXManager.SpeedLine(offsetPos, moveInv));
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onRenderLevel(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_LEVEL) {
         if (!ACTIVE_LINES.isEmpty()) {
            Camera camera = event.getCamera();
            long now = System.currentTimeMillis();
            PoseStack modelViewStack = RenderSystem.getModelViewStack();
            modelViewStack.pushPose();
            modelViewStack.setIdentity();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder buffer = tesselator.getBuilder();
            Iterator<DashVFXManager.SpeedLine> lineIt = ACTIVE_LINES.iterator();
            boolean drawing = false;

            while (lineIt.hasNext()) {
               DashVFXManager.SpeedLine line = lineIt.next();
               long age = now - line.spawnTime;
               if (age > 200L) {
                  lineIt.remove();
               } else {
                  if (!drawing) {
                     buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                     drawing = true;
                  }

                  float progress = (float)age / 200.0F;
                  int alpha = (int)(180.0F * (1.0F - progress));
                  PoseStack matrices = new PoseStack();
                  matrices.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
                  matrices.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));
                  matrices.translate(
                     line.pos.x - camera.getPosition().x,
                     line.pos.y - camera.getPosition().y,
                     line.pos.z - camera.getPosition().z
                  );
                  float yaw = (float)Math.toDegrees(Math.atan2(line.direction.z, line.direction.x)) - 90.0F;
                  float pitch = (float)(-Math.toDegrees(Math.asin(line.direction.y)));
                  matrices.mulPose(Axis.YP.rotationDegrees(-yaw));
                  matrices.mulPose(Axis.XP.rotationDegrees(pitch));
                  Matrix4f matrix = matrices.last().pose();
                  buffer.vertex(matrix, -0.05F, 0.0F, 0.0F).color(255, 255, 255, alpha).endVertex();
                  buffer.vertex(matrix, 0.05F, 0.0F, 0.0F).color(255, 255, 255, alpha).endVertex();
                  buffer.vertex(matrix, 0.05F, 0.0F, line.length).color(255, 255, 255, 0).endVertex();
                  buffer.vertex(matrix, -0.05F, 0.0F, line.length).color(255, 255, 255, 0).endVertex();
               }
            }

            if (drawing) {
               tesselator.end();
            }

            RenderSystem.enableDepthTest();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            modelViewStack.popPose();
            RenderSystem.applyModelViewMatrix();
         }
      }
   }

   private static class SpeedLine {
      public final Vec3 pos;
      public final Vec3 direction;
      public final long spawnTime;
      public final float length;

      public SpeedLine(Vec3 pos, Vec3 direction) {
         this.pos = pos;
         this.direction = direction;
         this.spawnTime = System.currentTimeMillis();
         this.length = (float)(3.0 + Math.random() * 6.0);
      }
   }
}
