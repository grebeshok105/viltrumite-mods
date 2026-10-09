package dev.baranhan.viltrumitecore.hero.ironman;

import dev.baranhan.viltrumitecore.hero.HeroDebris;
import dev.baranhan.viltrumitecore.hero.HeroShockwave;
import dev.baranhan.viltrumitecore.hero.fx.HeroFx;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Server side of touchdowns (spec §8.6; visuals in 1b). SOFT: small dust.
 * HEAVY: small shockwave (camera shake through HeroFx.shockwave) + kneel
 * flag. AIR_STRIKE: full shockwave + crater; blocks only through
 * HeroDestruction (mobGriefing).
 */
final class IronManLandings {
   private IronManLandings() {
   }

   static void land(ServerPlayer player, IronManState state, LandingKind kind, double impactSpeed) {
      switch (kind) {
         case SOFT -> {
            state.landedAt = player.level().getGameTime();
            BlockState ground = HeroShockwave.groundUnder(player.serverLevel(), player);
            HeroFx.launch(player, player.position(), 0.25F, ground);
         }
         case HEAVY -> heavy(player, state, (float)LandingKind.equivalentFall(impactSpeed, IronManRules.HEAVY_LANDING.fullPowerFall()));
         case AIR_STRIKE -> airStrike(player, state);
         default -> {
         }
      }
   }

   static void heavy(ServerPlayer player, IronManState state, float fall) {
      state.landedAt = player.level().getGameTime();
      state.heavyPoseTicks = IronManRules.HEAVY_POSE_TICKS;
      HeroShockwave.land(player, Math.max(fall, IronManRules.HEAVY_FALL), IronManRules.HEAVY_LANDING, 1.0, player::isAlliedTo, null);
   }

   static void airStrike(ServerPlayer player, IronManState state) {
      state.landedAt = player.level().getGameTime();
      state.heavyPoseTicks = IronManRules.HEAVY_POSE_TICKS;
      state.airStrike.consume();
      HeroShockwave.land(player, IronManRules.AIR_STRIKE_LANDING.fullPowerFall(), IronManRules.AIR_STRIKE_LANDING, 1.0, player::isAlliedTo, null);
      Vec3 look = player.getLookAngle();
      Vec3 flat = new Vec3(look.x, 0.0, look.z);
      Vec3 forward = flat.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
      HeroDebris.erupt(player, player.getOnPos(), forward, IronManRules.AIR_STRIKE_CRATER, IronManRules.AIR_STRIKE_DEBRIS_DAMAGE, player::isAlliedTo);
   }

   /** Distance to the ground along the look, or MAX_VALUE beyond the arm range. */
   static double groundAhead(ServerPlayer player) {
      Vec3 eye = player.getEyePosition();
      double reach = IronManRules.AIR_STRIKE_ARM_DIST + 1.0;
      HitResult hit = player.level().clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(reach)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
      return hit.getType() == HitResult.Type.MISS ? Double.MAX_VALUE : hit.getLocation().distanceTo(eye);
   }
}
