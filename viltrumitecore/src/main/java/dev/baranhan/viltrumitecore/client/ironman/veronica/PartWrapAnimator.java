package dev.baranhan.viltrumitecore.client.ironman.veronica;

import dev.baranhan.viltrumitecore.client.anim.geo.BakedGeoModel;
import dev.baranhan.viltrumitecore.client.anim.geo.GeoBone;

/**
 * Wrap of one suit part around its limb (spec §12.4 step 3): the plates open
 * wide, close around the limb, then click with a flash. Pure timing; the
 * plate geo scales about its own centre (scaleX/Z), so the plate opens around the limb.
 */
public final class PartWrapAnimator {
   /** Plate radius growth at the widest point (1.0 = limb fit). */
   public static final float OPEN = 0.6F;

   private PartWrapAnimator() {
   }

   /** Plate opening 0..1 over the wrap: opens to 1 at the middle, closes to 0 at the end. */
   public static float open(float progress) {
      float p = clamp(progress);
      return p < 0.5F ? smooth(p / 0.5F) : smooth((1.0F - p) / 0.5F);
   }

   /** Click flash 0..1 in the last part of the wrap. */
   public static float flash(float progress) {
      float p = clamp(progress);
      if (p < 0.85F) {
         return 0.0F;
      }

      return (float)Math.sin((p - 0.85F) / 0.15F * Math.PI);
   }

   public static float scale(float progress) {
      return 1.0F + OPEN * open(progress);
   }

   /** Applies the wrap pose to a plate model (its top-level bone scales about the centre). */
   public static void pose(BakedGeoModel geo, float progress) {
      float s = scale(progress);
      for (GeoBone bone : geo.topLevelBones()) {
         bone.scaleX = s;
         bone.scaleZ = s;
      }
   }

   private static float clamp(float v) {
      return Math.max(0.0F, Math.min(1.0F, v));
   }

   private static float smooth(float t) {
      float c = clamp(t);
      return c * c * (3.0F - 2.0F * c);
   }
}
