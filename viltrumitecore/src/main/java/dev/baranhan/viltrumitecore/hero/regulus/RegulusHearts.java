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
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.network.chat.Component;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Carrier hearts: manual server-resolved gaze assignment, up to 12 within
 * 20 blocks. Hearts persist once bound, even past the
 * radius. A dead carrier burns its heart (10% max-HP internal backlash plus
 * Weakness I 60t); an unloaded/despawned one drops it silently. The carrier
 * set is owner-private; only the count is public.
 */
public final class RegulusHearts {
   private RegulusHearts() {
   }

   enum Assignment {
      ASSIGNED, REMOVED, FULL, INVALID
   }

   static Assignment toggleCarrier(RegulusState state, UUID carrierId, boolean valid) {
      if (!valid) {
         return Assignment.INVALID;
      }
      if (dropCarrier(state, carrierId)) {
         return Assignment.REMOVED;
      }
      if (state.hearts() >= RegulusRules.MAX_HEARTS) {
         return Assignment.FULL;
      }
      state.carriers.add(carrierId);
      return Assignment.ASSIGNED;
   }

   public static void assignLookedAt(ServerPlayer player, RegulusState state) {
      Vec3 eye = player.getEyePosition();
      Vec3 end = eye.add(player.getLookAngle().scale(RegulusRules.HEART_SCAN_RADIUS));
      Vec3 clipped = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();
      LivingEntity target = null;
      double nearest = eye.distanceTo(clipped);
      AABB area = new AABB(eye, clipped).inflate(1.0);
      for (LivingEntity entity : player.serverLevel().getEntitiesOfClass(LivingEntity.class, area, e -> e != player)) {
         double distance = DebrisKick.rayDistance(eye, clipped, entity.getBoundingBox().inflate(0.1));
         if (distance >= 0.0 && distance < nearest) {
            nearest = distance;
            target = entity;
         }
      }
      if (target == null) {
         feedback(player, "no_target");
         return;
      }
      boolean valid = target.level() == player.level() && isValidCarrier(target, player)
         && target.distanceToSqr(player) <= RegulusRules.HEART_SCAN_RADIUS * RegulusRules.HEART_SCAN_RADIUS
         && player.hasLineOfSight(target);
      Assignment result = toggleCarrier(state, target.getUUID(), valid);
      if (result == Assignment.ASSIGNED) {
         bindCarrierLevels(state, List.of(target.getUUID()), player.level().dimension().location());
      }
      feedback(player, result.name().toLowerCase(java.util.Locale.ROOT));
      pushSnapshotIfChanged(player, state);
   }

   private static void feedback(ServerPlayer player, String result) {
      player.displayClientMessage(Component.translatable("message.viltrumitecore.hearts." + result), true);
   }

   /** How a bound carrier reads when looked up in its own dimension (spec 5.3). */
   enum CarrierStatus {
      BOUND,
      DEAD,
      GONE
   }

   /** Resolved in its level: bound if alive, dead if not; unresolved is gone (unloaded/despawned). */
   static CarrierStatus carrierStatus(boolean entityFound, boolean entityAlive) {
      if (!entityFound) {
         return CarrierStatus.GONE;
      }

      return entityAlive ? CarrierStatus.BOUND : CarrierStatus.DEAD;
   }

   /**
    * Spec 5.1: vanilla living, non-player, non-Enemy, no `heartless` tag — and
    * a real creature (Mob). Decoration LivingEntity types like armor stands
    * are not "живое существо" and would grant free permanent hearts.
    */
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
         entity.getTags().contains(RegulusRules.HEARTLESS_TAG),
         entity instanceof Mob
      );
   }

   /**
    * Fill the carrier set from scan candidates up to the cap; existing hearts
    * stay. Returns the ids actually bound so level bindings are never written
    * for candidates the cap rejected.
    */
   public static List<UUID> addCarriers(Set<UUID> carriers, Collection<UUID> candidates, int cap) {
      List<UUID> added = new ArrayList<>();
      for (UUID candidate : candidates) {
         if (carriers.size() >= cap) {
            break;
         }

         if (carriers.add(candidate)) {
            added.add(candidate);
         }
      }

      return added;
   }

   /** Drop one heart by carrier id; false when it was never bound (idempotent). */
   public static boolean dropCarrier(RegulusState state, UUID carrierId) {
      state.carrierLevels.remove(carrierId);
      return state.carriers.remove(carrierId);
   }

   /** Record the dimension each new carrier was bound in; an existing binding wins. */
   static void bindCarrierLevels(RegulusState state, Collection<UUID> candidates, ResourceLocation levelId) {
      for (UUID candidate : candidates) {
         state.carrierLevels.putIfAbsent(candidate, levelId);
      }
   }

   /**
    * The level a carrier was bound in — the heart lives there, not wherever
    * the owner currently stands, so a cross-dimension death still burns.
    */
   private static ServerLevel carrierLevelOf(ServerPlayer player, RegulusState state, UUID carrierId) {
      ResourceLocation levelId = state.carrierLevels.get(carrierId);
      MinecraftServer server = player.getServer();
      if (levelId == null || server == null) {
         return player.serverLevel();
      }

      ServerLevel carrierLevel = server.getLevel(ResourceKey.create(Registries.DIMENSION, levelId));
      return carrierLevel != null ? carrierLevel : player.serverLevel();
   }

   public static void tick(ServerPlayer player, RegulusState state) {
      ServerLevel level = player.serverLevel();
      long now = level.getGameTime();
      if (now >= state.nextCarrierScan) {
         state.nextCarrierScan = now + RegulusRules.HEART_SCAN_PERIOD;
         pruneGoneCarriers(player, state);
      }

      pushSnapshotIfChanged(player, state);
   }

   /**
    * A bound carrier that left the level dropped its heart silently (spec 5.3);
    * one that died while still tracked burns it (death semantics).
    */
   private static void pruneGoneCarriers(ServerPlayer player, RegulusState state) {
      List<UUID> gone = new ArrayList<>();
      List<UUID> dead = new ArrayList<>();
      for (UUID carrierId : state.carriers) {
         Entity carrier = carrierLevelOf(player, state, carrierId).getEntity(carrierId);
         switch (carrierStatus(carrier != null, carrier != null && carrier.isAlive())) {
            case GONE -> gone.add(carrierId);
            case DEAD -> dead.add(carrierId);
            default -> {
            }
         }
      }

      for (UUID carrierId : gone) {
         dropCarrier(state, carrierId);
      }

      for (UUID carrierId : dead) {
         onCarrierDeath(player, state, carrierId);
      }
   }

   private static void pushSnapshotIfChanged(ServerPlayer player, RegulusState state) {
      if (state.carriers.equals(state.pushedCarriers)) {
         return;
      }

      state.pushedCarriers.clear();
      state.pushedCarriers.addAll(state.carriers);
      List<Integer> ids = new ArrayList<>(state.carriers.size());
      for (UUID carrierId : state.carriers) {
         Entity carrier = carrierLevelOf(player, state, carrierId).getEntity(carrierId);
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
      state.carrierLevels.clear();
      state.pushedCarriers.clear();
      HeroRegistry.pushOwnerSnapshot(player, HeroOwnerSnapshot.EMPTY);
   }
}
