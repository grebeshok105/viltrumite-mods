package dev.baranhan.viltrumitecore.hero;

import dev.baranhan.viltrumitecore.hero.regulus.RegulusHero;
import dev.baranhan.viltrumitecore.util.ViltrumiteAbilityUser;
import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import dev.baranhan.viltrumiteflight.util.FlightPermissions;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class HeroRegistry {
   private static final Map<HeroId, HeroDefinition> DEFINITIONS = new EnumMap<>(HeroId.class);

   private HeroRegistry() {
   }

   public static void registerDefaults() {
      register(new HumanHero());
      register(new ViltrumiteHero());
      register(new RegulusHero());
   }

   public static void register(HeroDefinition definition) {
      DEFINITIONS.put(definition.id(), definition);
   }

   public static HeroDefinition get(HeroId id) {
      HeroDefinition definition = DEFINITIONS.get(id);
      return definition == null ? DEFINITIONS.get(HeroId.HUMAN) : definition;
   }

   public static HeroDefinition get(Player player) {
      if (player instanceof HeroPlayer heroPlayer) {
         return get(heroPlayer.getHeroId());
      }

      return get(HeroId.HUMAN);
   }

   /**
    * Victim-side control policy for shared push/grab/freeze paths: non-hero
    * targets always allow it; hero targets defer to their definition (an
    * active Lion's Heart denies every kind, including IMPULSE).
    */
   public static boolean allowsExternalControl(net.minecraft.world.entity.LivingEntity target, dev.baranhan.viltrumitecore.hero.control.ControlKind kind) {
      return !(target instanceof Player player) || get(player).allowsExternalControl(target, kind);
   }

   /**
    * Explicit hero transition: full lifecycle. No-op when the id is unchanged,
    * so selecting the current hero is never an exit/re-entry exploit.
    */
   public static void changeHero(ServerPlayer player, HeroId id) {
      if (!(player instanceof HeroPlayer heroPlayer)) {
         return;
      }

      HeroId current = heroPlayer.getHeroId();
      if (current == id) {
         return;
      }

      get(current).cleanup(player, CleanupReason.HERO_CHANGE);
      heroPlayer.viltrumitecore$setHeroState(null);

      if (player instanceof ViltrumiteAbilityUser abilityUser) {
         String[] loadout = get(id).defaultLoadout();

         for (int slot = 0; slot < 18; slot++) {
            abilityUser.setAbilityInSlot(slot, slot < loadout.length ? loadout[slot] : "");
         }
      }

      heroPlayer.viltrumitecore$setHeroSession(new HeroSession(id, UUID.randomUUID(), false));

      if (player instanceof ViltrumiteCorePlayer corePlayer) {
         corePlayer.setChosenRace(true);
      }

      FlightPermissions.resetModFlight(player);
      get(id).enter(player);
      syncSnapshot(player);
   }

   /**
    * NBT load / clone path: restores identity without a lifecycle, never enters
    * a new session, and clears mod flight leftovers.
    */
   public static void restoreHero(ServerPlayer player, HeroSession session) {
      if (!(player instanceof HeroPlayer heroPlayer)) {
         return;
      }

      HeroId previous = heroPlayer.getHeroId();
      heroPlayer.viltrumitecore$setHeroSession(session);

      if (previous != session.heroId()) {
         heroPlayer.viltrumitecore$setHeroState(null);
      }

      FlightPermissions.resetModFlight(player);
   }

   /** Compose and publish the public snapshot when it changed. */
   public static void syncSnapshot(ServerPlayer player) {
      if (!(player instanceof HeroPlayer heroPlayer)) {
         return;
      }

      HeroPublicSnapshot snapshot = get(player).snapshot(player);
      heroPlayer.viltrumitecore$setSyncedSnapshot(snapshot);
   }

   /** Owner-private snapshot push; only the owner sees carrier ids etc. */
   public static void pushOwnerSnapshot(ServerPlayer player, HeroOwnerSnapshot snapshot) {
      if (!snapshot.equals(((HeroPlayer)player).viltrumitecore$getOwnerSnapshot())) {
         ((HeroPlayer)player).viltrumitecore$setOwnerSnapshot(snapshot);
         dev.baranhan.viltrumitecore.network.CoreMessages.sendToPlayer(
            new dev.baranhan.viltrumitecore.network.packet.HeroOwnerSnapshotS2CPacket(snapshot.carrierEntityIds()), player
         );
      }
   }
}
