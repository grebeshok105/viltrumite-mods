package dev.baranhan.viltrumitecore.ability;

import dev.baranhan.viltrumitecore.hero.HeroAction;
import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import dev.baranhan.viltrumiteflight.util.FlightState;
import dev.baranhan.viltrumiteflight.util.ViltrumiteFlightPlayer;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class ViltrumiteAbilities {
   public static final Map<String, ViltrumiteAbility> REGISTRY = new LinkedHashMap<>();

   public static void registerAll() {
      register(
         new ViltrumiteAbility(
            "viltrumite:block",
            new ResourceLocation("viltrumitecore", "textures/gui/ability/block.png"),
            "ability.viltrumitecore.block.name",
            "ability.viltrumitecore.block.desc",
            0,
            player -> {
               if (player instanceof ViltrumiteCorePlayer corePlayer
                  && (
                     corePlayer.isTryingToGrab()
                        || corePlayer.isDashing()
                        || corePlayer.getPunchTicks() > 0
                        || corePlayer.getChopTicks() > 0
                        || corePlayer.getThunderclapTicks() > 0
                        || corePlayer.getBarrageTicks() != 0
                        || corePlayer.getBlockCooldown() > 0
                  )) {
                  return true;
               }

               return false;
            }
         )
      );
      register(
         new ViltrumiteAbility(
            "viltrumite:grab",
            new ResourceLocation("viltrumitecore", "textures/gui/ability/grab.png"),
            "ability.viltrumitecore.grab.name",
            "ability.viltrumitecore.grab.desc",
            0,
            player -> {
               if (!(player instanceof ViltrumiteCorePlayer corePlayer)) {
                  return false;
               } else {
                  boolean isLeftPunching = corePlayer.getPunchTicks() > 0 && corePlayer.isLeftArmPunch();
                  boolean isLeftChopping = corePlayer.getChopTicks() > 0 && corePlayer.isLeftChop();
                  return corePlayer.isBlocking()
                     || isLeftPunching
                     || isLeftChopping
                     || corePlayer.getThunderclapTicks() > 0
                     || corePlayer.getBarrageTicks() != 0;
               }
            }
         )
      );
      register(
         new ViltrumiteAbility(
            "viltrumite:lock",
            new ResourceLocation("viltrumitecore", "textures/gui/ability/target_lock.png"),
            "ability.viltrumitecore.lock.name",
            "ability.viltrumitecore.lock.desc",
            0
         )
      );
      register(
         new ViltrumiteAbility(
            "viltrumite:chop",
            new ResourceLocation("viltrumitecore", "textures/gui/ability/chop.png"),
            "ability.viltrumitecore.chop.name",
            "ability.viltrumitecore.chop.desc",
            0,
            player -> {
               if (player instanceof ViltrumiteCorePlayer corePlayer
                  && (
                     corePlayer.getChopTicks() > 0
                        || corePlayer.getPunchTicks() > 0
                        || corePlayer.isBlocking()
                        || corePlayer.getThunderclapTicks() > 0
                        || corePlayer.getBarrageTicks() != 0
                  )) {
                  return true;
               }

               return false;
            }
         )
      );
      register(
         new ViltrumiteAbility(
            "viltrumite:punch",
            new ResourceLocation("viltrumitecore", "textures/gui/ability/punch.png"),
            "ability.viltrumitecore.punch.name",
            "ability.viltrumitecore.punch.desc",
            0,
            player -> {
               if (player instanceof ViltrumiteCorePlayer corePlayer
                  && (
                     corePlayer.getChopTicks() > 0
                        || corePlayer.getPunchTicks() > 0
                        || corePlayer.isBlocking()
                        || corePlayer.getThunderclapTicks() > 0
                        || corePlayer.getBarrageTicks() != 0
                        || corePlayer.getPunchCooldown() > 0
                  )) {
                  return true;
               }

               return false;
            }
         )
      );
      register(
         new ViltrumiteAbility(
            "viltrumite:thunderclap",
            new ResourceLocation("viltrumitecore", "textures/gui/ability/thunderclap.png"),
            "ability.viltrumitecore.thunderclap.name",
            "ability.viltrumitecore.thunderclap.desc",
            0,
            player -> {
               if (player instanceof ViltrumiteCorePlayer corePlayer
                  && (
                     corePlayer.getChopTicks() > 0
                        || corePlayer.getPunchTicks() > 0
                        || corePlayer.isBlocking()
                        || corePlayer.getThunderclapTicks() > 0
                        || corePlayer.getBarrageTicks() != 0
                  )) {
                  return true;
               }

               return false;
            }
         )
      );
      register(
         new ViltrumiteAbility(
            "viltrumite:barrage",
            new ResourceLocation("viltrumitecore", "textures/gui/ability/barrage.png"),
            "ability.viltrumitecore.barrage.name",
            "ability.viltrumitecore.barrage.desc",
            0,
            player -> {
               if (player instanceof ViltrumiteCorePlayer corePlayer
                  && (
                     corePlayer.isBlocking()
                        || corePlayer.isDashing()
                        || corePlayer.getChopTicks() > 0
                        || corePlayer.getPunchTicks() > 0
                        || corePlayer.getThunderclapTicks() > 0
                        || corePlayer.getBarrageTicks() < 0
                        || corePlayer.isTryingToGrab()
                        || corePlayer.getGrabbedTarget() != null
                        || corePlayer.getBarrageCooldown() > 0
                  )) {
                  return true;
               }

               return false;
            }
         )
      );
      register(
         new ViltrumiteAbility(
            "viltrumite:speed_lock",
            new ResourceLocation("viltrumitecore", "textures/gui/ability/speed_lock.png"),
            "ability.viltrumitecore.speed_lock.name",
            "ability.viltrumitecore.speed_lock.desc",
            0,
            player -> {
               if (player instanceof ViltrumiteFlightPlayer flightPlayer && flightPlayer.getFlightThrottle() <= 0.2F) {
                  return true;
               }

               return false;
            }
         )
      );
      register(
         new ViltrumiteAbility(
            "viltrumite:fast_takeoff",
            new ResourceLocation("viltrumitecore", "textures/gui/ability/takeoff.png"),
            "ability.viltrumitecore.fast_takeoff.name",
            "ability.viltrumitecore.fast_takeoff.desc",
            0
         )
      );
      register(
         new ViltrumiteAbility(
            "viltrumite:supersonic_flight",
            new ResourceLocation("viltrumitecore", "textures/gui/ability/supersonic.png"),
            "ability.viltrumitecore.supersonic_flight.name",
            "ability.viltrumitecore.supersonic_flight.desc",
            0,
            player -> {
               if (player instanceof ViltrumiteFlightPlayer flightPlayer && flightPlayer.getFlightState() == FlightState.NONE) {
                  return true;
               }

               return false;
            }
         )
      );
      register(
         new ViltrumiteAbility(
            "viltrumite:dash",
            new ResourceLocation("viltrumitecore", "textures/gui/ability/dash.png"),
            "ability.viltrumitecore.dash.name",
            "ability.viltrumitecore.dash.desc",
            1,
            player -> {
               if (!dev.baranhan.viltrumitecore.hero.LegacyKit.allows(player, dev.baranhan.viltrumitecore.hero.LegacyKit.DASH)) {
                  return true;
               }

               if (player instanceof ViltrumiteCorePlayer corePlayer
                  && (corePlayer.isDashing() || corePlayer.isBlocking() || corePlayer.getBarrageTicks() != 0)) {
                  return true;
               }

               return false;
            }
         )
      );
      register(
         new ViltrumiteAbility(
            "viltrumite:speed",
            new ResourceLocation("viltrumitecore", "textures/gui/ability/speed.png"),
            "ability.viltrumitecore.speed.name",
            "ability.viltrumitecore.speed.desc",
            0,
            player -> {
               if (player instanceof ViltrumiteFlightPlayer flightPlayer && flightPlayer.getFlightState() != FlightState.NONE) {
                  return true;
               }

               return false;
            }
         )
      );
      registerRegulusAbility("regulus:lions_heart", "lions_heart", 0, HeroAction.LIONS_HEART);
      registerRegulusAbility("regulus:debris_kick", "debris_kick", 1, HeroAction.DEBRIS_KICK);
      registerRegulusAbility("regulus:mania", "mania", 2, HeroAction.MANIA);
      registerRegulusAbility("regulus:greeds_embrace", "greeds_embrace", 3, HeroAction.GREEDS_EMBRACE);
      registerRegulusAbility("regulus:counter", "counter", 4, HeroAction.COUNTER);
      register(
         new ViltrumiteAbility(
            "regulus:punch",
            new ResourceLocation("viltrumitecore", "textures/gui/ability/regulus/punch.png"),
            "ability.viltrumitecore.regulus_punch.name",
            "ability.viltrumitecore.regulus_punch.desc",
            0,
            ViltrumiteAbilities::regulusPunchGrey
         )
      );
      registerHeroPanels();
   }

   private static void registerHeroPanels() {
      for (ViltrumiteAbility ability : dev.baranhan.viltrumitecore.hero.homelander.HomelanderAbilities.panelAbilities()) {
         register(ability);
      }
   }

   private static void registerRegulusAbility(String id, String name, int cooldownIndex, HeroAction action) {
      register(
         new ViltrumiteAbility(
            id,
            new ResourceLocation("viltrumitecore", "textures/gui/ability/regulus/" + name + ".png"),
            "ability.viltrumitecore." + name + ".name",
            "ability.viltrumitecore." + name + ".desc",
            0,
            player -> regulusSlotGrey(player, cooldownIndex, action)
         )
      );
   }

   /**
    * Panel grey-out mirrors RegulusHero.actionPermitted for the five ability
    * slots: while Lion's Heart runs only the heart toggle and the ritual stay
    * lit, a busy cast, an open Mania channel or a running ritual greys every
    * slot, Counter exists only inside madness and the ritual cannot re-enter
    * mid-madness. Resolved effects — the freeze, a standing dome and madness
    * itself — occupy no slot (spec 1.2.5/13.2).
    */
   private static boolean regulusSlotGrey(Player player, int cooldownIndex, HeroAction action) {
      if (!(player instanceof dev.baranhan.viltrumitecore.hero.HeroPlayer heroPlayer)) {
         return false;
      }

      dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot snapshot = heroPlayer.getHeroSnapshot();
      if (snapshot == null || snapshot.heroId() != dev.baranhan.viltrumitecore.hero.HeroId.REGULUS) {
         return false;
      }

      int[] cooldowns = snapshot.cooldowns();
      if (cooldownIndex >= 0 && cooldownIndex < cooldowns.length && cooldowns[cooldownIndex] > 0) {
         return true;
      }

      if (snapshot.lionActive()) {
         return action != HeroAction.LIONS_HEART && action != HeroAction.RITUAL;
      }

      if (snapshot.actionBusy() || snapshot.controlTargetId() >= 0 || snapshot.ritualTicks() >= 0) {
         return true;
      }

      if (action == HeroAction.COUNTER) {
         return !snapshot.madness();
      }

      return action == HeroAction.RITUAL && snapshot.madness();
   }

   /** The Regulus fist greys like a free cast, plus the shared punch timers. */
   private static boolean regulusPunchGrey(Player player) {
      if (player instanceof ViltrumiteCorePlayer corePlayer && (corePlayer.getPunchTicks() > 0 || corePlayer.getPunchCooldown() > 0)) {
         return true;
      }

      return regulusSlotGrey(player, -1, HeroAction.DEBRIS_KICK);
   }

   private static void register(ViltrumiteAbility ability) {
      REGISTRY.put(ability.getId(), ability);
   }

   public static ViltrumiteAbility get(String id) {
      return REGISTRY.get(id);
   }
}
