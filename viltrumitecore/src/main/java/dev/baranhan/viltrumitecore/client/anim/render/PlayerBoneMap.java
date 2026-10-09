package dev.baranhan.viltrumitecore.client.anim.render;

import javax.annotation.Nullable;

/**
 * Maps geo armor bones (Blockbench player-armor naming) onto vanilla player
 * model parts and converts the part pose into geo bone offsets (render space
 * is the model space flipped by scale(-1,-1,1), GeckoLib armor convention).
 * Child bones follow their parent; unknown top bones are skipped.
 */
public final class PlayerBoneMap {
   public enum Part {
      HEAD(0.0F, 0.0F),
      BODY(0.0F, 0.0F),
      RIGHT_ARM(5.0F, 2.0F),
      LEFT_ARM(-5.0F, 2.0F),
      RIGHT_LEG(1.9F, 12.0F),
      LEFT_LEG(-1.9F, 12.0F);

      /** Vanilla default pivot, negated on x, so a default pose gives a zero offset. */
      final float dx;
      final float dy;

      Part(float dx, float dy) {
         this.dx = dx;
         this.dy = dy;
      }
   }

   private PlayerBoneMap() {
   }

   @Nullable
   public static Part of(@Nullable String boneName) {
      if (boneName == null) {
         return null;
      }

      return switch (boneName) {
         case "armorHead" -> Part.HEAD;
         case "armorBody" -> Part.BODY;
         case "armorRightArm" -> Part.RIGHT_ARM;
         case "armorLeftArm" -> Part.LEFT_ARM;
         case "armorRightLeg" -> Part.RIGHT_LEG;
         case "armorLeftLeg" -> Part.LEFT_LEG;
         default -> null;
      };
   }

   /** Geo bone pos (pixels) from the model part position (pixels). */
   public static float[] position(Part part, float x, float y, float z) {
      return new float[]{x + part.dx, part.dy - y, z};
   }

   /** Geo bone rotation (radians) from the model part rotation. */
   public static float[] rotation(float xRot, float yRot, float zRot) {
      return new float[]{-xRot, -yRot, zRot};
   }
}
