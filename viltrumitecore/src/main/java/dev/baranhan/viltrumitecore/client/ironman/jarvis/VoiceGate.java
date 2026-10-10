package dev.baranhan.viltrumitecore.client.ironman.jarvis;

import dev.baranhan.viltrumitecore.hero.ironman.IronManRules;
import java.util.HashMap;
import java.util.Map;

/**
 * JARVIS voice gate (spec §11.1), pure: never two lines at once, a global gap
 * of {@link IronManRules#JARVIS_GLOBAL_GAP} after a line ends, and a
 * per-line cooldown.
 */
public final class VoiceGate {
   private long busyUntil = Long.MIN_VALUE;
   private final Map<String, Long> nextAllowed = new HashMap<>();

   /** True when the line may play now (and it is booked). */
   public boolean tryPlay(String line, long now, int durationTicks, int cooldownTicks) {
      if (now < this.busyUntil || now < this.nextAllowed.getOrDefault(line, Long.MIN_VALUE)) {
         return false;
      }

      this.busyUntil = now + Math.max(0, durationTicks) + IronManRules.JARVIS_GLOBAL_GAP;
      this.nextAllowed.put(line, now + Math.max(0, cooldownTicks));
      return true;
   }

   public boolean busy(long now) {
      return now < this.busyUntil;
   }

   public void reset() {
      this.busyUntil = Long.MIN_VALUE;
      this.nextAllowed.clear();
   }
}
