package dev.baranhan.viltrumitecore.hero.ironman.mark;

/**
 * Part timeline of putting on a mark (spec §12.4), pure and shared by both
 * sides: the server decides when the suit is complete, the client draws each
 * part from the synced elapsed ticks. Delivery: parts launch from Veronica in
 * a cascade, fly, wrap the limb ({@link #WRAP} t) and lock; the helmet locks
 * last at {@link #DELIVERY} t. Entering an empty suit: no flight, {@link #ENTER} t.
 */
public final class EquipTimeline {
   public static final int DELIVERY = 50;
   public static final int ENTER = 20;
   public static final int FLIGHT = 14;
   public static final int WRAP = 8;
   public static final int ENTER_WRAP = 6;

   public enum Phase {
      WAITING,
      FLYING,
      WRAPPING,
      LOCKED
   }

   private EquipTimeline() {
   }

   public static int length(boolean delivery) {
      return delivery ? DELIVERY : ENTER;
   }

   /** Tick at which part {@code index} of {@code count} locks on the body. */
   public static int lockTick(int index, int count, boolean delivery) {
      int total = length(delivery);
      int wrap = delivery ? WRAP + FLIGHT : ENTER_WRAP;
      int first = Math.min(total, wrap);
      if (count <= 1) {
         return total;
      }

      return first + Math.round((total - first) * index / (float)(count - 1));
   }

   public static int arriveTick(int index, int count, boolean delivery) {
      return minus(lockTick(index, count, delivery), delivery ? WRAP : ENTER_WRAP);
   }

   public static int launchTick(int index, int count, boolean delivery) {
      return delivery ? minus(arriveTick(index, count, true), FLIGHT) : arriveTick(index, count, false);
   }

   private static int minus(int tick, int minus) {
      return Math.max(0, tick - minus);
   }

   public static Phase phase(int index, int count, boolean delivery, float elapsed) {
      if (elapsed >= lockTick(index, count, delivery)) {
         return Phase.LOCKED;
      }

      if (elapsed >= arriveTick(index, count, delivery)) {
         return Phase.WRAPPING;
      }

      return elapsed >= launchTick(index, count, delivery) ? Phase.FLYING : Phase.WAITING;
   }

   /** 0..1 progress inside the current phase of a part. */
   public static float phaseProgress(int index, int count, boolean delivery, float elapsed) {
      int launch = launchTick(index, count, delivery);
      int arrive = arriveTick(index, count, delivery);
      int lock = lockTick(index, count, delivery);
      return switch (phase(index, count, delivery, elapsed)) {
         case WAITING -> 0.0F;
         case FLYING -> arrive <= launch ? 1.0F : Math.min(1.0F, (elapsed - launch) / (arrive - launch));
         case WRAPPING -> lock <= arrive ? 1.0F : Math.min(1.0F, (elapsed - arrive) / (lock - arrive));
         case LOCKED -> 1.0F;
      };
   }

   /** Parts locked at {@code elapsed}, on top of the parts already present. */
   public static int lockedMask(int count, boolean delivery, int elapsed, int presentBefore) {
      int mask = presentBefore;
      for (int i = 0; i < count; i++) {
         if (elapsed >= lockTick(i, count, delivery)) {
            mask |= 1 << i;
         }
      }

      return mask;
   }
}
