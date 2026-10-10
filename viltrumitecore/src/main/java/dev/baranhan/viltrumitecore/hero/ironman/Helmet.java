package dev.baranhan.viltrumitecore.hero.ironman;

import net.minecraft.nbt.CompoundTag;

/**
 * Nano helmet (spec §10), pure. Closed by default; a toggle folds or
 * unfolds the mask over {@link IronManRules#HELMET_TOGGLE_TICKS}. The state
 * flips at once (gating is immediate), {@link #progress()} only drives the
 * visuals. A combat hit closes an open helmet. Putting the suit on closes it.
 */
public final class Helmet {
   private static final String KEY = "HelmetOpen";
   private boolean closed = true;
   private int ticks = IronManRules.HELMET_TOGGLE_TICKS;

   public boolean closed() {
      return this.closed;
   }

   /** 0 = fully open (face visible), 1 = fully closed. */
   public float progress() {
      float t = Math.min(1.0F, this.ticks / (float)IronManRules.HELMET_TOGGLE_TICKS);
      return this.closed ? t : 1.0F - t;
   }

   public boolean moving() {
      return this.ticks < IronManRules.HELMET_TOGGLE_TICKS;
   }

   public int ticks() {
      return this.ticks;
   }

   public void toggle() {
      this.closed = !this.closed;
      // Reversing mid-fold continues from the current position.
      this.ticks = this.moving() ? IronManRules.HELMET_TOGGLE_TICKS - this.ticks : 0;
   }

   public void tick() {
      if (this.ticks < IronManRules.HELMET_TOGGLE_TICKS) {
         this.ticks++;
      }
   }

   /** Combat hit (living attacker or projectile): closes an open helmet. True when it started closing. */
   public boolean onHit() {
      if (this.closed) {
         return false;
      }

      this.toggle();
      return true;
   }

   /** Suit put on: the helmet is closed at once (it reveals last with the wave). */
   public void reset() {
      this.closed = true;
      this.ticks = IronManRules.HELMET_TOGGLE_TICKS;
   }

   public void save(CompoundTag tag) {
      tag.putBoolean(KEY, !this.closed);
   }

   public void load(CompoundTag tag) {
      this.closed = !tag.getBoolean(KEY);
      this.ticks = IronManRules.HELMET_TOGGLE_TICKS;
   }

   /** Combat hit for the auto-close: a living attacker or a projectile, never fall / fire / drowning. */
   public static boolean combatHit(boolean livingAttacker, boolean projectile) {
      return livingAttacker || projectile;
   }
}
