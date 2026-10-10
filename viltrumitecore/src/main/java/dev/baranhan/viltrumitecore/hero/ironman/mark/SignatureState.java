package dev.baranhan.viltrumitecore.hero.ironman.mark;

import javax.annotation.Nullable;
import net.minecraft.world.phys.Vec3;

/**
 * Generic per-player state of the worn mark's signature (spec §13). Each
 * {@link MarkSignature} uses the fields it needs; {@link #clear()} runs on
 * every suit change. Only {@link #cooldown} is persisted (spec §16).
 */
public final class SignatureState {
   /** Ticks left until the signature can start again (extraCooldowns[3]). */
   public int cooldown;
   /** Channel ticks (0 = idle); meaning per signature. */
   public int ticks;
   /** Channel length for the snapshot timeline (0 = no timeline). */
   public int length;
   /** Phase / counter per signature. */
   public int phase;
   public int count;
   /** Held by the player (RMB or slot hold). */
   public boolean held;
   /** Entity id per signature (rocket fist, laser target), -1 = none. */
   public int entityId = -1;
   /** Point per signature (beam end, dive target); synced as the snapshot actionTarget when set. */
   @Nullable
   public Vec3 point;
   /** Visual bits for the snapshot (IronManFlags SIGNATURE_ACTIVE / SIGNATURE_AUX). */
   public boolean active;
   public boolean aux;

   public void clear() {
      this.ticks = 0;
      this.length = 0;
      this.phase = 0;
      this.count = 0;
      this.held = false;
      this.entityId = -1;
      this.point = null;
      this.active = false;
      this.aux = false;
   }

   public void tickCooldown() {
      if (this.cooldown > 0) {
         this.cooldown--;
      }
   }
}
