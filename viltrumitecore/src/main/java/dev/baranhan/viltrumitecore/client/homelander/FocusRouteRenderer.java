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
            Vec3 from = client.player.getPosition(partialTick);
            Vec3 to = target.getPosition(partialTick);
            List<Vec3> path = route != null && route.size() >= 2 ? groundPath(from, route, to) : arcPath(from, to);
            drawStream(buffer, cameraPos, camera, RouteCurve.resample(RouteCurve.smooth(path, 3), 0.2), time, id * 1.618F, r, g, b);
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
      return new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
   }

   /** A* block route with the real end points instead of block centres. */
   private static List<Vec3> groundPath(Vec3 from, List<BlockPos> route, Vec3 to) {
      List<Vec3> path = new java.util.ArrayList<>(route.size() + 2);
      path.add(from);
      for (int i = 1; i < route.size() - 1; i++) {
         path.add(point(route.get(i)));
      }

      path.add(to);
      return path;
   }

   /** No walkable route: a soft arc over the obstacles. */
   private static List<Vec3> arcPath(Vec3 from, Vec3 to) {
      double dist = from.distanceTo(to);
      Vec3 mid = from.lerp(to, 0.5).add(0.0, Math.min(6.0, dist * 0.2), 0.0);
      List<Vec3> path = new java.util.ArrayList<>();
      int steps = Math.max(4, (int)(dist / 2.0));
      for (int i = 0; i <= steps; i++) {
         double t = i / (double)steps;
         path.add(from.lerp(mid, t).lerp(mid.lerp(to, t), t));
      }

      return path;
   }

   /**
    * Glowing smoky stream along the smoothed path: soft-edged additive ribbon
    * (wide haze, body, bright core) floating above the ground, swaying and
    * breathing procedurally, with pulses running toward the target.
    */
   private static void drawStream(BufferBuilder buffer, Vec3 cameraPos, Camera camera, List<Vec3> samples, float time, float seed, int r, int g, int b) {
      int n = samples.size();
      if (n < 2) {
         return;
      }

      double[] along = new double[n];
      for (int i = 1; i < n; i++) {
         along[i] = along[i - 1] + samples.get(i).distanceTo(samples.get(i - 1));
      }

      double length = along[n - 1];
      Vec3[] pts = new Vec3[n];
      float[] fade = new float[n];
      for (int i = 0; i < n; i++) {
         double s = along[i];
         Vec3 p = samples.get(i);
         Vec3 next = samples.get(Math.min(n - 1, i + 1));
         Vec3 prev = samples.get(Math.max(0, i - 1));
         Vec3 dir = next.subtract(prev);
         Vec3 side = new Vec3(-dir.z, 0.0, dir.x);
         side = side.lengthSqr() < 1.0E-6 ? Vec3.ZERO : side.normalize();
         double envelope = Math.min(1.0, Math.min(s / 2.0, (length - s) / 1.5));
         double sway = 0.35 * Math.sin(s * 0.55 + time * 1.6 + seed) + 0.12 * Math.sin(s * 1.7 - time * 2.9 + seed * 2.0);
         double lift = 0.55 + 0.25 * Math.sin(s * 0.4 - time * 1.2 + seed) + 0.08 * Math.sin(s * 2.3 + time * 3.1);
         pts[i] = p.add(side.scale(sway * envelope)).add(0.0, Math.max(0.15, lift * Math.max(0.35, envelope)), 0.0);
         float head = (float)Mth.clamp(s / 1.5, 0.0, 1.0);
         float pulse = 0.7F + 0.3F * Mth.sin((float)(s * 0.9 - time * 7.0));
         fade[i] = head * pulse * (1.0F - 0.35F * (float)(s / Math.max(1.0, length)));
      }

      int wr = (r + 255 * 2) / 3;
      int wg = (g + 255 * 2) / 3;
      int wb = (b + 255 * 2) / 3;
      for (int i = 0; i < n - 1; i++) {
         double s = along[i];
         float breathe = 0.8F + 0.3F * Mth.sin((float)(s * 0.8 - time * 3.0 + seed));
         ribbon(buffer, cameraPos, pts[i], pts[i + 1], 0.55F * breathe, r, g, b, (int)(55 * fade[i]), (int)(55 * fade[i + 1]));
         ribbon(buffer, cameraPos, pts[i], pts[i + 1], 0.22F * breathe, r, g, b, (int)(120 * fade[i]), (int)(120 * fade[i + 1]));
         ribbon(buffer, cameraPos, pts[i], pts[i + 1], 0.06F, wr, wg, wb, (int)(200 * fade[i]), (int)(200 * fade[i + 1]));
      }

      // Wisps drifting toward the target.
      float offset = (time * 5.0F + seed) % 3.0F;
      int k = 0;
      for (double s = offset; s < length; s += 3.0) {
         while (k < n - 2 && along[k + 1] < s) {
            k++;
         }

         double t = (s - along[k]) / Math.max(1.0E-6, along[k + 1] - along[k]);
         Vec3 at = pts[k].lerp(pts[k + 1], Mth.clamp(t, 0.0, 1.0)).add(0.0, 0.1 * Math.sin(s + time * 4.0), 0.0);
         PixelVfx.crossGlow(buffer, cameraPos, camera, at, 0.22F, r, g, b, (int)(120 * fade[k]));
      }
   }

   /** Camera-facing strip with full alpha on the centre line and zero at both edges. */
   private static void ribbon(BufferBuilder buffer, Vec3 cameraPos, Vec3 a, Vec3 c, float halfWidth, int r, int g, int b, int alphaA, int alphaC) {
      if (alphaA <= 0 && alphaC <= 0) {
         return;
      }

      Vec3 dir = c.subtract(a);
      Vec3 toCamera = cameraPos.subtract(a.lerp(c, 0.5));
      Vec3 side = dir.cross(toCamera);
      if (side.lengthSqr() < 1.0E-8) {
         return;
      }

      side = side.normalize().scale(halfWidth);
      Vec3 ra = a.subtract(cameraPos);
      Vec3 rc = c.subtract(cameraPos);
      for (int sign = -1; sign <= 1; sign += 2) {
         Vec3 off = side.scale(sign);
         vertex(buffer, ra.add(off), r, g, b, 0);
         vertex(buffer, ra, r, g, b, alphaA);
         vertex(buffer, rc, r, g, b, alphaC);
         vertex(buffer, rc.add(off), r, g, b, 0);
      }
   }

   private static void vertex(BufferBuilder buffer, Vec3 p, int r, int g, int b, int a) {
      buffer.vertex(p.x, p.y, p.z).color(r, g, b, Mth.clamp(a, 0, 255)).endVertex();
   }
}
