package dev.baranhan.viltrumitecore.hero.ironman.mark;

import dev.baranhan.viltrumitecore.hero.ironman.IronManState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Server side of one mark's signature ability (spec §13), selected by
 * {@link MarkSignatures#of}. IronManHero routes to it without mark branches:
 * page 1 slot 3 and, when {@link #onRmb()}, RMB through the MMB tool cycle.
 * Gating (suit fully on, control, energy lock, cooldown) is done by the
 * caller before {@link #press}; state lives in {@link IronManState#signature}.
 */
public interface MarkSignature {
   /** Slot / RMB press. */
   void press(ServerPlayer player, IronManState state);

   /** Release of a held signature (gun, laser). Always routed, also after the suit is gone. */
   default void release(ServerPlayer player, IronManState state) {
   }

   /** Every server tick while this mark is worn (also while idle: passives). */
   default void tick(ServerPlayer player, IronManState state) {
   }

   /** Everything off at once, no effects (suit change, control, death, cleanup). */
   default void stop(ServerPlayer player, IronManState state) {
      state.signature.clear();
   }

   /** The signature is also an RMB tool (MMB cycle repulsor ↔ signature). */
   default boolean onRmb() {
      return false;
   }

   /** Held input: the slot key / RMB release ends it ({@link #release}). */
   default boolean held() {
      return false;
   }

   /** Outgoing damage factor of one hit while worn (Mark 15: first hit from camo ×2). */
   default float outgoingFactor(ServerPlayer player, IronManState state, LivingEntity target) {
      return 1.0F;
   }
}
