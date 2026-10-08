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
 * Walkable route from the focusing player to each target (spec §6.4), drawn as
 * a soft drifting gas trail in the target colour, seen through walls. Routes
 * are recomputed rarely (only after a long time or a big move), simplified and
 * smoothed so they do not follow the block grid, and a new route cross-fades
 * over the old one. The live ends follow the player and the target.
 */
@EventBusSubscriber(
   modid = "viltrumitecore",
   bus = Bus.FORGE,
   value = {Dist.CLIENT}
)
public final class FocusRouteRenderer {
   private static final int MAX_NODES = 4000;
   private static final int GROUND_SCAN = 64;
   /** A route is never recomputed sooner than this. */
   private static final int MIN_AGE = 80;
   /** ...and only if an end moved this far, unless it is older than MAX_AGE. */
   private static final double MOVE_TRIGGER = 6.0;
   private static final int MAX_AGE = 300;
   private static final float FADE_TICKS = 30.0F;
   private static final Map<Integer, Route> ROUTES = new HashMap<>();
   private static int cursor;

   /** Smoothed middle of a route; ends are attached live while drawing. */
   private record Route(List<Vec3> middle, Vec3 from, Vec3 to, long born, @Nullable Route previous) {
   }

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

      long now = level.getGameTime();
      // Look at one target per tick; most ticks nothing is recomputed.
      int id = targets.get(Math.floorMod(cursor++, targets.size()));
      Entity target = level.getEntity(id);
      if (target == null) {
         ROUTES.remove(id);
         return;
      }

      Route old = ROUTES.get(id);
      Vec3 from = client.player.position();
      Vec3 to = target.position();
      if (old != null) {
         long age = now - old.born();
         boolean moved = old.from().distanceTo(from) > MOVE_TRIGGER || old.to().distanceTo(to) > MOVE_TRIGGER;
         if (age < MIN_AGE || !moved && age < MAX_AGE) {
            return;
         }
      }

      BlockPos start = ground(level, client.player.blockPosition());
      BlockPos end = ground(level, target.blockPosition());
      List<BlockPos> blocks = start == null || end == null ? List.of() : GroundRoute.find(start, end, (x, y, z) -> standable(level, x, y, z), MAX_NODES);
      List<Vec3> middle = new java.util.ArrayList<>();
      for (BlockPos pos : blocks) {
         middle.add(new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
      }

      middle = RouteCurve.simplify(middle, 1.2);
      ROUTES.put(id, new Route(middle, from, to, now, old == null ? null : new Route(old.middle(), old.from(), old.to(), old.born(), null)));
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
      float now = level.getGameTime() + partialTick;
      PoseStack modelViewStack = RenderSystem.getModelViewStack();
      modelViewStack.pushPose();
      modelViewStack.setIdentity();
      PixelVfx.rotateCamera(modelViewStack, camera.getXRot(), camera.getYRot());
      RenderSystem.applyModelViewMatrix();
      RenderSystem.enableBlend();
      // Plain alpha blending: a soft gas, not an additive neon glow.
      RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
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

            // Soften the palette colour toward a pale grey so it reads as gas.
            int r = ((color >> 16 & 255) * 4 + 160) / 5;
            int g = ((color >> 8 & 255) * 4 + 160) / 5;
            int b = ((color & 255) * 4 + 160) / 5;
            Vec3 from = client.player.getPosition(partialTick);
            Vec3 to = target.getPosition(partialTick);
            Route route = ROUTES.get(id);
            float seed = id * 1.618F;
            if (route == null) {
               drawGas(buffer, cameraPos, path(from, List.of(), to), time, seed, r, g, b, 1.0F);
               continue;
            }

            float fadeIn = route.previous() == null ? 1.0F : Mth.clamp((now - route.born()) / FADE_TICKS, 0.0F, 1.0F);
            if (fadeIn < 1.0F) {
               drawGas(buffer, cameraPos, path(from, route.previous().middle(), to), time, seed, r, g, b, 1.0F - fadeIn);
            }

            drawGas(buffer, cameraPos, path(from, route.middle(), to), time, seed, r, g, b, fadeIn);
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

   /**
    * Live ends + the stored middle (trimmed to the parts still ahead of the
    * ends), smoothed and evenly resampled. No middle = a soft low arc.
    */
   private static List<Vec3> path(Vec3 from, List<Vec3> middle, Vec3 to) {
      List<Vec3> points = new java.util.ArrayList<>();
      points.add(from);
      if (middle.size() >= 2) {
         int first = nearest(middle, from);
         int last = nearest(middle, to);
         if (first <= last) {
            for (int i = first + 1; i < last; i++) {
               points.add(middle.get(i));
            }
         } else {
            for (int i = first - 1; i > last; i--) {
               points.add(middle.get(i));
            }
         }
      } else {
         double dist = from.distanceTo(to);
         points.add(from.lerp(to, 0.5).add(0.0, Math.min(4.0, dist * 0.15), 0.0));
      }

      points.add(to);
      return RouteCurve.resample(RouteCurve.smooth(points, 4), 0.25);
   }

   private static int nearest(List<Vec3> points, Vec3 at) {
      int best = 0;
      double bestDist = Double.MAX_VALUE;
      for (int i = 0; i < points.size(); i++) {
         double d = points.get(i).distanceToSqr(at);
         if (d < bestDist) {
            bestDist = d;
            best = i;
         }
      }

      return best;
   }

   /**
    * Gas trail: wide, soft-edged translucent ribbons plus slow drifting puffs.
    * Motion is slow and small so the trail breathes instead of jumping.
    */
   private static void drawGas(BufferBuilder buffer, Vec3 cameraPos, List<Vec3> samples, float time, float seed, int r, int g, int b, float strength) {
      int n = samples.size();
      if (n < 2 || strength <= 0.01F) {
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
         Vec3 dir = samples.get(Math.min(n - 1, i + 1)).subtract(samples.get(Math.max(0, i - 1)));
         Vec3 side = new Vec3(-dir.z, 0.0, dir.x);
         side = side.lengthSqr() < 1.0E-6 ? Vec3.ZERO : side.normalize();
         double envelope = Math.min(1.0, Math.min(s / 3.0, (length - s) / 2.0));
         double sway = 0.18 * Math.sin(s * 0.25 + time * 0.5 + seed);
         double lift = 0.5 + 0.1 * Math.sin(s * 0.2 - time * 0.4 + seed);
         pts[i] = samples.get(i).add(side.scale(sway * envelope)).add(0.0, lift * Math.max(0.5, envelope), 0.0);
         float head = (float)Mth.clamp(s / 2.0, 0.0, 1.0);
         float drift = 0.85F + 0.15F * Mth.sin((float)(s * 0.35 - time * 1.2 + seed));
         fade[i] = strength * head * drift * (1.0F - 0.3F * (float)(s / Math.max(1.0, length)));
      }

      for (int i = 0; i < n - 1; i++) {
         float breathe = 0.9F + 0.15F * Mth.sin((float)(along[i] * 0.3 - time * 0.8 + seed));
         ribbon(buffer, cameraPos, pts[i], pts[i + 1], 1.0F * breathe, r, g, b, (int)(40 * fade[i]), (int)(40 * fade[i + 1]));
         ribbon(buffer, cameraPos, pts[i], pts[i + 1], 0.5F * breathe, r, g, b, (int)(60 * fade[i]), (int)(60 * fade[i + 1]));
      }

      // Puffs drifting slowly toward the target, each swelling and thinning.
      float offset = (time * 1.0F + seed) % 0.9F;
      int k = 0;
      int index = 0;
      for (double s = offset; s < length; s += 0.9, index++) {
         while (k < n - 2 && along[k + 1] < s) {
            k++;
         }

         double t = Mth.clamp((s - along[k]) / Math.max(1.0E-6, along[k + 1] - along[k]), 0.0, 1.0);
         float phase = (float)(s * 0.7 + seed * 3.0);
         Vec3 at = pts[k].lerp(pts[k + 1], t).add(0.08 * Math.sin(phase + time * 0.7), 0.1 * Math.sin(phase * 1.3 + time * 0.5), 0.08 * Math.cos(phase + time * 0.6));
         float radius = 0.4F + 0.15F * Mth.sin(phase + time * 0.9F);
         int alpha = (int)(46 * fade[k] * (0.7F + 0.3F * Mth.sin(phase * 2.1F + time)));
         puff(buffer, cameraPos, at, radius, r, g, b, alpha);
      }
   }

   /** Camera-facing strip with full alpha on the centre line and zero at both edges. */
   private static void ribbon(BufferBuilder buffer, Vec3 cameraPos, Vec3 a, Vec3 c, float halfWidth, int r, int g, int b, int alphaA, int alphaC) {
      if (alphaA <= 0 && alphaC <= 0) {
         return;
      }

      Vec3 side = c.subtract(a).cross(cameraPos.subtract(a.lerp(c, 0.5)));
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

   /** Soft round camera-facing blob: opaque centre fading to a clear rim. */
   private static void puff(BufferBuilder buffer, Vec3 cameraPos, Vec3 at, float radius, int r, int g, int b, int alpha) {
      if (alpha <= 0) {
         return;
      }

      Vec3 toCamera = cameraPos.subtract(at);
      if (toCamera.lengthSqr() < 1.0E-6) {
         return;
      }

      Vec3 forward = toCamera.normalize();
      Vec3 u = Math.abs(forward.y) > 0.95 ? new Vec3(1, 0, 0) : forward.cross(new Vec3(0, 1, 0)).normalize();
      Vec3 v = forward.cross(u);
      Vec3 c = at.subtract(cameraPos);
      int segments = 10;
      for (int i = 0; i < segments; i++) {
         double a0 = Math.PI * 2.0 * i / segments;
         double a1 = Math.PI * 2.0 * (i + 1) / segments;
         Vec3 e0 = c.add(u.scale(Math.cos(a0) * radius)).add(v.scale(Math.sin(a0) * radius));
         Vec3 e1 = c.add(u.scale(Math.cos(a1) * radius)).add(v.scale(Math.sin(a1) * radius));
         vertex(buffer, c, r, g, b, alpha);
         vertex(buffer, c, r, g, b, alpha);
         vertex(buffer, e0, r, g, b, 0);
         vertex(buffer, e1, r, g, b, 0);
      }
   }

   private static void vertex(BufferBuilder buffer, Vec3 p, int r, int g, int b, int a) {
      buffer.vertex(p.x, p.y, p.z).color(r, g, b, Mth.clamp(a, 0, 255)).endVertex();
   }
}
