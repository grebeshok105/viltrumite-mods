package dev.baranhan.viltrumitecore.hero;

import dev.baranhan.viltrumitecore.hero.control.ControlManager;
import dev.baranhan.viltrumitecore.hero.control.HeroControlSync;
import dev.baranhan.viltrumitecore.hero.regulus.Evangelium;
import dev.baranhan.viltrumitecore.hero.regulus.RegulusHero;
import dev.baranhan.viltrumitecore.hero.regulus.RegulusState;
import dev.baranhan.viltrumitecore.item.ViltrumiteItems;
import dev.baranhan.viltrumitecore.util.ViltrumiteAbilityUser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Hero lifecycle glue on the Forge bus: clone restore, death/disconnect
 * cleanup, world control ticking, fall handling and carrier bookkeeping.
 */
@EventBusSubscriber(modid = "viltrumitecore")
public final class HeroEvents {
   private HeroEvents() {
   }

   /**
    * Clone (respawn / end-return): restore the SAME hero session — identity,
    * session id and consumed totem — and copy all 18 ability slots. Restore is
    * never a new session, so the totem does not refresh on respawn.
    */
   @SubscribeEvent
   public static void onPlayerClone(PlayerEvent.Clone event) {
      Player original = event.getOriginal();
      Player clone = event.getEntity();
      if (!(original instanceof HeroPlayer originalHero) || !(clone instanceof ServerPlayer newPlayer)) {
         return;
      }

      HeroRegistry.restoreHero(newPlayer, originalHero.getHeroSession());
      // The hero decides what carries over to the clone (Regulus: cooldowns).
      HeroRegistry.get(newPlayer).cloneHeroState(original, newPlayer);

      if (newPlayer instanceof ViltrumiteAbilityUser newAbility && original instanceof ViltrumiteAbilityUser oldAbility) {
         for (int slot = 0; slot < 18; slot++) {
            newAbility.setAbilityInSlot(slot, oldAbility.getAbilityInSlot(slot));
         }

         newAbility.setActivePage(oldAbility.getActivePage());
      }
   }

   @SubscribeEvent
   public static void onPlayerDeath(LivingDeathEvent event) {
      LivingEntity entity = event.getEntity();
      if (entity.level().isClientSide()) {
         return;
      }

      if (entity instanceof ServerPlayer player && player instanceof HeroPlayer heroPlayer) {
         // The hero's own cleanup runs before the death completes.
         HeroRegistry.get(player).cleanup(player, CleanupReason.DEATH);
      }

      // A dead carrier costs its Regulus a heart (backlash handled inside).
      // The owner may be in a different dimension than the dying carrier, so
      // scan every online player — spec 5.3 burns the heart regardless.
      MinecraftServer server = entity.getServer();
      if (server != null) {
         for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            RegulusState state = RegulusHero.stateOf(player);
            if (state != null && state.carriers.contains(entity.getUUID())) {
               dev.baranhan.viltrumitecore.hero.regulus.RegulusHearts.onCarrierDeath(player, state, entity.getUUID());
            }
         }
      }
   }

   /**
    * A carrier that leaves the level without dying (unload, despawn, dimension
    * change) drops the heart silently — no backlash (spec 5.3). Owners in
    * other dimensions are included: the carrier-level prune would read the
    * same departure as "gone" on its next scan either way.
    */
   @SubscribeEvent
   public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
      Entity entity = event.getEntity();
      if (!(entity instanceof LivingEntity)) {
         return;
      }

      MinecraftServer server = entity.getServer();
      if (server == null) {
         return;
      }

      for (ServerPlayer player : server.getPlayerList().getPlayers()) {
         RegulusState state = RegulusHero.stateOf(player);
         if (state != null && state.carriers.contains(entity.getUUID())) {
            dev.baranhan.viltrumitecore.hero.regulus.RegulusHearts.onCarrierLost(player, state, entity.getUUID());
         }
      }
   }

   /**
    * Anchored victims (FREEZE/STASIS) cannot attack, use items or spawn
    * projectiles — spec 8.3/9.2 denies their own actions outright; PULL keeps
    * them (isAnchored covers only the anchoring kinds).
    */
   private static boolean anchored(Entity entity) {
      return entity instanceof LivingEntity living && HeroDamage.isAnchored(living);
   }

   @SubscribeEvent
   public static void onAttackEntity(AttackEntityEvent event) {
      if (anchored(event.getEntity())) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void onArrowLoose(ArrowLooseEvent event) {
      if (anchored(event.getEntity())) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void onItemUseStart(LivingEntityUseItemEvent.Start event) {
      if (anchored(event.getEntity())) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
      if (anchored(event.getEntity())) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
      if (anchored(event.getEntity())) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
      if (anchored(event.getEntity())) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
      if (anchored(event.getEntity())) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
      if (anchored(event.getEntity())) {
         event.setCanceled(true);
      }
   }

   /** A frozen projectile rejoining a level after its record dropped gets its saved gravity back. */
   @SubscribeEvent
   public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
      if (event.getLevel().isClientSide()) {
         return;
      }

      // Deferred restores run first: a cancelled projectile join below must
      // still consume its pending gravity restore (a4 audit finding).
      Entity entity = event.getEntity();
      ControlManager.onEntityJoinLevel(entity);

      // Projectiles released while the owner is anchored never spawn — the
      // last deny layer for crossbow/trident throws past the item-use gates.
      if (entity instanceof Projectile projectile && projectile.getOwner() instanceof LivingEntity owner && HeroDamage.isAnchored(owner)) {
         event.setCanceled(true);
      }
   }

   /** The bound Evangelium never drops on death. */
   @SubscribeEvent
   public static void onLivingDrops(LivingDropsEvent event) {
      if (event.getEntity() instanceof Player) {
         event.getDrops().removeIf(drop -> drop.getItem().getItem() == ViltrumiteItems.EVANGELIUM.get());
      }
   }

   @SubscribeEvent
   public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         HeroRegistry.get(player).cleanup(player, CleanupReason.DISCONNECT);
      }
   }

   // Control-state baselines: a client sees the whole world snapshot on join,
   // respawn and dimension change; mutations re-broadcast from the tick.
   @SubscribeEvent
   public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         HeroControlSync.sendBaseline(player);
         ControlManager.restorePendingTarget(player);
      }
   }

   @SubscribeEvent
   public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         HeroControlSync.sendBaseline(player);
         ControlManager.restorePendingTarget(player);
         // The bound Evangelium returns on respawn when it is missing (spec 11.2).
         if (player instanceof HeroPlayer heroPlayer && heroPlayer.getHeroId() == HeroId.REGULUS) {
            Evangelium.grant(player);
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         HeroControlSync.sendBaseline(player);
         ControlManager.restorePendingTarget(player);
      }
   }

   /** Regulus never takes fall damage; the shockwave itself lives in the tick. */
   @SubscribeEvent
   public static void onLivingFall(LivingFallEvent event) {
      LivingEntity entity = event.getEntity();
      if (entity instanceof Player player && RegulusHero.stateOf(player) != null) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void onLevelTick(TickEvent.LevelTickEvent event) {
      if (event.phase == TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
         return;
      }

      ControlManager.get(level).tick(level);
   }
}
