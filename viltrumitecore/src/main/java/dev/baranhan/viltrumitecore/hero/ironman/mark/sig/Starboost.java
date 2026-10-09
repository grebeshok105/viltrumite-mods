package dev.baranhan.viltrumitecore.hero.ironman.mark.sig;

import dev.baranhan.viltrumitecore.hero.ironman.IronManCombatSounds;
import dev.baranhan.viltrumitecore.hero.ironman.IronManMarkSounds;
import dev.baranhan.viltrumitecore.hero.ironman.IronManState;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkSignature;
import dev.baranhan.viltrumitecore.hero.ironman.mark.SignatureState;
import dev.baranhan.viltrumiteflight.util.FlightMotion;
import dev.baranhan.viltrumiteflight.util.FlightProfiles;
import dev.baranhan.viltrumiteflight.util.FlightState;
import dev.baranhan.viltrumiteflight.util.ViltrumiteFlightPlayer;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * MARK_39: back booster. One burst to supersonic along the look, also a vertical
 * take-off from the ground (spec §13.5). The flight throttle then decays as usual.
 */
public final class Starboost implements MarkSignature {
   @Override
   public void press(ServerPlayer player, IronManState state) {
      if (!(player instanceof ViltrumiteFlightPlayer flight)) {
         return;
      }

      if (!state.energy.spend(SignatureRules.BOOST_COST)) {
         SignatureTrace.sound(player, IronManCombatSounds.REPULSOR_FIZZLE.get(), 0.8F, 1.0F);
         return;
      }

      float maxSpeed = (float)(flight.getMaxFlightSpeed() * FlightProfiles.speedScale(player));
      Vec3 velocity = boostVelocity(player.getLookAngle(), maxSpeed);
      flight.setFlightState(FlightState.SONIC);
      flight.setFlightThrottle(throttleAfterBoost());
      // The server tick would drop a grounded flight in the same tick (stale onGround from the last packet).
      player.setOnGround(false);
      player.setDeltaMovement(velocity);
      player.hasImpulse = true;
      player.hurtMarked = true;
      player.connection.send(new ClientboundSetEntityMotionPacket(player));

      SignatureState sig = state.signature;
      sig.cooldown = SignatureRules.BOOST_COOLDOWN;
      sig.active = true;
      sig.ticks = 0;
      sig.length = SignatureRules.BOOST_BLAST_TICKS;
      SignatureTrace.sound(player, IronManMarkSounds.STARBOOST.get(), 1.0F, 1.0F);
   }

   @Override
   public void tick(ServerPlayer player, IronManState state) {
      SignatureState sig = state.signature;
      if (!sig.active) {
         return;
      }

      sig.ticks++;
      if (sig.ticks >= SignatureRules.BOOST_BLAST_TICKS) {
         sig.clear();
      }
   }

   /** The burst sets the throttle to its maximum. */
   public static float throttleAfterBoost() {
      return 1.0F;
   }

   /** Full-throttle flight velocity along the look, in any direction (vertical take-off included). */
   public static Vec3 boostVelocity(Vec3 look, float maxSpeed) {
      return FlightMotion.legacyVelocity(look, throttleAfterBoost(), maxSpeed);
   }
}
