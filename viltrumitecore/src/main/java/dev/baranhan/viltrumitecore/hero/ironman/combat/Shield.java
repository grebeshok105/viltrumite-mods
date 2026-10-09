package dev.baranhan.viltrumitecore.hero.ironman.combat;

import dev.baranhan.viltrumitecore.hero.ironman.Energy;
import dev.baranhan.viltrumitecore.hero.ironman.IronManRules;
import javax.annotation.Nullable;
import net.minecraft.world.phys.Vec3;

/**
 * Nano shield on F (spec §8.5), pure. Raised instantly on guard down,
 * collapses on guard up. Absorbs hits from the front cone for 4 energy each.
 * A hit within {@link IronManRules#PERFECT_BLOCK_TICKS} of the raise (server
 * time of the guard-down packet) is a perfect block.
 */
public final class Shield {
   public enum Block {
      PASS,
      ABSORBED,
      PERFECT
   }

   private boolean raised;
   private long raisedAt = Long.MIN_VALUE;

   /** Guard down; false when the energy is locked (no shield). */
   public boolean raise(long now, Energy energy) {
      if (energy.weaponsLocked()) {
         return false;
      }

      this.raised = true;
      this.raisedAt = now;
      return true;
   }

   public void lower() {
      this.raised = false;
   }

   public boolean raised() {
      return this.raised;
   }

   public long raisedAt() {
      return this.raisedAt;
   }

   public boolean perfectWindow(long now) {
      return this.raised && now - this.raisedAt >= 0 && now - this.raisedAt <= IronManRules.PERFECT_BLOCK_TICKS;
   }

   /** Front cone test: horizontal look vs the direction to the hit source. No direction = not blocked. */
   public static boolean inFront(Vec3 look, @Nullable Vec3 toSource) {
      if (toSource == null) {
         return false;
      }

      Vec3 a = new Vec3(look.x, 0.0, look.z);
      Vec3 b = new Vec3(toSource.x, 0.0, toSource.z);
      if (a.lengthSqr() < 1.0E-6 || b.lengthSqr() < 1.0E-6) {
         // Straight up / down: use full 3D vectors.
         a = look;
         b = toSource;
      }

      double cos = a.normalize().dot(b.normalize());
      return cos >= Math.cos(Math.toRadians(IronManRules.SHIELD_CONE_DEG / 2.0F));
   }

   /** One incoming hit. Spends energy; no energy → the hit passes. */
   public Block hit(long now, Vec3 look, @Nullable Vec3 toSource, Energy energy) {
      if (!this.raised || energy.weaponsLocked() || !inFront(look, toSource)) {
         return Block.PASS;
      }

      if (!energy.spend(IronManRules.COST_SHIELD_HIT)) {
         return Block.PASS;
      }

      return this.perfectWindow(now) ? Block.PERFECT : Block.ABSORBED;
   }
}
