package dev.baranhan.viltrumitecore.hero;

import dev.baranhan.viltrumitecore.hero.regulus.RegulusHero;
import dev.baranhan.viltrumitecore.hero.regulus.RegulusState;
import dev.baranhan.viltrumitecore.hero.control.ControlManager;
import dev.baranhan.viltrumitecore.util.ViltrumiteAbilityUser;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
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
      if (entity.level() instanceof ServerLevel level) {
         for (ServerPlayer player : level.players()) {
            RegulusState state = RegulusHero.stateOf(player);
            if (state != null && state.carriers.contains(entity.getUUID())) {
               dev.baranhan.viltrumitecore.hero.regulus.RegulusHearts.onCarrierGone(player, state, entity.getUUID());
            }
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         HeroRegistry.get(player).cleanup(player, CleanupReason.DISCONNECT);
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
