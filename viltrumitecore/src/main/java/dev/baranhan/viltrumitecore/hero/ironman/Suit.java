package dev.baranhan.viltrumitecore.hero.ironman;

import net.minecraft.nbt.CompoundTag;

/** Nano Mark 50 state machine: put on / take off as a ~1 s wave (spec §4.2, §4.5). */
public final class Suit {
   private static final String KEY = "Suit";
   private static final String NANO_LOCK_KEY = "NanoLock";
   private SuitState state = SuitState.NONE;
   private int ticks;
   /** After a core explosion the nano cannot be put on (spec §9.4). */
   private int nanoLockTicks;

   public int nanoLockTicks() {
      return this.nanoLockTicks;
   }

   public void setNanoLock(int ticks) {
      this.nanoLockTicks = Math.max(0, ticks);
   }

   public boolean nanoLocked() {
      return this.nanoLockTicks > 0;
   }

   /** Core explosion: the nanites scatter, the suit is gone at once. */
   public void scatter(int lockTicks) {
      this.set(SuitState.NONE);
      this.nanoLockTicks = Math.max(this.nanoLockTicks, lockTicks);
   }

   /** "Костюм" key: NONE → DEPLOYING, NANO → RETRACTING. Ignored mid-wave and while the nano is locked. */
   public boolean toggle() {
      switch (this.state) {
         case NONE -> {
            if (this.nanoLocked()) {
               return false;
            }

            this.start(SuitState.DEPLOYING);
         }
         case NANO -> this.start(SuitState.RETRACTING);
         default -> {
            return false;
         }
      }

      return true;
   }

   public void tick() {
      if (this.nanoLockTicks > 0) {
         this.nanoLockTicks--;
      }

      if (this.state == SuitState.DEPLOYING && ++this.ticks >= IronManRules.SUIT_DEPLOY_TICKS) {
         this.set(SuitState.NANO);
      } else if (this.state == SuitState.RETRACTING && ++this.ticks >= IronManRules.SUIT_RETRACT_TICKS) {
         this.set(SuitState.NONE);
      }
   }

   /** Control interrupts an active wave: the suit falls back to NONE. */
   public void interrupt() {
      if (this.transitioning()) {
         this.set(SuitState.NONE);
      }
   }

   /** Remove everything at once (death, hero change). */
   public void clear() {
      this.set(SuitState.NONE);
   }

   /** Finish an active wave at its target state (disconnect, save). */
   public void resolve() {
      this.set(target(this.state));
   }

   public SuitState state() {
      return this.state;
   }

   public int ticks() {
      return this.ticks;
   }

   public int waveLength() {
      return this.state == SuitState.RETRACTING ? IronManRules.SUIT_RETRACT_TICKS : IronManRules.SUIT_DEPLOY_TICKS;
   }

   /** Wave progress 0..1; 1 when worn, 0 without a suit. */
   public float progress() {
      return switch (this.state) {
         case NONE -> 0.0F;
         case NANO -> 1.0F;
         default -> Math.min(1.0F, this.ticks / (float)this.waveLength());
      };
   }

   public boolean transitioning() {
      return this.state == SuitState.DEPLOYING || this.state == SuitState.RETRACTING;
   }

   /** Fully worn: flight, armor-dependent abilities. */
   public boolean worn() {
      return this.state == SuitState.NANO;
   }

   /** Armor attributes and no fall damage: worn or the wave going back. */
   public boolean armored() {
      return this.state == SuitState.NANO || this.state == SuitState.RETRACTING;
   }

   public void save(CompoundTag tag) {
      tag.putString(KEY, target(this.state).name());
      tag.putInt(NANO_LOCK_KEY, this.nanoLockTicks);
   }

   public void load(CompoundTag tag) {
      SuitState loaded = SuitState.NONE;
      if (tag.contains(KEY, 8)) {
         try {
            loaded = target(SuitState.valueOf(tag.getString(KEY)));
         } catch (IllegalArgumentException ignored) {
            // Unknown state from a newer version: no suit.
         }
      }

      this.set(loaded);
      this.nanoLockTicks = Math.max(0, tag.getInt(NANO_LOCK_KEY));
   }

   private static SuitState target(SuitState state) {
      return switch (state) {
         case DEPLOYING -> SuitState.NANO;
         case RETRACTING -> SuitState.NONE;
         default -> state;
      };
   }

   private void start(SuitState next) {
      this.state = next;
      this.ticks = 0;
   }

   private void set(SuitState next) {
      this.state = next;
      this.ticks = 0;
   }
}
