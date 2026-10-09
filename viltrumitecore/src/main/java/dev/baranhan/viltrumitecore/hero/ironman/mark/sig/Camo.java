package dev.baranhan.viltrumitecore.hero.ironman.mark.sig;

import dev.baranhan.viltrumitecore.hero.ironman.IronManMarkSounds;
import dev.baranhan.viltrumitecore.hero.ironman.IronManState;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkSignature;
import dev.baranhan.viltrumitecore.hero.ironman.mark.SignatureState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * MARK_15: camouflage ~8 s. Vanilla invisibility hides the body from everyone;
 * mobs farther than 8 blocks lose Tony; the first hit from camo is doubled and ends it (spec §13.4).
 */
public final class Camo implements MarkSignature {
   private static final double MOB_SEARCH = 64.0;

   @Override
   public void press(ServerPlayer player, IronManState state) {
      SignatureState sig = state.signature;
      if (sig.active) {
         return;
      }

      sig.cooldown = SignatureRules.CAMO_COOLDOWN;
      sig.active = true;
      sig.ticks = 0;
      sig.length = SignatureRules.CAMO_TICKS;
      player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, SignatureRules.CAMO_TICKS, 0, false, false, false));
      SignatureTrace.sound(player, IronManMarkSounds.CAMO_ON.get(), 1.0F, 1.0F);
   }

   @Override
   public void tick(ServerPlayer player, IronManState state) {
      SignatureState sig = state.signature;
      if (!sig.active) {
         return;
      }

      sig.ticks++;
      if (sig.ticks % SignatureRules.CAMO_MOB_CHECK == 0) {
         dropDistantMobs(player);
      }

      if (lapsed(sig.ticks)) {
         end(player, state, true);
      }
   }

   public static boolean lapsed(int ticks) {
      return ticks >= SignatureRules.CAMO_TICKS;
   }

   @Override
   public void stop(ServerPlayer player, IronManState state) {
      if (state.signature.active) {
         player.removeEffect(MobEffects.INVISIBILITY);
      }

      state.signature.clear();
   }

   /** Called once per hit: the first hit from camo is doubled, then the camo ends. */
   @Override
   public float outgoingFactor(ServerPlayer player, IronManState state, LivingEntity target) {
      if (!state.signature.active) {
         return SignatureRules.camoOutgoing(false);
      }

      end(player, state, true);
      return SignatureRules.camoOutgoing(true);
   }

   private static void end(ServerPlayer player, IronManState state, boolean withSound) {
      player.removeEffect(MobEffects.INVISIBILITY);
      state.signature.clear();
      if (withSound) {
         SignatureTrace.sound(player, IronManMarkSounds.CAMO_OFF.get(), 1.0F, 1.0F);
      }
   }

   private static void dropDistantMobs(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(MOB_SEARCH), m -> m.isAlive() && m.getTarget() == player)) {
         if (SignatureRules.camoLosesPlayer(mob.distanceTo(player))) {
            mob.setTarget(null);
         }
      }
   }
}
