package dev.baranhan.viltrumitecore.client.homelander;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import dev.baranhan.viltrumitecore.client.render.vfx.PixelVfx;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

/**
 * Walkable route from the focusing player to each target (spec §6.4): a thin
 * line in the target colour with soft sparks running along it, fading toward
 * the target, seen through walls. One target is re-routed per tick (round
 * robin) so the A* cost is spread; no route = a dim dashed arc.
 */
@EventBusSubscriber(
   modid = "viltrumitecore",
   bus = Bus.FORGE,
   value = {Dist.CLIENT}
)
public final class FocusRouteRenderer {
   private static final int MAX_NODES = 4000;
   private static final int GROUND_SCAN = 64;
   private static final Map<Integer, List<BlockPos>> ROUTES = new HashMap<>();
   private static int cursor;

   private FocusRouteRenderer() {
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent event) {
      if (event.phase != Phase.END) {
         return;
      }

      Minecraft client = Minecraft.getInstance();
      ClientLevel level = client.level;
      List<Integer> targets = FocusClient.targets();
      ROUTES.keySet().retainAll(targets);
      if (level == null || client.player == null || targets.isEmpty() || client.isPaused()) {
         return;
      }

      int id = targets.get(Math.floorMod(cursor++, targets.size()));
      Entity target = level.getEntity(id);
      if (target == null) {
         ROUTES.remove(id);
         return;
      }

      BlockPos from = ground(level, client.player.blockPosition());
      BlockPos to = ground(level, target.blockPosition());
      List<BlockPos> route = from == null || to == null ? List.of() : GroundRoute.find(from, to, (x, y, z) -> standable(level, x, y, z), MAX_NODES);
      ROUTES.put(id, route);
   }

   static boolean standable(ClientLevel level, int x, int y, int z) {
      BlockPos feet = new BlockPos(x, y, z);
      if (!level.isLoaded(feet)) {
         return false;
      }

      BlockPos below = feet.below();
      BlockState floor = level.getBlockState(below);
      return floor.isFaceSturdy(level, below, Direction.UP)
         && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
         && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty();
   }

   /** First standable position at or below pos (flying players/targets look down to the ground). */
   @Nullable
   static BlockPos ground(ClientLevel level, BlockPos pos) {
      for (int dy = 0; dy <= GROUND_SCAN; dy++) {
         int y = pos.getY() - dy;
         if (y < level.getMinBuildHeight()) {
            return null;
         }

         if (standable(level, pos.getX(), y, pos.getZ())) {
            return new BlockPos(pos.getX(), y, pos.getZ());
         }
      }

      return null;
   }

   @SubscribeEvent
   public static void onRenderLevel(RenderLevelStageEvent event) {
      if (event.getStage() != Stage.AFTER_LEVEL || FocusClient.targets().isEmpty()) {
         return;
      }

      Minecraft client = Minecraft.getInstance();
      ClientLevel level = client.level;
      if (level == null || client.player == null) {
         return;
      }

      float partialTick = event.getPartialTick();
      Camera camera = event.getCamera();
      Vec3 cameraPos = camera.getPosition();
      float time = (client.player.tickCount + partialTick) / 20.0F;
      PoseStack modelViewStack = RenderSystem.getModelViewStack();
      modelViewStack.pushPose();
      modelViewStack.setIdentity();
      PixelVfx.rotateCamera(modelViewStack, camera.getXRot(), camera.getYRot());
      RenderSystem.applyModelViewMatrix();
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
      RenderSystem.disableCull();
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      Tesselator tessellator = Tesselator.getInstance();
      BufferBuilder buffer = tessellator.getBuilder();
      try {
         buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
         for (int id : FocusClient.targets()) {
            Entity target = level.getEntity(id);
            int color = target == null ? -1 : FocusClient.colorOf(target);
            if (color < 0) {
               continue;
            }

            int r = color >> 16 & 255;
            int g = color >> 8 & 255;
            int b = color & 255;
            List<BlockPos> route = ROUTES.get(id);
            if (route != null && route.size() >= 2) {
               drawRoute(buffer, cameraPos, camera, route, time, r, g, b);
            } else {
               drawArc(buffer, cameraPos, camera, client.player.getPosition(partialTick), target.getPosition(partialTick), r, g, b);
            }
         }

         tessellator.end();
      } finally {
         RenderSystem.depthMask(true);
         RenderSystem.enableDepthTest();
         RenderSystem.enableCull();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableBlend();
         modelViewStack.popPose();
         RenderSystem.applyModelViewMatrix();
      }
   }

   private static Vec3 point(BlockPos pos) {
      return new Vec3(pos.getX() + 0.5, pos.getY() + 0.08, pos.getZ() + 0.5);
   }

   private static void drawRoute(BufferBuilder buffer, Vec3 cameraPos, Camera camera, List<BlockPos> route, float time, int r, int g, int b) {
      int segments = route.size() - 1;
      for (int i = 0; i < segments; i++) {
         float fade = 1.0F - 0.7F * i / (float)segments;
         PixelVfx.beamDots(buffer, cameraPos, camera, point(route.get(i)), point(route.get(i + 1)), 0.2F, 0.025F, r, g, b, (int)(150 * fade));
      }

      // Sparks every 4 blocks, running toward the target at 3 blocks/s.
      float offset = (time * 3.0F) % 4.0F;
      for (float s = offset; s < segments; s += 4.0F) {
         int i = (int)s;
         Vec3 at = point(route.get(i)).lerp(point(route.get(i + 1)), s - i).add(0.0, 0.05, 0.0);
         float fade = 1.0F - 0.7F * s / segments;
         PixelVfx.crossGlow(buffer, cameraPos, camera, at, 0.08F, r, g, b, (int)(200 * fade));
      }
   }

   private static void drawArc(BufferBuilder buffer, Vec3 cameraPos, Camera camera, Vec3 from, Vec3 to, int r, int g, int b) {
      double dist = from.distanceTo(to);
      Vec3 mid = from.lerp(to, 0.5).add(0.0, Math.min(8.0, dist * 0.25), 0.0);
      int steps = Math.max(8, (int)(dist * 4.0));
      for (int i = 0; i <= steps; i++) {
         if ((i / 3) % 2 == 1) {
            continue;
         }

         double t = i / (double)steps;
         Vec3 a = from.lerp(mid, t);
         Vec3 c = mid.lerp(to, t);
         PixelVfx.billboardPixel(buffer, cameraPos, camera, a.lerp(c, t), 0.025F, r, g, b, (int)(Mth.lerp((float)t, 90.0F, 40.0F)));
      }
   }
}
