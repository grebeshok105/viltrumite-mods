package dev.baranhan.viltrumitecore.hero.ironman.combat;

import dev.baranhan.viltrumitecore.hero.ironman.Energy;
import dev.baranhan.viltrumitecore.hero.ironman.IronManRules;
import javax.annotation.Nullable;

/**
 * Nano-arsenal (spec §8.4), pure. Slot press: none → blade, then blade ↔
 * hammer; every forming costs 8 energy, strikes are free. MMB dissolves the
 * weapon (the right tool goes back to the repulsor).
 */
public final class NanoArsenal {
   @Nullable
   private RightTool weapon;
   private int formTicks;
   /** Dissolve wave ticks left (visual only). */
   private int dissolveTicks;

   /** Slot press. Returns the weapon formed now, or null (refused). */
   @Nullable
   public RightTool press(Energy energy) {
      if (energy.weaponsLocked() || this.forming()) {
         return null;
      }

      RightTool next = this.weapon == RightTool.NANO_BLADE ? RightTool.NANO_HAMMER : RightTool.NANO_BLADE;
      if (!energy.spend(IronManRules.COST_NANO_WEAPON)) {
         return null;
      }

      this.weapon = next;
      this.formTicks = IronManRules.NANO_FORM_TICKS;
      this.dissolveTicks = 0;
      return next;
   }

   public void tick() {
      if (this.formTicks > 0) {
         this.formTicks--;
      }

      if (this.dissolveTicks > 0) {
         this.dissolveTicks--;
      }
   }

   /** MMB or suit off: the weapon flows back into the arm. */
   public boolean dissolve() {
      if (this.weapon == null) {
         return false;
      }

      this.weapon = null;
      this.formTicks = 0;
      this.dissolveTicks = IronManRules.NANO_FORM_TICKS;
      return true;
   }

   /** Death / hero change: gone at once. */
   public void clear() {
      this.weapon = null;
      this.formTicks = 0;
      this.dissolveTicks = 0;
   }

   @Nullable
   public RightTool weapon() {
      return this.weapon;
   }

   public boolean forming() {
      return this.formTicks > 0;
   }

   public int formTicks() {
      return this.formTicks;
   }

   public int dissolveTicks() {
      return this.dissolveTicks;
   }
}
