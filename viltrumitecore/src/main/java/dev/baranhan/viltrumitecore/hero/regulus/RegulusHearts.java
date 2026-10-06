package dev.baranhan.viltrumitecore.hero.regulus;

import dev.baranhan.viltrumitecore.hero.CleanupReason;
import dev.baranhan.viltrumitecore.hero.HeroDamage;
import dev.baranhan.viltrumitecore.hero.HeroOwnerSnapshot;
import dev.baranhan.viltrumitecore.hero.HeroRegistry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Carrier hearts (spec 5): every 20 ticks scan for up to 12 valid living
 * carriers in a 20-block sphere. Hearts persist once bound, even past the
 * radius. A dead carrier burns its heart (10% max-HP internal backlash plus
 * Weakness I 60t); an unloaded/despawned one drops it silently. The carrier
 * set is owner-private; only the count is public.
 */
public final class RegulusHearts {
   private RegulusHearts() {
   }

   /** Spec 5.1: vanilla living, non-player, non-Enemy, no `heartless` tag. */
   public static boolean isValidCarrier(LivingEntity entity, Player owner) {
      if (!entity.isAlive() || entity == owner) {
         return false;
      }

      ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
      boolean vanilla = typeId != null && "minecraft".equals(typeId.getNamespace());
      return RegulusRules.carrierEligible(
         vanilla,
         entity instanceof Player,
         entity instanceof Enemy,
         entity.getTags().contains(RegulusRules.HEARTLESS_TAG)
      );
   }

   /** Fill the carrier set from scan candidates up to the cap; existing hearts stay. */
   public static int addCarriers(Set<UUID> carriers, Collection<UUID> candidates, int cap) {
      int added = 0;
      for (UUID candidate : candidates) {
         if (carriers.size() >= cap) {
            break;
         }

         if (carriers.add(candidate)) {
            added++;
         }
      }

      return added;
   }

   /** Drop one heart by carrier id; false when it was never bound (idempotent). */
   public static boolean dropCarrier(RegulusState state, UUID carrierId) {
      return state.carriers.remove(carrierId);
   }

   public static void tick(ServerPlayer player, RegulusState state) {
      ServerLevel level = player.serverLevel();
      long now = level.getGameTime();
      if (now >= state.nextCarrierScan) {
         state.nextCarrierScan = now + RegulusRules.HEART_SCAN_PERIOD;
         pruneGoneCarriers(player, state, level);
         scanForCarriers(player, state, level);
      }

      pushSnapshotIfChanged(player, state);
   }

   /**
    * A bound carrier that left the level dropped its heart silently (spec 5.3);
    * one that died while still tracked burns it (death semantics).
    */
   private static void pruneGoneCarriers(ServerPlayer player, RegulusState state, ServerLevel level) {
      List<UUID> gone = new ArrayList<>();
      List<UUID> dead = new ArrayList<>();
      for (UUID carrierId : state.carriers) {
         Entity carrier = level.getEntity(carrierId);
         if (carrier == null) {
            gone.add(carrierId);
         } else if (!carrier.isAlive()) {
            dead.add(carrierId);
         }
      }

      for (UUID carrierId : gone) {
         dropCarrier(state, carrierId);
      }

      for (UUID carrierId : dead) {
         onCarrierDeath(player, state, carrierId);
      }
   }

   private static void scanForCarriers(ServerPlayer player, RegulusState state, ServerLevel level) {
      if (state.carriers.size() >= RegulusRules.MAX_HEARTS) {
         return;
      }

      double radius = RegulusRules.HEART_SCAN_RADIUS;
      AABB area = player.getBoundingBox().inflate(radius);
      List<UUID> candidates = new ArrayList<>();
      for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
         if (entity.distanceToSqr(player) <= radius * radius && isValidCarrier(entity, player)) {
            candidates.add(entity.getUUID());
         }
      }

      addCarriers(state.carriers, candidates, RegulusRules.MAX_HEARTS);
   }

   private static void pushSnapshotIfChanged(ServerPlayer player, RegulusState state) {
      if (state.carriers.equals(state.pushedCarriers)) {
         return;
      }

      state.pushedCarriers.clear();
      state.pushedCarriers.addAll(state.carriers);
      ServerLevel level = player.serverLevel();
      List<Integer> ids = new ArrayList<>(state.carriers.size());
      for (UUID carrierId : state.carriers) {
         Entity carrier = level.getEntity(carrierId);
         if (carrier != null) {
            ids.add(carrier.getId());
         }
      }

      int[] entityIds = new int[ids.size()];
      for (int i = 0; i < ids.size(); i++) {
         entityIds[i] = ids.get(i);
      }

      HeroRegistry.pushOwnerSnapshot(player, new HeroOwnerSnapshot(entityIds));
   }

   /** Carrier died: the heart burns — 10% max-HP internal backlash + Weakness I 60t. */
   public static void onCarrierDeath(ServerPlayer player, RegulusState state, UUID carrierId) {
      if (!dropCarrier(state, carrierId)) {
         return;
      }

      HeroDamage.applyInternal(player, RegulusRules.heartBacklash(player.getMaxHealth()));
      player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, RegulusRules.WEAKNESS_TICKS_ON_HEART_DEATH, 0));
      pushSnapshotIfChanged(player, state);
   }

   /** Carrier unloaded/despawned/left the dimension: heart lost, no backlash. */
   public static void onCarrierLost(ServerPlayer player, RegulusState state, UUID carrierId) {
      if (dropCarrier(state, carrierId)) {
         pushSnapshotIfChanged(player, state);
      }
   }

   /** Owner death/race change/disconnect: all hearts vanish without backlash. */
   public static void releaseAll(ServerPlayer player, RegulusState state, CleanupReason reason) {
      state.carriers.clear();
      state.pushedCarriers.clear();
      HeroRegistry.pushOwnerSnapshot(player, HeroOwnerSnapshot.EMPTY);
   }
}
