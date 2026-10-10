package dev.baranhan.viltrumitecore.hero.ironman.combat;

import javax.annotation.Nullable;

/**
 * MMB tool cycle (spec §6.1, §8.4). Nano: repulsor ↔ formed weapon; with no
 * weapon formed the tool stays REPULSOR. A mark passes its signature (Stage
 * 4), the Hulkbuster its jackhammer (Stage 5) as {@code alternative}.
 */
public final class ToolCycle {
   private ToolCycle() {
   }

   public static RightTool next(RightTool current, @Nullable RightTool formedWeapon, @Nullable RightTool alternative) {
      if (current != RightTool.REPULSOR) {
         return RightTool.REPULSOR;
      }

      if (formedWeapon != null) {
         return formedWeapon;
      }

      return alternative != null ? alternative : RightTool.REPULSOR;
   }
}
