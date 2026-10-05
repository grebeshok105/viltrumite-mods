package dev.baranhan.viltrumitecore.client.render.vfx;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
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
public class MeltedTunnelRenderer {
   private static final Map<BlockPos, Long> MELTED_BLOCKS = new ConcurrentHashMap<>();
   private static final long MELT_LIFETIME_MS = 1000L;

   public static void addMeltedBlocks(List<BlockPos> blocks) {
      long now = System.currentTimeMillis();

      for (BlockPos pos : blocks) {
         MELTED_BLOCKS.put(pos, now);
      }
   }

   @SubscribeEvent
   public static void onRenderLevel(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_PARTICLES) {
         if (!MELTED_BLOCKS.isEmpty()) {
            long now = System.currentTimeMillis();
            MELTED_BLOCKS.entrySet().removeIf(entryx -> now - (Long)entryx.getValue() > 1000L);
            if (!MELTED_BLOCKS.isEmpty()) {
               PoseStack matrices = event.getPoseStack();
               Vec3 camPos = event.getCamera().getPosition();
               PoseStack modelViewStack = RenderSystem.getModelViewStack();
               modelViewStack.pushPose();
               modelViewStack.setIdentity();
               RenderSystem.applyModelViewMatrix();
               RenderSystem.enableBlend();
               RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
               RenderSystem.setShader(GameRenderer::getPositionColorShader);
               RenderSystem.disableCull();
               Tesselator tesselator = Tesselator.getInstance();
               BufferBuilder buffer = tesselator.getBuilder();
               buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

               for (Entry<BlockPos, Long> entry : MELTED_BLOCKS.entrySet()) {
                  BlockPos pos = entry.getKey();
                  long age = now - entry.getValue();
                  float life = 1.0F - (float)age / 1000.0F;
                  float r = 1.0F;
                  float g = life * 0.6F;
                  float b = life * 0.1F;
                  float a = life * 0.7F;
                  float expand = 0.01F;
                  float minX = (float)((double)pos.getX() - camPos.x) - expand;
                  float minY = (float)((double)pos.getY() - camPos.y) - expand;
                  float minZ = (float)((double)pos.getZ() - camPos.z) - expand;
                  float maxX = minX + 1.0F + expand * 2.0F;
                  float maxY = minY + 1.0F + expand * 2.0F;
                  float maxZ = minZ + 1.0F + expand * 2.0F;
                  drawBox(matrices, buffer, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, a);
               }

               tesselator.end();
               RenderSystem.enableCull();
               RenderSystem.defaultBlendFunc();
               RenderSystem.disableBlend();
               modelViewStack.popPose();
               RenderSystem.applyModelViewMatrix();
            }
         }
      }
   }

   private static void drawBox(
      PoseStack matrices, BufferBuilder buffer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float r, float g, float b, float a
   ) {
      Matrix4f matrix = matrices.last().pose();
      buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a).endVertex();
      buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a).endVertex();
      buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a).endVertex();
      buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a).endVertex();
      buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a).endVertex();
      buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a).endVertex();
      buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a).endVertex();
      buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a).endVertex();
      buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a).endVertex();
      buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a).endVertex();
      buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a).endVertex();
      buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a).endVertex();
      buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a).endVertex();
      buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a).endVertex();
      buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a).endVertex();
      buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a).endVertex();
      buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a).endVertex();
      buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a).endVertex();
      buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a).endVertex();
      buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a).endVertex();
      buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a).endVertex();
      buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a).endVertex();
      buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a).endVertex();
      buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a).endVertex();
   }
}
