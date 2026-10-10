package dev.baranhan.viltrumitecore.client.ironman.mark;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.baranhan.viltrumitecore.client.anim.geo.BakedGeoModel;
import dev.baranhan.viltrumitecore.client.anim.geo.GeoBone;
import dev.baranhan.viltrumitecore.client.anim.render.AnimRenderer;
import dev.baranhan.viltrumitecore.client.anim.render.PlayerGeoLayer;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkId;
import dev.baranhan.viltrumitecore.hero.ironman.mark.SuitPart;
import java.util.List;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Geometry of the suit parts that fly, wrap, stay on a partial suit or fall
 * as debris (spec §12.4). Satsu marks use the addon's {@code each_part}
 * pieces with the mark skin (tools/assets/convert_ironman_stage4_marks.py);
 * Mark 42 uses its own Sind pieces ({@link Mark42Parts}). Each piece has one
 * top-level armor bone pivoted at the piece centre, so it draws on the body
 * with {@link PlayerGeoLayer} and in the world around its centre.
 */
public final class MarkParts {
   private MarkParts() {
   }

   /** Satsu {@code each_part} piece name of a part: the 7-part arm takes the shoulder with it. */
   public static String piece(SuitPart part) {
      if (part.bone() == SuitPart.Bone.HEAD) {
         return "head";
      }

      boolean arm = part.bone() == SuitPart.Bone.LEFT_ARM || part.bone() == SuitPart.Bone.RIGHT_ARM;
      return arm && part.from() <= 0.0F && part.to() >= 1.0F ? part.name() + "_full" : part.name();
   }

   public static ResourceLocation geo(MarkId mark, SuitPart part) {
      if (mark == MarkId.MARK_42) {
         return Mark42Parts.geo(part);
      }

      return new ResourceLocation("viltrumitecore", "geo/ironman/marks/parts/" + piece(part) + ".geo.json");
   }

   public static List<PlayerGeoLayer.Pass> passes(MarkId mark, SuitPart part) {
      if (mark == MarkId.MARK_42) {
         return Mark42Parts.passes(part);
      }

      return List.of(PlayerGeoLayer.Pass.cutout(MarkTextures.skin(mark)), PlayerGeoLayer.Pass.glow(MarkTextures.glow(mark)));
   }

   /** Rest geometry centre in block units, parsed frame (the frame AnimRenderer draws in). */
   public static Vec3 centre(BakedGeoModel geo) {
      float minX = Float.MAX_VALUE;
      float minY = Float.MAX_VALUE;
      float minZ = Float.MAX_VALUE;
      float maxX = -Float.MAX_VALUE;
      float maxY = -Float.MAX_VALUE;
      float maxZ = -Float.MAX_VALUE;
      for (GeoBone bone : geo.allBones()) {
         for (GeoBone.Cube cube : bone.cubes) {
            for (GeoBone.Quad quad : cube.quads()) {
               for (GeoBone.Vertex v : quad.vertices()) {
                  minX = Math.min(minX, v.x());
                  minY = Math.min(minY, v.y());
                  minZ = Math.min(minZ, v.z());
                  maxX = Math.max(maxX, v.x());
                  maxY = Math.max(maxY, v.y());
                  maxZ = Math.max(maxZ, v.z());
               }
            }
         }
      }

      if (minX > maxX) {
         return Vec3.ZERO;
      }

      return new Vec3((minX + maxX) / 2.0, (minY + maxY) / 2.0, (minZ + maxZ) / 2.0);
   }

   /** Draws a posed piece in the current frame: cutout passes lit, glow passes full bright, all at {@code alpha}. */
   public static void draw(BakedGeoModel geo, PoseStack stack, MultiBufferSource buffers, List<PlayerGeoLayer.Pass> passes, int light, float alpha) {
      for (PlayerGeoLayer.Pass pass : passes) {
         boolean glow = pass.kind() == PlayerGeoLayer.Pass.Kind.GLOW;
         RenderType type = glow ? RenderType.eyes(pass.texture())
            : alpha < 1.0F ? RenderType.entityTranslucentCull(pass.texture()) : RenderType.entityCutoutNoCull(pass.texture());
         float k = glow ? alpha : 1.0F;
         AnimRenderer.render(geo, stack, null, buffers.getBuffer(type), glow ? LightTexture.FULL_BRIGHT : light, OverlayTexture.NO_OVERLAY,
            pass.r() * k, pass.g() * k, pass.b() * k, glow ? pass.a() : pass.a() * alpha, null);
      }
   }
}
