package dev.baranhan.viltrumitecore.hero.ironman.combat;

import dev.baranhan.viltrumitecore.hero.ironman.IronManRules;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;

/**
 * Micro-missile aim (spec §8.3), pure. Hold: the shoulder flaps open in
 * {@link IronManRules#MISSILE_FLAPS}; while held, targets near the crosshair
 * are marked (unique, max {@link IronManRules#MISSILE_MARKS}, line of sight).
 * Release with open flaps fires one missile per mark, or a straight fan.
 */
public final class MissileLock {
   private boolean held;
   private int ticks;
   private final List<Integer> marks = new ArrayList<>(IronManRules.MISSILE_MARKS);

   public boolean press(boolean weaponsLocked) {
      if (this.held || weaponsLocked) {
         return false;
      }

      this.held = true;
      this.ticks = 0;
      this.marks.clear();
      return true;
   }

   public void tick() {
      if (this.held) {
         this.ticks++;
      }
   }

   public boolean held() {
      return this.held;
   }

   public int ticks() {
      return this.ticks;
   }

   public boolean flapsOpen() {
      return this.held && this.ticks >= IronManRules.MISSILE_FLAPS;
   }

   /** Candidate under the crosshair. True when it became a new mark. */
   public boolean offer(int entityId, boolean lineOfSight) {
      if (!this.flapsOpen() || !lineOfSight || this.marks.size() >= IronManRules.MISSILE_MARKS || this.marks.contains(entityId)) {
         return false;
      }

      this.marks.add(entityId);
      return true;
   }

   /** A marked entity died or left: drop it (a new one may take the slot). */
   public void drop(int entityId) {
      this.marks.remove(Integer.valueOf(entityId));
   }

   public List<Integer> marks() {
      return List.copyOf(this.marks);
   }

   /** Result of a release. */
   public record Volley(boolean fire, List<Integer> targets) {
      public static final Volley NONE = new Volley(false, List.of());
   }

   /** Release: fire only with open flaps; targets empty = straight fan. */
   public Volley release() {
      if (!this.held) {
         return Volley.NONE;
      }

      boolean open = this.flapsOpen();
      List<Integer> targets = List.copyOf(this.marks);
      this.cancel();
      return open ? new Volley(true, targets) : Volley.NONE;
   }

   public void cancel() {
      this.held = false;
      this.ticks = 0;
      this.marks.clear();
   }

   public static int missileCount(int marks) {
      return marks <= 0 ? IronManRules.MISSILE_MARKS : Math.min(marks, IronManRules.MISSILE_MARKS);
   }

   /** Inside the lock cone around the aim and in range. */
   public static boolean inCone(Vec3 eye, Vec3 look, Vec3 targetCenter) {
      Vec3 to = targetCenter.subtract(eye);
      double distance = to.length();
      if (distance < 1.0E-3 || distance > IronManRules.MISSILE_LOCK_RANGE) {
         return false;
      }

      // The cone widens a bit for near targets so their hitbox counts.
      double cos = look.normalize().dot(to.scale(1.0 / distance));
      double half = Math.toRadians(IronManRules.MISSILE_LOCK_DEG) + Math.atan(0.5 / distance);
      return cos >= Math.cos(half);
   }

   /** Fan launch yaw offset (degrees) of missile i of n. */
   public static float fanOffset(int i, int n) {
      return n <= 1 ? 0.0F : (i - (n - 1) / 2.0F) * IronManRules.MISSILE_FAN_DEG;
   }
}
