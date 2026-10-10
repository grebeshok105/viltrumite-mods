package dev.baranhan.viltrumitecore.client.anim.render;

import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Maps top-level bones of armor-style geo models (Blockbench / GeckoLib armor
 * names) to the six vanilla player model parts. Child bones follow their parent.
 * Pure logic: no client classes.
 */
public final class PlayerBoneMap {
   /** Vanilla player part with its standing pivot in model pixels (y down). */
   public enum Part {
      HEAD(0.0F, 0.0F, 0.0F),
      BODY(0.0F, 0.0F, 0.0F),
      RIGHT_ARM(-5.0F, 2.0F, 0.0F),
      LEFT_ARM(5.0F, 2.0F, 0.0F),
      RIGHT_LEG(-1.9F, 12.0F, 0.0F),
      LEFT_LEG(1.9F, 12.0F, 0.0F);

      public final float pivotX;
      public final float pivotY;
      public final float pivotZ;

      Part(float pivotX, float pivotY, float pivotZ) {
         this.pivotX = pivotX;
         this.pivotY = pivotY;
         this.pivotZ = pivotZ;
      }

      /** Arm parts receive first-person rendering. */
      public boolean isArm() {
         return this == RIGHT_ARM || this == LEFT_ARM;
      }
   }

   private static final Map<String, Part> NAMES = Map.ofEntries(
      Map.entry("armorhead", Part.HEAD),
      Map.entry("head", Part.HEAD),
      Map.entry("armorbody", Part.BODY),
      Map.entry("body", Part.BODY),
      Map.entry("armorrightarm", Part.RIGHT_ARM),
      Map.entry("rightarm", Part.RIGHT_ARM),
      Map.entry("armorleftarm", Part.LEFT_ARM),
      Map.entry("leftarm", Part.LEFT_ARM),
      Map.entry("armorrightleg", Part.RIGHT_LEG),
      Map.entry("rightleg", Part.RIGHT_LEG),
      Map.entry("armorleftleg", Part.LEFT_LEG),
      Map.entry("leftleg", Part.LEFT_LEG),
      // GeckoLib armor boots ride on the legs.
      Map.entry("armorrightboot", Part.RIGHT_LEG),
      Map.entry("armorleftboot", Part.LEFT_LEG)
   );

   private PlayerBoneMap() {
   }

   /** The player part for a top-level bone name, or null (bone skipped). */
   @Nullable
   public static Part of(String boneName) {
      return boneName == null ? null : NAMES.get(boneName.toLowerCase(Locale.ROOT));
   }
}
