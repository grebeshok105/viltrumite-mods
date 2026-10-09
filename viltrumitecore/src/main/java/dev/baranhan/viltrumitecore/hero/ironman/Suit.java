package dev.baranhan.viltrumitecore.hero.ironman;

import dev.baranhan.viltrumitecore.hero.ironman.mark.EquipTimeline;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkId;
import dev.baranhan.viltrumitecore.hero.ironman.mark.SuitPart;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;

/**
 * What Tony wears (spec §4): the nano Mark 50 put on / taken off as a ~1 s
 * wave (§4.2, §4.5) and, from Stage 4, a Veronica mark put on by parts
 * (§12.4), left through the exit (§12.5) or kept partial after a control
 * interrupt. Ownership of the mark itself lives in the MarkRoster.
 */
public final class Suit {
   private static final String KEY = "Suit";
   private static final String NANO_LOCK_KEY = "NanoLock";
   private static final String MARK_KEY = "SuitMark";
   private static final String PARTS_KEY = "SuitParts";
   /** Exit: plates open, Tony steps out, the suit closes (spec §12.5). */
   public static final int EXIT_TICKS = 30;
   /** Fast exit before another mark arrives (spec §12.4 step 1). */
   public static final int SWAP_EXIT_TICKS = 15;
   private SuitState state = SuitState.NONE;
   private int ticks;
   /** After a core explosion the nano cannot be put on (spec §9.4). */
   private int nanoLockTicks;
   /** Mark on Tony (MARK, EQUIPPING, MARK_PARTIAL) or leaving him (EXITING). */
   @Nullable
   private MarkId mark;
   /** Mark parts on the body (bit = SuitPart index). */
   private int parts;
   /** Parts already on the body when this equip started (re-choose of a partial mark). */
   private int startParts;
   /** Equip with flying parts (Veronica) or entering an empty suit. */
   private boolean delivery;
   private int exitLength = EXIT_TICKS;
   /** Visual-only nano wave after a mark broke: the nano is on at once (no fall). */
   private int autoNanoTicks;

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

   /** "Костюм" key for the nano: NONE → DEPLOYING, NANO → RETRACTING. Ignored mid-wave, in a mark and while the nano is locked. */
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

   /** What one tick of the suit finished (the caller moves the mark in the roster). */
   public enum Event {
      NONE,
      MARK_ON,
      EXITED
   }

   public Event tick() {
      if (this.nanoLockTicks > 0) {
         this.nanoLockTicks--;
      }

      if (this.autoNanoTicks > 0) {
         this.autoNanoTicks--;
      }

      switch (this.state) {
         case DEPLOYING -> {
            if (++this.ticks >= IronManRules.SUIT_DEPLOY_TICKS) {
               this.set(SuitState.NANO);
            }
         }
         case RETRACTING -> {
            if (++this.ticks >= IronManRules.SUIT_RETRACT_TICKS) {
               this.set(SuitState.NONE);
            }
         }
         case EQUIPPING -> {
            this.ticks++;
            int count = SuitPart.of(this.mark).size();
            this.parts = EquipTimeline.lockedMask(count, this.delivery, this.ticks, this.startParts);
            if (this.ticks >= EquipTimeline.length(this.delivery)) {
               this.parts = SuitPart.fullMask(this.mark);
               this.state = SuitState.MARK;
               this.ticks = 0;
               return Event.MARK_ON;
            }
         }
         case EXITING -> {
            if (++this.ticks >= this.exitLength) {
               this.set(SuitState.NONE);
               return Event.EXITED;
            }
         }
         default -> {
         }
      }

      return Event.NONE;
   }

   /**
    * Control interrupts an active wave or equip (spec §16): the nano wave falls
    * back to NONE; an equip keeps the locked parts as MARK_PARTIAL (none locked
    * → NONE). Returns the mark that left the body completely, or null.
    */
   @Nullable
   public MarkId interrupt() {
      if (this.transitioning()) {
         this.set(SuitState.NONE);
      } else if (this.state == SuitState.EQUIPPING) {
         if (this.parts == 0) {
            MarkId gone = this.mark;
            this.set(SuitState.NONE);
            return gone;
         }

         this.state = SuitState.MARK_PARTIAL;
         this.ticks = 0;
      }

      return null;
   }

   /** Remove everything at once (death, hero change). */
   public void clear() {
      this.set(SuitState.NONE);
      this.autoNanoTicks = 0;
   }

   /** Finish an active wave at its target state (disconnect, save). */
   public void resolve() {
      SuitState target = target(this.state);
      if (target == SuitState.NONE) {
         this.set(SuitState.NONE);
      } else if (target != this.state) {
         this.state = target;
         this.ticks = 0;
      }
   }

   // ---- Stage 4: marks ----

   /**
    * Start putting on a mark from NONE (the caller retracted the nano and sent
    * the old mark out first). {@code present} = parts already on the body
    * (re-choose of a partial mark sends only the missing ones).
    */
   public boolean startEquip(MarkId id, boolean delivery, int present) {
      if (this.state != SuitState.NONE && this.state != SuitState.MARK_PARTIAL) {
         return false;
      }

      this.state = SuitState.EQUIPPING;
      this.ticks = 0;
      this.mark = id;
      this.delivery = delivery;
      this.startParts = present & SuitPart.fullMask(id);
      this.parts = this.startParts;
      this.autoNanoTicks = 0;
      return true;
   }

   /** "Костюм" in a mark: plates open and Tony steps out (spec §12.5). */
   public boolean startExit(int length) {
      if (this.state != SuitState.MARK) {
         return false;
      }

      this.state = SuitState.EXITING;
      this.ticks = 0;
      this.exitLength = Math.max(1, length);
      this.parts = 0;
      return true;
   }

   /** Mark broke (spec §4.3) or nano retract for a new mark: instant state change, no wave. */
   public void dropMark() {
      this.set(SuitState.NONE);
   }

   /** Mark broke: the nano is on at once (no fall), its wave plays as a visual only. */
   public void autoNano() {
      this.set(SuitState.NANO);
      this.autoNanoTicks = IronManRules.SUIT_DEPLOY_TICKS;
   }

   /** Nano off at once (a mark is coming, spec §12.4 step 1). */
   public void retractNanoInstantly() {
      if (this.state == SuitState.NANO || this.state == SuitState.DEPLOYING || this.state == SuitState.RETRACTING) {
         this.set(SuitState.NONE);
      }
   }

   @Nullable
   public MarkId mark() {
      return this.mark;
   }

   public int parts() {
      return this.parts;
   }

   /** Mark 42 lost parts are no longer on the body. */
   public void removeParts(int lostMask) {
      if (this.state == SuitState.MARK) {
         this.parts = SuitPart.fullMask(this.mark) & ~lostMask;
      }
   }

   public boolean delivery() {
      return this.delivery;
   }

   public int exitLength() {
      return this.exitLength;
   }

   public int autoNanoTicks() {
      return this.autoNanoTicks;
   }

   /** A mark is fully on. */
   public boolean markWorn() {
      return this.state == SuitState.MARK;
   }

   /** Mark durability absorbs damage: fully on or partial (spec §4.3, plan Task 7). */
   public boolean markOn() {
      return this.state == SuitState.MARK || this.state == SuitState.MARK_PARTIAL;
   }

   public boolean equipping() {
      return this.state == SuitState.EQUIPPING;
   }

   public boolean exiting() {
      return this.state == SuitState.EXITING;
   }

   public boolean partial() {
      return this.state == SuitState.MARK_PARTIAL;
   }

   /** The nano is the suit on Tony (arsenal, nano shield visuals, nano damage). */
   public boolean nano() {
      return this.state == SuitState.NANO;
   }

   public SuitState state() {
      return this.state;
   }

   public int ticks() {
      return this.ticks;
   }

   public int waveLength() {
      return switch (this.state) {
         case RETRACTING -> IronManRules.SUIT_RETRACT_TICKS;
         case EQUIPPING -> EquipTimeline.length(this.delivery);
         case EXITING -> this.exitLength;
         default -> IronManRules.SUIT_DEPLOY_TICKS;
      };
   }

   /** Wave progress 0..1; 1 when worn, 0 without a suit. */
   public float progress() {
      return switch (this.state) {
         case NONE -> 0.0F;
         case NANO, MARK, MARK_PARTIAL -> 1.0F;
         default -> Math.min(1.0F, this.ticks / (float)this.waveLength());
      };
   }

   /** The nano wave runs (deploy or retract). */
   public boolean transitioning() {
      return this.state == SuitState.DEPLOYING || this.state == SuitState.RETRACTING;
   }

   /** Any timeline that blocks the suit key and abilities: nano wave, mark equip or exit. */
   public boolean busy() {
      return this.transitioning() || this.state == SuitState.EQUIPPING || this.state == SuitState.EXITING;
   }

   /** Fully worn (nano or mark): flight, armor-dependent abilities. */
   public boolean worn() {
      return this.state == SuitState.NANO || this.state == SuitState.MARK;
   }

   /** Armor attributes and no fall damage: worn, partial or the nano wave going back. */
   public boolean armored() {
      return this.state == SuitState.NANO || this.state == SuitState.RETRACTING || this.state == SuitState.MARK || this.state == SuitState.MARK_PARTIAL;
   }

   public void save(CompoundTag tag) {
      SuitState saved = target(this.state);
      tag.putString(KEY, saved.name());
      tag.putInt(NANO_LOCK_KEY, this.nanoLockTicks);
      if ((saved == SuitState.MARK || saved == SuitState.MARK_PARTIAL) && this.mark != null) {
         tag.putString(MARK_KEY, this.mark.key());
         tag.putInt(PARTS_KEY, saved == SuitState.MARK ? SuitPart.fullMask(this.mark) : this.parts);
      }
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
      if (loaded == SuitState.MARK || loaded == SuitState.MARK_PARTIAL) {
         MarkId id = MarkId.byKey(tag.getString(MARK_KEY));
         int mask = id == null ? 0 : tag.getInt(PARTS_KEY) & SuitPart.fullMask(id);
         if (id == null || mask == 0) {
            this.set(SuitState.NONE);
         } else {
            this.state = loaded;
            this.mark = id;
            this.parts = loaded == SuitState.MARK ? SuitPart.fullMask(id) : mask;
         }
      }
   }

   /** Saved form of a state: the nano wave finishes, a running equip/exit leaves Tony without the mark. */
   private static SuitState target(SuitState state) {
      return switch (state) {
         case DEPLOYING -> SuitState.NANO;
         case RETRACTING, EQUIPPING, EXITING -> SuitState.NONE;
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
      if (next == SuitState.NONE || next == SuitState.NANO || next == SuitState.DEPLOYING || next == SuitState.RETRACTING) {
         this.mark = null;
         this.parts = 0;
         this.startParts = 0;
      }
   }
}
