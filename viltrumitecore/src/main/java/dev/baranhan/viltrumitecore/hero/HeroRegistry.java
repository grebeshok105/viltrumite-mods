package dev.baranhan.viltrumitecore.hero;

import dev.baranhan.viltrumitecore.hero.control.ControlManager;
import dev.baranhan.viltrumitecore.hero.regulus.RegulusHero;
import dev.baranhan.viltrumitecore.util.ViltrumiteAbilityUser;
import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import dev.baranhan.viltrumiteflight.util.FlightPermissions;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
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
      register(new dev.baranhan.viltrumitecore.hero.homelander.HomelanderHero());
      installFlightPolicy();
   }

   /**
    * Core adapter for mod flight (plan flight-policy contract): the hero's own
    * capability AND vanilla mayfly AND no world-control deny — an anchored or
    * dome-captured player loses mod flight until the control releases.
    */
   public static void installFlightPolicy() {
      FlightPermissions.setPolicy(player ->
         get(player).allowsFlight(player)
            && player.getAbilities().mayfly
            && (!(player.level() instanceof ServerLevel level) || !ControlManager.get(level).preventsFlight(player))
      );
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
      if (!isRealTransition(current, id)) {
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

      if (shouldResetState(previous, session.heroId())) {
         heroPlayer.viltrumitecore$setHeroState(null);
      }

      FlightPermissions.resetModFlight(player);
   }

   /** Selecting the hero already worn is never an exit/re-entry exploit. */
   public static boolean isRealTransition(HeroId current, HeroId next) {
      return current != next;
   }

   /** Restore keeps the loaded state only when the identity did not change. */
   public static boolean shouldResetState(HeroId previous, HeroId restored) {
      return previous != restored;
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

   /**
    * Repair the slots after login / a repeated choice: a slot holding an ability
    * the current hero does not own (e.g. a migrated Viltrumite save) resets the
    * whole loadout to the hero's default.
    */
   public static void repairLoadout(ServerPlayer player) {
      if (!(player instanceof ViltrumiteAbilityUser abilityUser)) {
         return;
      }

      HeroDefinition hero = get(player);
      String[] slots = new String[18];
      for (int slot = 0; slot < 18; slot++) {
         slots[slot] = abilityUser.getAbilityInSlot(slot);
      }

      if (!needsLoadoutReset(slots, hero::ownsAbility)) {
         return;
      }

      String[] loadout = hero.defaultLoadout();
      for (int slot = 0; slot < 18; slot++) {
         String id = slot < loadout.length ? loadout[slot] : null;
         abilityUser.setAbilityInSlot(slot, id == null ? "" : id);
      }
   }

   /** True when any non-empty slot holds an ability the hero does not own. */
   public static boolean needsLoadoutReset(String[] slots, java.util.function.Predicate<String> owns) {
      for (String id : slots) {
         if (id != null && !id.isEmpty() && !owns.test(id)) {
            return true;
         }
      }

      return false;
   }
}
