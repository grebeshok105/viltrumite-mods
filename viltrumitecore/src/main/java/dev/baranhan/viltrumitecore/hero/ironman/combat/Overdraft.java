package dev.baranhan.viltrumitecore.hero.ironman.combat;

import dev.baranhan.viltrumitecore.hero.ironman.IronManRules;

/**
 * Overdraft timeline (spec §9.3), pure. Starts with the third overheating
 * beam. +40 t sputter, +30 t after that the core explodes. A release before
 * the blast turns the beam off; the explosion still comes at the same tick,
 * weaker. Death cancels the pending explosion; control does not.
 */
public final class Overdraft {
   public enum Event {
      NONE,
      SPUTTER,
      EXPLODE
   }

   private boolean active;
   private boolean released;
   private int ticks;

   public void start() {
      this.active = true;
      this.released = false;
      this.ticks = 0;
   }

   public Event tick() {
      if (!this.active) {
         return Event.NONE;
      }

      this.ticks++;
      if (this.ticks == IronManRules.OVERDRAFT_SPUTTER_AT) {
         return Event.SPUTTER;
      }

      if (this.ticks >= explodeAt()) {
         this.active = false;
         return Event.EXPLODE;
      }

      return Event.NONE;
   }

   public static int explodeAt() {
      return IronManRules.OVERDRAFT_SPUTTER_AT + IronManRules.OVERDRAFT_EXPLODE_AFTER_SPUTTER;
   }

   /** The key was released before the blast: beam off, explosion weaker. */
   public void release() {
      if (this.active) {
         this.released = true;
      }
   }

   /** Death or hero change: no explosion. */
   public void cancel() {
      this.active = false;
      this.released = false;
      this.ticks = 0;
   }

   public boolean active() {
      return this.active;
   }

   public boolean released() {
      return this.released;
   }

   public boolean sputtering() {
      return this.active && this.ticks >= IronManRules.OVERDRAFT_SPUTTER_AT;
   }

   public int ticks() {
      return this.ticks;
   }

   /** Explosion power for the current state. */
   public float power() {
      return this.released ? IronManRules.CORE_EXPLOSION_POWER_RELEASED : IronManRules.CORE_EXPLOSION_POWER;
   }

   /**
    * Spec §9.4 survival floor: HP loss from the own core explosion leaves at
    * least CORE_SURVIVE_HP (only when the player has more than that).
    */
   public static float survivalClamp(float health, float afterArmor) {
      float allowed = Math.max(0.0F, health - IronManRules.CORE_SURVIVE_HP);
      return Math.min(afterArmor, allowed);
   }

   /** Keyed by the source, not the tick: explosion type, caused by self, inside the blast window. */
   public static boolean isOwnCoreExplosion(boolean explosionType, boolean causedBySelf, int windowTicks) {
      return explosionType && causedBySelf && windowTicks > 0;
   }
}
