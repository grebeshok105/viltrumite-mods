package dev.baranhan.viltrumitecore.hero.ironman.combat;

import dev.baranhan.viltrumitecore.hero.ironman.IronManRules;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Hammer launch (spec §8.4), pure: launched targets break soft blocks on
 * their path for {@link IronManRules#HAMMER_LAUNCH_TICKS}.
 */
public final class HammerLaunch {
   private final Map<Integer, Integer> flying = new HashMap<>();

   public void launch(int entityId) {
      this.flying.put(entityId, IronManRules.HAMMER_LAUNCH_TICKS);
   }

   public boolean isFlying(int entityId) {
      return this.flying.containsKey(entityId);
   }

   public Iterable<Integer> targets() {
      return Map.copyOf(this.flying).keySet();
   }

   public void tick() {
      Iterator<Map.Entry<Integer, Integer>> it = this.flying.entrySet().iterator();
      while (it.hasNext()) {
         Map.Entry<Integer, Integer> entry = it.next();
         int left = entry.getValue() - 1;
         if (left <= 0) {
            it.remove();
         } else {
            entry.setValue(left);
         }
      }
   }

   public void stop(int entityId) {
      this.flying.remove(entityId);
   }

   public void clear() {
      this.flying.clear();
   }

   /** Soft = breakable (speed ≥ 0) and destroy speed at most HAMMER_SOFT_HARDNESS. Air never. */
   public static boolean soft(boolean air, float destroySpeed) {
      return !air && destroySpeed >= 0.0F && destroySpeed <= IronManRules.HAMMER_SOFT_HARDNESS;
   }

   /** Launch speed by charge ticks. */
   public static double speed(int chargeTicks) {
      float t = Math.max(0.0F, Math.min(1.0F, chargeTicks / (float)IronManRules.HAMMER_CHARGE_MAX));
      return IronManRules.HAMMER_LAUNCH_MIN + (IronManRules.HAMMER_LAUNCH_MAX - IronManRules.HAMMER_LAUNCH_MIN) * t;
   }

   /** A hammer hit on an airborne target drives it down instead of away. */
   public static boolean slamsDown(boolean targetOnGround, double heightAboveGround) {
      return !targetOnGround && heightAboveGround > 1.0;
   }
}
