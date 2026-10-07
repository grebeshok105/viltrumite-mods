package dev.baranhan.viltrumitecore.client.regulus;

/**
 * Per-player cast clock. While the server reports a cast the clock is exactly
 * the synced actionElapsed (never ahead of it: no snapshot extrapolation);
 * render code adds partialTick on top, the same way the Viltrumite mixins add
 * it to their synced tick counters. Only after the server cleared a cast that
 * reached its end does the clock count locally through the client-only
 * recovery tail.
 */
public final class RegulusActionClock {
   private int actionId = -1;
   private int ticks;
   private int visualLength;
   private boolean running;

   /**
    * One client tick. {@code syncedActionId} is -1 while the server has no cast.
    * A cast that reached its server end keeps ticking through the client-only
    * recovery tail; a cast cleared early (cancel) freezes so the layer weight
    * can fade it out.
    */
   public void tick(int syncedActionId, int syncedElapsed, int serverLength, int visualLength) {
      if (syncedActionId >= 0) {
         boolean restart = !this.running || syncedActionId != this.actionId || syncedElapsed + 2 < this.ticks;
         if (restart) {
            this.actionId = syncedActionId;
            this.ticks = Math.max(0, syncedElapsed);
            this.running = true;
         } else {
            this.ticks = syncedElapsed;
         }
         this.visualLength = Math.max(serverLength, visualLength);
         this.ticks = Math.min(this.ticks, this.visualLength);
         return;
      }

      if (!this.running) {
         return;
      }
      if (this.ticks >= serverLength - 2 && this.ticks < this.visualLength) {
         this.ticks++;
         this.running = this.ticks < this.visualLength;
      } else {
         this.running = false;
      }
   }

   public boolean isPlaying(int action) {
      return this.running && this.actionId == action;
   }

   /** Last cast id, also while frozen after a cancel. */
   public int actionId() {
      return this.actionId;
   }

   public int ticks() {
      return this.ticks;
   }

   /** Elapsed cast time for rendering; frozen once the cast stopped. */
   public float time(float partialTick) {
      return this.running ? Math.min((float)this.visualLength, (float)this.ticks + partialTick) : (float)this.ticks;
   }

   public void reset() {
      this.actionId = -1;
      this.ticks = 0;
      this.visualLength = 0;
      this.running = false;
   }
}
