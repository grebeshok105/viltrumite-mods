package dev.baranhan.viltrumitecore.client.ironman.mark;

import dev.baranhan.viltrumitecore.client.anim.render.PlayerGeoLayer;
import dev.baranhan.viltrumitecore.hero.ironman.mark.SuitPart;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

/**
 * The 14 Mark 42 pieces (spec §3.1: Sind {@code tabula/mk42}, converted by
 * tools/assets/convert_ironman_sind.py). Files are named after
 * {@link SuitPart#FOURTEEN}; the abdomen is the Sind lower chest piece, the
 * helmet carries the faceplate. All pieces share the Sind 160 px atlas.
 */
public final class Mark42Parts {
   public static final ResourceLocation PIECES = new ResourceLocation("viltrumitecore", "textures/entity/ironman/mark_42/pieces.png");
   public static final ResourceLocation PIECES_GLOW = new ResourceLocation("viltrumitecore", "textures/entity/ironman/mark_42/pieces_glow.png");
   private static final List<PlayerGeoLayer.Pass> PASSES = List.of(PlayerGeoLayer.Pass.cutout(PIECES), PlayerGeoLayer.Pass.glow(PIECES_GLOW));

   private Mark42Parts() {
   }

   /** Geo of one Mark 42 suit part: armor space, one top-level armor bone. */
   public static ResourceLocation geo(SuitPart part) {
      return new ResourceLocation("viltrumitecore", "geo/ironman/mark_42/" + part.name() + ".geo.json");
   }

   /** Draw passes (cutout + glow) of that part. */
   public static List<PlayerGeoLayer.Pass> passes(SuitPart part) {
      return PASSES;
   }

   /** Thruster flame geo drawn with the part while flying, or null. */
   @Nullable
   public static ResourceLocation fire(SuitPart part) {
      return new ResourceLocation("viltrumitecore", "geo/ironman/mark_42/" + part.name() + "_fire.geo.json");
   }

   /** One frame of the Sind repulsor flame texture of the piece thrusters (8 frames). */
   public static ResourceLocation fireTexture(int frame) {
      return new ResourceLocation("viltrumitecore", "textures/entity/ironman/mark_42/fire_" + Math.floorMod(frame, 8) + ".png");
   }
}
