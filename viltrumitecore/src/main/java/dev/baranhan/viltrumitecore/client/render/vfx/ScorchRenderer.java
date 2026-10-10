package dev.baranhan.viltrumitecore.client.render.vfx;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Client-only scorch decals on block faces (no blocks change). One instance per hero with its own
 * limits; pixels are exactly the original Homelander blotches. Render thread only.
 */
public final class ScorchRenderer {
   /** Scorch mark at the exact hit point (quantized to 1/16 block) on a face. */
   public record Scorch(int qx, int qy, int qz, Direction face) {
      /** Tangent axes snap to the 1/16 grid; the face axis keeps the hit plane (rounded to 1/1024). */
      public static Scorch at(Vec3 p, Direction face) {
         Direction.Axis axis = face.getAxis();
         return new Scorch(q(p.x, axis == Direction.Axis.X), q(p.y, axis == Direction.Axis.Y), q(p.z, axis == Direction.Axis.Z), face);
      }

      private static int q(double v, boolean plane) {
         return plane ? (int)Math.round(v * 1024.0) : Mth.floor(v * 16.0);
      }

      public Vec3 center() {
         Direction.Axis axis = this.face.getAxis();
         return new Vec3(c(this.qx, axis == Direction.Axis.X), c(this.qy, axis == Direction.Axis.Y), c(this.qz, axis == Direction.Axis.Z));
      }

      private static double c(int q, boolean plane) {
         return plane ? q / 1024.0 : (q + 0.5) / 16.0;
      }
   }

   /** Last scorch hit per shooter, to draw a continuous burn line while sweeping. */
   private record LastHit(Vec3 at, Direction face) {
   }

   private final Map<UUID, LastHit> lastHit = new HashMap<>();
   private final ScorchBuffer<Scorch> buffer;
   private final long lifetime;

   public ScorchRenderer(int max, long lifetime) {
      this.buffer = new ScorchBuffer<>(max, lifetime);
      this.lifetime = lifetime;
   }

   public void clear() {
      this.buffer.clear();
      this.lastHit.clear();
   }

   public int size() {
      return this.buffer.size();
   }

   /** Ages marks out; every 5 ticks drops marks whose block is gone (broken, burnt, moved). */
   public void tick(ClientLevel level, long now) {
      this.buffer.expire(now);
      if (now % 5L == 0L) {
         this.buffer.removeIf(scorch -> {
            Vec3 inside = scorch.center().subtract(Vec3.atLowerCornerOf(scorch.face().getNormal()).scale(0.02));
            BlockPos pos = BlockPos.containing(inside);
            return level.isLoaded(pos) && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
         });
      }
   }

   public void forget(UUID shooter) {
      this.lastHit.remove(shooter);
   }

   /** One mark, no line (single shots). */
   public void add(Vec3 at, Direction face, long now) {
      this.buffer.add(Scorch.at(at, face), now);
   }

   /** Burn from the previous hit to this one when both lie on the same flat surface. */
   public void addLine(UUID shooter, Vec3 at, Direction face, long now) {
      LastHit last = this.lastHit.put(shooter, new LastHit(at, face));
      if (last != null && last.face() == face) {
         Direction.Axis axis = face.getAxis();
         double gap = last.at().distanceTo(at);
         if (Math.abs(last.at().get(axis) - at.get(axis)) < 1.0E-3 && gap > 0.06 && gap < 4.0) {
            int steps = (int)Math.ceil(gap / 0.06);
            for (int i = 1; i < steps; i++) {
               this.buffer.add(Scorch.at(last.at().lerp(at, i / (double)steps), face), now);
            }
         }
      }

      this.buffer.add(Scorch.at(at, face), now);
   }

   /** Draws all marks; the caller has the camera-relative model view, shader and depth state set. */
   public void render(Tesselator tessellator, Vec3 cameraPos, long now, float partialTick) {
      if (this.buffer.size() == 0) {
         return;
      }

      RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
      BufferBuilder out = tessellator.getBuilder();
      out.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      for (Map.Entry<Scorch, Long> entry : this.buffer.entries()) {
         float age = (now - entry.getValue() + partialTick) / (float)this.lifetime;
         int alpha = (int)(170.0F * (1.0F - Mth.clamp((age - 0.7F) / 0.3F, 0.0F, 1.0F)));
         draw(out, cameraPos, entry.getKey(), alpha, now - entry.getValue() < 6L);
      }
      tessellator.end();
   }

   /** Dark blotch right on the hit point, slightly off the surface; a fresh mark glows hot first. */
   private static void draw(BufferBuilder buffer, Vec3 cameraPos, Scorch scorch, int alpha, boolean hot) {
      if (alpha <= 0) {
         return;
      }

      Direction face = scorch.face();
      long seed = (scorch.qx() * 73856093L) ^ (scorch.qy() * 19349663L) ^ (scorch.qz() * 83492791L) ^ face.ordinal();
      // Snap the mark onto the face plane (the hit point already lies on it), nudged out to avoid z-fighting.
      Vec3 c0 = scorch.center();
      double plane = c0.get(face.getAxis());
      Vec3 centre = switch (face.getAxis()) {
         case X -> new Vec3(plane + face.getStepX() * 0.003, c0.y, c0.z);
         case Y -> new Vec3(c0.x, plane + face.getStepY() * 0.003, c0.z);
         case Z -> new Vec3(c0.x, c0.y, plane + face.getStepZ() * 0.003);
      };
      Vec3 u = face.getAxis() == Direction.Axis.Y ? new Vec3(1, 0, 0) : new Vec3(-face.getStepZ(), 0, face.getStepX());
      Vec3 v = face.getAxis() == Direction.Axis.Y ? new Vec3(0, 0, 1) : new Vec3(0, 1, 0);
      // 3x3 pixel blotch (1/16 block pixels) with seeded ragged corners.
      for (int i = 0; i < 3; i++) {
         for (int j = 0; j < 3; j++) {
            boolean core = i == 1 && j == 1;
            boolean corner = i != 1 && j != 1;
            if (corner && ((seed >> (i * 3 + j)) & 1L) == 0L) {
               continue;
            }

            int r = hot && core ? 255 : core ? 18 : 40;
            int g = hot && core ? 110 : core ? 14 : 30;
            int b = hot && core ? 30 : core ? 12 : 24;
            Vec3 c = centre.add(u.scale((i - 1) * 0.0625)).add(v.scale((j - 1) * 0.0625)).subtract(cameraPos);
            Vec3 hu = u.scale(0.03125);
            Vec3 hv = v.scale(0.03125);
            int a = core ? alpha : alpha * 2 / 3;
            vertex(buffer, c.subtract(hu).subtract(hv), r, g, b, a);
            vertex(buffer, c.add(hu).subtract(hv), r, g, b, a);
            vertex(buffer, c.add(hu).add(hv), r, g, b, a);
            vertex(buffer, c.subtract(hu).add(hv), r, g, b, a);
         }
      }
   }

   private static void vertex(BufferBuilder buffer, Vec3 p, int r, int g, int b, int a) {
      buffer.vertex(p.x, p.y, p.z).color(r, g, b, a).endVertex();
   }
}
