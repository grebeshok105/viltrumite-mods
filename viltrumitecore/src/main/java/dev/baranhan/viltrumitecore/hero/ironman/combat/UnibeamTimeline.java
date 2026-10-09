package dev.baranhan.viltrumitecore.hero.ironman.combat;

import dev.baranhan.viltrumitecore.hero.ironman.IronManRules;
import java.util.Collection;
import net.minecraft.world.phys.Vec3;

/**
 * Unibeam state machine (spec §8.2), pure. Hold the slot: CHARGE 20 t, then
 * BEAM while held (max 60 t, no cap in overdraft), then OVERHEAT 40 t with
 * weapons locked. A release during CHARGE cancels without cost.
 */
public final class UnibeamTimeline {
   public enum Phase {
      IDLE,
      CHARGE,
      BEAM,
      OVERHEAT
   }

   public enum Event {
      NONE,
      /** The charge completed: the caller spends energy and calls startBeam or refuse. */
      CHARGED,
      /** The beam reached its cap and ended in an overheat. */
      BEAM_MAX,
      OVERHEAT_END
   }

   private Phase phase = Phase.IDLE;
   private int ticks;
   private boolean overdraft;

   public Phase phase() {
      return this.phase;
   }

   /** Ticks in the current phase. */
   public int ticks() {
      return this.ticks;
   }

   public boolean overdraft() {
      return this.overdraft;
   }

   public boolean active() {
      return this.phase == Phase.CHARGE || this.phase == Phase.BEAM;
   }

   public boolean overheated() {
      return this.phase == Phase.OVERHEAT;
   }

   /** Start the charge; false when busy, overheated or weapons are locked. */
   public boolean press(boolean weaponsLocked) {
      if (this.phase != Phase.IDLE || weaponsLocked) {
         return false;
      }

      this.set(Phase.CHARGE);
      return true;
   }

   public Event tick() {
      this.ticks++;
      switch (this.phase) {
         case CHARGE -> {
            if (this.ticks >= IronManRules.UNIBEAM_CHARGE) {
               return Event.CHARGED;
            }
         }
         case BEAM -> {
            if (!this.overdraft && this.ticks >= IronManRules.UNIBEAM_MAX) {
               this.set(Phase.OVERHEAT);
               return Event.BEAM_MAX;
            }
         }
         case OVERHEAT -> {
            if (this.ticks >= IronManRules.UNIBEAM_OVERHEAT_LOCK) {
               this.set(Phase.IDLE);
               return Event.OVERHEAT_END;
            }
         }
         default -> {
         }
      }

      return Event.NONE;
   }

   /** After CHARGED with energy spent: the beam is on. */
   public void startBeam(boolean overdraft) {
      this.overdraft = overdraft;
      this.set(Phase.BEAM);
   }

   /** After CHARGED without energy: back to idle, nothing fired. */
   public void refuse() {
      this.set(Phase.IDLE);
   }

   /**
    * Key released. CHARGE → IDLE (no cost); BEAM → OVERHEAT. Returns true
    * when a beam ended (an overheat to count).
    */
   public boolean release() {
      if (this.phase == Phase.CHARGE) {
         this.set(Phase.IDLE);
         return false;
      }

      if (this.phase == Phase.BEAM) {
         this.set(Phase.OVERHEAT);
         return true;
      }

      return false;
   }

   /**
    * Control (spec §16) ends the channel like a normal end: CHARGE → IDLE,
    * BEAM → OVERHEAT. An overdraft beam is not cancelled.
    */
   public boolean interrupt() {
      if (this.phase == Phase.BEAM && this.overdraft) {
         return false;
      }

      return this.release();
   }

   /** Death, hero change, explosion: everything off at once. */
   public void clear() {
      this.set(Phase.IDLE);
      this.overdraft = false;
   }

   private void set(Phase next) {
      this.phase = next;
      this.ticks = 0;
      if (next != Phase.BEAM) {
         this.overdraft = false;
      }
   }

   // ---- pure beam rules ----

   /** Damage tick: first beam tick and every UNIBEAM_HIT_INTERVAL after. */
   public static boolean damageTick(int beamTicks) {
      return beamTicks >= 0 && beamTicks % IronManRules.UNIBEAM_HIT_INTERVAL == 0;
   }

   /** Per-hit damage: full up to UNIBEAM_NEAR, linear to UNIBEAM_HIT_FAR at the range end. */
   public static float damageAt(double distance, boolean overdraft) {
      float base;
      if (distance <= IronManRules.UNIBEAM_NEAR) {
         base = IronManRules.UNIBEAM_HIT;
      } else {
         double t = Math.min(1.0, (distance - IronManRules.UNIBEAM_NEAR) / (IronManRules.UNIBEAM_RANGE - IronManRules.UNIBEAM_NEAR));
         base = (float)(IronManRules.UNIBEAM_HIT + (IronManRules.UNIBEAM_HIT_FAR - IronManRules.UNIBEAM_HIT) * t);
      }

      return overdraft ? base * IronManRules.OVERDRAFT_DAMAGE_MULT : base;
   }

   /** Turns {@code current} towards {@code target} by at most maxDeg (both unit vectors). */
   public static Vec3 turn(Vec3 current, Vec3 target, float maxDeg) {
      Vec3 a = current.normalize();
      Vec3 b = target.normalize();
      double dot = Math.max(-1.0, Math.min(1.0, a.dot(b)));
      double angle = Math.acos(dot);
      double max = Math.toRadians(maxDeg);
      if (angle <= max || angle < 1.0E-6) {
         return b;
      }

      // Slerp by max / angle.
      double t = max / angle;
      double sin = Math.sin(angle);
      double wa = Math.sin((1.0 - t) * angle) / sin;
      double wb = Math.sin(t * angle) / sin;
      Vec3 out = a.scale(wa).add(b.scale(wb));
      return out.lengthSqr() < 1.0E-9 ? a : out.normalize();
   }

   /** Spec §8.2: only glass and leaves break under the beam. Tag ids as "namespace:path". */
   public static boolean breaksBlock(Collection<String> blockTags) {
      return blockTags.contains("minecraft:leaves") || blockTags.contains("forge:glass") || blockTags.contains("forge:glass_panes")
         || blockTags.contains("minecraft:impermeable");
   }

   /** Mob/player looking into the beam: angle between its look and the direction to the source. */
   public static boolean looksInto(Vec3 look, Vec3 toSource, double distance) {
      if (distance > IronManRules.UNIBEAM_BLIND_RANGE || toSource.lengthSqr() < 1.0E-6) {
         return false;
      }

      double cos = look.normalize().dot(toSource.normalize());
      return cos >= Math.cos(Math.toRadians(IronManRules.UNIBEAM_BLIND_ANGLE));
   }

   /** Flash ticks; PvP (target is a player) is capped short. */
   public static int flashTicks(int ticks, boolean targetIsPlayer) {
      return targetIsPlayer ? Math.min(ticks, IronManRules.FLASH_PVP_MAX_TICKS) : ticks;
   }
}
