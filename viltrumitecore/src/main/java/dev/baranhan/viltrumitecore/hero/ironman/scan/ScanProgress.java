package dev.baranhan.viltrumitecore.hero.ironman.scan;

import dev.baranhan.viltrumitecore.hero.ironman.IronManRules;

/**
 * Scan timeline (spec §11.2), pure. One press starts scan mode and locks the
 * first offered target (the server offers the entity under the crosshair or
 * the one closest to it in a cone); the lock then scans by itself — no aiming —
 * for {@link IronManRules#SCAN_TICKS} of line of sight. Cancelled when the
 * target dies, leaves {@link IronManRules#SCAN_RANGE}, line of sight is lost
 * longer than {@link IronManRules#SCAN_LOS_GRACE}, or the helmet opens.
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

   /** True while scan mode waits for a target to lock (the server then offers one). */
   public boolean seeking() {
      return this.active && this.targetId < 0;
   }

   /** The entity the server should check this tick: the locked target, else the offered one. */
   public int resolve(int offeredId) {
      return this.targetId >= 0 ? this.targetId : offeredId;
   }

   /**
    * One tick. offeredId = target candidate while nothing is locked (-1 = none,
    * ignored once locked); the other inputs describe {@link #resolve(int)}.
    */
   public Event tick(int offeredId, boolean targetAlive, double distance, boolean lineOfSight, boolean helmetClosed) {
      if (!this.active) {
         return Event.NONE;
      }

      if (!helmetClosed) {
         this.cancel();
         return Event.CANCELLED;
      }

      if (this.targetId < 0 && offeredId >= 0) {
         this.targetId = offeredId;
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

      if (lineOfSight) {
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
