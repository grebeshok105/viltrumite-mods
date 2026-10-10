package dev.baranhan.viltrumitecore.hero.ironman.mark.sig;

import dev.baranhan.viltrumitecore.hero.HeroDebris;
import dev.baranhan.viltrumitecore.hero.HeroShockwave;
import dev.baranhan.viltrumitecore.hero.fx.HeroFx;
import dev.baranhan.viltrumitecore.hero.ironman.IronManMarkSounds;
import dev.baranhan.viltrumitecore.hero.ironman.IronManState;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkSignature;
import dev.baranhan.viltrumitecore.hero.ironman.mark.SignatureState;
import dev.baranhan.viltrumiteflight.util.FlightState;
import dev.baranhan.viltrumiteflight.util.ViltrumiteFlightPlayer;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * IRON_HEART_MK3: slam. On the ground: jump {@link SignatureRules#SLAM_JUMP_HEIGHT} blocks,
 * then dive. In flight or in the air: dive. One ring shockwave per use (spec §13.8).
 */
public final class Slam implements MarkSignature {
   public static final int IDLE = 0;
   public static final int JUMP = 1;
   public static final int DIVE = 2;

   @Override
   public void press(ServerPlayer player, IronManState state) {
      SignatureState sig = state.signature;
      if (sig.active) {
         return;
      }

      sig.cooldown = SignatureRules.SLAM_COOLDOWN;
      sig.active = true;
      sig.ticks = 0;
      sig.length = SignatureRules.SLAM_MAX_TICKS;
      sig.point = null;
      boolean flying = player instanceof ViltrumiteFlightPlayer flight && flight.getFlightState() != FlightState.NONE;
      if (!flying && player.onGround()) {
         double jump = SignatureRules.slamJumpSpeed();
         Vec3 motion = player.getDeltaMovement();
         sig.phase = JUMP;
         sig.count = SignatureRules.apexTicks(jump);
         player.setDeltaMovement(motion.x, jump, motion.z);
         sendMotion(player);
      } else {
         startDive(player, sig);
      }
   }

   @Override
   public void tick(ServerPlayer player, IronManState state) {
      SignatureState sig = state.signature;
      if (!sig.active) {
         return;
      }

      sig.ticks++;
      if (sig.phase == JUMP) {
         if (--sig.count <= 0) {
            startDive(player, sig);
         }

         return;
      }

      if (sig.phase == DIVE) {
         if (impactDue(sig.phase, player.onGround(), sig.ticks - sig.count)) {
            impact(player, state);
         } else if (sig.ticks - sig.count > SignatureRules.SLAM_MAX_TICKS) {
            sig.clear();
         }
      }
   }

   /** The impact fires once: in a dive, on the ground, after the grace ticks. */
   public static boolean impactDue(int phase, boolean onGround, int ticksSinceDive) {
      return phase == DIVE && onGround && ticksSinceDive > SignatureRules.SLAM_DIVE_GRACE;
   }

   private static void startDive(ServerPlayer player, SignatureState sig) {
      if (player instanceof ViltrumiteFlightPlayer flight && flight.getFlightState() != FlightState.NONE) {
         flight.stopFlight();
      }

      sig.phase = DIVE;
      sig.count = sig.ticks;
      sig.point = player.position();
      sig.aux = true;
      player.setDeltaMovement(0.0, -SignatureRules.SLAM_DIVE_SPEED, 0.0);
      sendMotion(player);
   }

   private static void impact(ServerPlayer player, IronManState state) {
      SignatureState sig = state.signature;
      ServerLevel level = player.serverLevel();
      double height = sig.point == null ? 0.0 : sig.point.y - player.getY();
      Vec3 at = player.position();
      BlockState ground = HeroShockwave.groundUnder(level, player);
      HeroShockwave.land(player, (float)Math.max(0.0, height), SignatureRules.SLAM_LANDING, 1.0, e -> false, IronManMarkSounds.SLAM_IMPACT.get());
      HeroFx.slam(player, at, ground, (float)SignatureRules.SLAM_RADIUS);
      Vec3 look = player.getLookAngle();
      Vec3 flat = new Vec3(look.x, 0.0, look.z);
      Vec3 forward = flat.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : flat.normalize();
      HeroDebris.erupt(player, player.getOnPos(), forward, new HeroDebris.Eruption(3.0, 2.0, 1.0, 16, 0.6, 0.9), 4.0F, e -> false);
      sig.clear();
   }

   private static void sendMotion(ServerPlayer player) {
      player.hasImpulse = true;
      player.hurtMarked = true;
      player.connection.send(new ClientboundSetEntityMotionPacket(player));
   }
}
