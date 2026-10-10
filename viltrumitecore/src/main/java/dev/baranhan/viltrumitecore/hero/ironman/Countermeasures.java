package dev.baranhan.viltrumitecore.hero.ironman;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Countermeasure flares (spec §11.3), pure timers. Fire: cooldown
 * {@link IronManRules#COUNTERMEASURES_COOLDOWN}, no energy. Mobs that had the
 * player as target forget it for {@link IronManRules#FLARE_FORGET_TICKS}
 * (re-cleared every tick), then act normally. Bosses are never affected.
 */
public final class Countermeasures {
   private int cooldown;
   private final Map<Integer, Integer> forget = new HashMap<>();

   public int cooldown() {
      return this.cooldown;
   }

   public boolean ready() {
      return this.cooldown <= 0;
   }

   /** True when the flares fire (cooldown starts). */
   public boolean fire() {
      if (!this.ready()) {
         return false;
      }

      this.cooldown = IronManRules.COUNTERMEASURES_COOLDOWN;
      return true;
   }

   /** Energy cost of a launch: none (spec §11.3). */
   public static float energyCost() {
      return 0.0F;
   }

   /** A homing projectile on the player (target entity id) gets a flare. */
   public static boolean retargets(int homingTargetId, int playerId) {
      return homingTargetId >= 0 && homingTargetId == playerId;
   }

   /** Withers and the Ender Dragon ignore flares. */
   public static boolean affects(boolean boss) {
      return !boss;
   }

   public void forget(int mobId) {
      this.forget.put(mobId, IronManRules.FLARE_FORGET_TICKS);
   }

   public boolean forgetting(int mobId) {
      return this.forget.containsKey(mobId);
   }

   public Set<Integer> forgetting() {
      return this.forget.keySet();
   }

   public void tick() {
      if (this.cooldown > 0) {
         this.cooldown--;
      }

      Iterator<Map.Entry<Integer, Integer>> it = this.forget.entrySet().iterator();
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

   public void clearForget() {
      this.forget.clear();
   }

   /** Death keeps the cooldown (like the nano lock); hero change clears it. */
   public void setCooldown(int ticks) {
      this.cooldown = Math.max(0, ticks);
   }
}
