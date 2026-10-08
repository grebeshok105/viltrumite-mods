package dev.baranhan.viltrumitecore.hero.homelander;

/** Passive regeneration timer (spec §4): +1 HP every REGEN_INTERVAL t, paused REGEN_DAMAGE_PAUSE t after damage. */
public final class Regen {
   private int timer;
   private int pause;

   /** @return true when the player heals 1 HP this tick */
   public boolean tick() {
      if (this.pause > 0) {
         this.pause--;
         return false;
      }

      if (++this.timer >= HomelanderRules.REGEN_INTERVAL) {
         this.timer = 0;
         return true;
      }

      return false;
   }

   public void onDamaged() {
      this.pause = HomelanderRules.REGEN_DAMAGE_PAUSE;
      this.timer = 0;
   }
}
