package dev.baranhan.viltrumitecore.client.ironman.mark;

import dev.baranhan.viltrumitecore.client.anim.geo.GeoBone;
import dev.baranhan.viltrumitecore.client.anim.render.PlayerGeoLayer;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkId;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

/**
 * The iron faceplate of a mark helmet (spec §10): a plate over the head front
 * (and hat front) textured from the mark skin. While the helmet opens it slides
 * up into the crown (shrinks towards its top edge, tilting a little outward),
 * and slides back down when it closes. The face under it comes from
 * {@link MarkSkins} (only the face region is Tony's). Drawn only while the
 * helmet is not fully closed: closed, the skin carries the plate.
 */
public final class MarkFaceplate {
   public static final ResourceLocation GEO = new ResourceLocation("viltrumitecore", "geo/ironman/marks/faceplate.geo.json");
   /** How far (px) the plate rises above its closed place at full open. */
   private static final float RISE = 1.5F;
   /** Outward tilt (degrees) in the middle of the motion. */
   private static final float TILT = 14.0F;

   private MarkFaceplate() {
   }

   /** Plate part for a helmet closure 0 (open) .. 1 (closed), or null when closed / fully open. */
   @Nullable
   public static PlayerGeoLayer.Part part(MarkId mark, float closed) {
      if (closed >= 0.999F || closed <= 0.001F) {
         return null;
      }

      float open = ease(1.0F - closed);
      List<PlayerGeoLayer.Pass> passes = List.of(PlayerGeoLayer.Pass.cutout(MarkTextures.skin(mark)), PlayerGeoLayer.Pass.glow(MarkTextures.glow(mark)));
      return new PlayerGeoLayer.Part(GEO, passes, geo -> {
         GeoBone plate = geo.getBone("faceplate");
         if (plate != null) {
            plate.scaleY = Math.max(0.001F, 1.0F - open);
            plate.posY = RISE * open;
            // Positive rotX about the top-front hinge swings the lower edge forward (outward, never into the head).
            plate.rotX = plate.initRotX + (float)Math.toRadians(TILT * Math.sin(Math.PI * open));
         }
      });
   }

   private static float ease(float x) {
      x = Math.max(0.0F, Math.min(1.0F, x));
      return x * x * (3.0F - 2.0F * x);
   }
}
