package dev.baranhan.viltrumitecore.hero.ironman.scan;

import dev.baranhan.viltrumitecore.hero.ironman.IronManRules;

/**
 * Scan timeline (spec §11.2), pure. Press starts scan mode; the entity under
 * the crosshair accumulates {@link IronManRules#SCAN_TICKS} of aim, a new
 * entity under the crosshair resets the count. Cancelled when the target dies,
 * leaves {@link IronManRules#SCAN_RANGE}, aim / line of sight is lost longer
 * than {@link IronManRules#SCAN_LOS_GRACE}, or the helmet opens.
 */
public final class ScanProgress {
   public enum Event {
      NONE,
      PROGRESS,
      DONE,
      CANCELLED
   }

   private boolean active;
   private int targetId = -1;
   private int ticks;
   private int lostTicks;
   private int idleTicks;

   public boolean active() {
      return this.active;
   }

   public int targetId() {
      return this.targetId;
   }

   public int ticks() {
      return this.ticks;
   }

   /** Press: starts (true) or, while active, cancels (false). */
   public boolean toggle() {
      if (this.active) {
         this.cancel();
         return false;
      }

      this.active = true;
      this.targetId = -1;
      this.ticks = 0;
      this.lostTicks = 0;
      this.idleTicks = 0;
      return true;
   }

   public void cancel() {
      this.active = false;
      this.targetId = -1;
      this.ticks = 0;
      this.lostTicks = 0;
      this.idleTicks = 0;
   }

   /** The entity the server should check this tick: the aimed one, else the current target. */
   public int resolve(int aimedId) {
      return aimedId >= 0 ? aimedId : this.targetId;
   }

   /**
    * One tick. aimedId = scannable entity under the crosshair (-1 = none);
    * the other inputs describe {@link #resolve(int)} of that id.
    */
   public Event tick(int aimedId, boolean targetAlive, double distance, boolean lineOfSight, boolean helmetClosed) {
      if (!this.active) {
         return Event.NONE;
      }

      if (!helmetClosed) {
         this.cancel();
         return Event.CANCELLED;
      }

      if (aimedId >= 0 && aimedId != this.targetId) {
         this.targetId = aimedId;
         this.ticks = 0;
         this.lostTicks = 0;
      }

      if (this.targetId < 0) {
         if (++this.idleTicks > IronManRules.SCAN_IDLE_TIMEOUT) {
            this.cancel();
            return Event.CANCELLED;
         }

         return Event.NONE;
      }

      if (!targetAlive || distance > IronManRules.SCAN_RANGE) {
         this.cancel();
         return Event.CANCELLED;
      }

      if (aimedId == this.targetId && lineOfSight) {
         this.ticks++;
         this.lostTicks = 0;
      } else if (++this.lostTicks > IronManRules.SCAN_LOS_GRACE) {
         this.cancel();
         return Event.CANCELLED;
      }

      if (this.ticks >= IronManRules.SCAN_TICKS) {
         this.active = false;
         this.lostTicks = 0;
         return Event.DONE;
      }

      return Event.PROGRESS;
   }

   /** Heroes hidden from scans (Mark 15 camo) are never offered as targets. */
   public static boolean scannable(boolean alive, boolean spectator, boolean hidden) {
      return alive && !spectator && !hidden;
   }
}
