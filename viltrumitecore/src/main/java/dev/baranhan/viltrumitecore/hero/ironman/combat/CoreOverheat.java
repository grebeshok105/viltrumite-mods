package dev.baranhan.viltrumitecore.hero.ironman.combat;

import net.minecraft.nbt.CompoundTag;

/**
 * Unibeam overheat counter (spec §9.1). Every beam that ends in an overheat
 * adds 1. The third beam is the overdraft. Only a core explosion or death
 * resets the counter; a suit change, relog or dimension change keep it
 * (NBT key {@code CoreOverheats}).
 */
public final class CoreOverheat {
   public static final String KEY = "CoreOverheats";
   public static final int MAX = 3;
   private int count;

   public int count() {
      return this.count;
   }

   /** Spec §9.2: the beam that starts now is the third overheat (overdraft on this same beam). */
   public boolean nextIsOverdraft() {
      return this.count >= MAX - 1;
   }

   /** A beam ended in an overheat. */
   public void add() {
      this.count = Math.min(MAX, this.count + 1);
   }

   /** JARVIS warning after the second overheat (text now, voice in Stage 3). */
   public boolean warn() {
      return this.count == MAX - 1;
   }

   public void resetByExplosion() {
      this.count = 0;
   }

   public void resetByDeath() {
      this.count = 0;
   }

   public void save(CompoundTag tag) {
      tag.putInt(KEY, this.count);
   }

   public void load(CompoundTag tag) {
      this.count = Math.max(0, Math.min(MAX, tag.getInt(KEY)));
   }
}
