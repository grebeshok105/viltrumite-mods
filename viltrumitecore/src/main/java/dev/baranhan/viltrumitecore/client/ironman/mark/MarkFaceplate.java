package dev.baranhan.viltrumitecore.client.ironman.mark;

import dev.baranhan.viltrumitecore.client.anim.geo.GeoBone;
import dev.baranhan.viltrumitecore.client.anim.render.PlayerGeoLayer;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkId;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

/**
 * The iron faceplate of a mark helmet (spec §10): a rigid plate over the head
 * front (and hat front) textured from the mark skin. Opening, it glides up over
 * the forehead along the head (a 90° turn about the head centre, lifted a
 * little off the surface mid-way so the helmet edge never pokes through) and
 * rests on the crown; closing runs the same path back. No squashing. Its eyes
 * go dark while it is up. The face under it comes from {@link MarkSkins}
 * (only the face region is Tony's). Not drawn when fully closed: then the skin
 * carries the plate.
 */
public final class MarkFaceplate {
   public static final ResourceLocation GEO = new ResourceLocation("viltrumitecore", "geo/ironman/marks/faceplate.geo.json");
   /** Extra lift (px) off the head surface in the middle of the turn. */
   private static final float LIFT = 1.9F;
   /** Below this closure the plate's glow (eyes) is off. */
   private static final float GLOW_FROM = 0.2F;

   private MarkFaceplate() {
   }

   /** Plate part for a helmet closure 0 (open, on the crown) .. 1 (closed), or null when fully closed. */
   @Nullable
   public static PlayerGeoLayer.Part part(MarkId mark, float closed) {
      if (closed >= 0.999F) {
         return null;
      }

      float t = ease(1.0F - closed);
      float angle = (float)(Math.PI * 0.5 * t);
      float lift = LIFT * (float)Math.sin(Math.PI * t);
      List<PlayerGeoLayer.Pass> passes = closed > GLOW_FROM
         ? List.of(PlayerGeoLayer.Pass.cutout(MarkTextures.skin(mark)), PlayerGeoLayer.Pass.glow(MarkTextures.glow(mark)))
         : List.of(PlayerGeoLayer.Pass.cutout(MarkTextures.skin(mark)));
      return new PlayerGeoLayer.Part(GEO, passes, geo -> {
         GeoBone plate = geo.getBone("faceplate");
         if (plate != null) {
            // Pivot = head centre: positive rotX carries the front face up onto the top face.
            plate.rotX = plate.initRotX + angle;
            plate.posY = lift * (float)Math.sin(angle);
            plate.posZ = -lift * (float)Math.cos(angle);
         }
      });
   }

   private static float ease(float x) {
      x = Math.max(0.0F, Math.min(1.0F, x));
      return x * x * (3.0F - 2.0F * x);
   }
}
