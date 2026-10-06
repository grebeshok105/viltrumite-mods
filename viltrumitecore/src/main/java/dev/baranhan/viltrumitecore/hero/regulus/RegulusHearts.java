package dev.baranhan.viltrumitecore.hero.regulus;

import dev.baranhan.viltrumitecore.hero.CleanupReason;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * Carrier hearts: every 20 ticks scan for up to 12 valid living carriers in a
 * 20-block sphere. Carrier set is owner-private; only the count is public.
 */
public final class RegulusHearts {
   private RegulusHearts() {
   }

   /** A carrier is any living enemy or non-Regulus living player. */
   public static boolean isValidCarrier(LivingEntity entity, Player owner) {
      if (!entity.isAlive() || entity == owner) {
         return false;
      }

      if (entity instanceof Enemy) {
         return true;
      }

      if (entity instanceof Player player && player instanceof dev.baranhan.viltrumitecore.hero.HeroPlayer heroPlayer) {
         return heroPlayer.getHeroId() != dev.baranhan.viltrumitecore.hero.HeroId.REGULUS;
      }

      return entity instanceof Player;
   }

   public static void tick(ServerPlayer player, RegulusState state) {
      ServerLevel level = player.serverLevel();
      long now = level.getGameTime();
      if (now < state.nextCarrierScan) {
         pushSnapshotIfChanged(player, state);
         return;
      }

      state.nextCarrierScan = now + RegulusRules.HEART_SCAN_PERIOD;
      state.carriers.clear();
      AABB sphere = player.getBoundingBox().inflate(RegulusRules.HEART_SCAN_RADIUS);
      for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, sphere)) {
         if (state.carriers.size() >= RegulusRules.MAX_HEARTS) {
            break;
         }

         if (isValidCarrier(entity, player)) {
            state.carriers.add(entity.getUUID());
         }
      }

      pushSnapshotIfChanged(player, state);
   }

   private static void pushSnapshotIfChanged(ServerPlayer player, RegulusState state) {
      if (state.carriers.size() != state.lastCarrierCount) {
         state.lastCarrierCount = state.carriers.size();
         int[] ids = new int[state.carriers.size()];
         int index = 0;
         ServerLevel level = player.serverLevel();
         for (UUID carrierId : state.carriers) {
            Entity carrier = level.getEntity(carrierId);
            if (carrier != null) {
               ids[index++] = carrier.getId();
            }
         }

         int[] trimmed = new int[index];
         System.arraycopy(ids, 0, trimmed, 0, index);
         dev.baranhan.viltrumitecore.hero.HeroRegistry.pushOwnerSnapshot(
            player, new dev.baranhan.viltrumitecore.hero.HeroOwnerSnapshot(trimmed)
         );
      }
   }

   /** Carrier death or unload drops the heart with a 10% max-HP backlash. */
   public static void onCarrierGone(ServerPlayer player, RegulusState state, UUID carrierId) {
      if (!state.carriers.remove(carrierId)) {
         return;
      }

      dev.baranhan.viltrumitecore.hero.HeroDamage.applyInternal(player, RegulusRules.heartBacklash(player.getMaxHealth()));
      player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, RegulusRules.WEAKNESS_TICKS_ON_HEART_DEATH, 0));
      state.lastCarrierCount = -1;
      pushSnapshotIfChanged(player, state);
   }

   /** Owner-private snapshot maintenance; the set itself is cleared in cleanup(). */
   public static void releaseAll(ServerPlayer player, RegulusState state, CleanupReason reason) {
      state.carriers.clear();
      state.lastCarrierCount = -1;
      dev.baranhan.viltrumitecore.hero.HeroRegistry.pushOwnerSnapshot(
         player, dev.baranhan.viltrumitecore.hero.HeroOwnerSnapshot.EMPTY
      );
   }
}
