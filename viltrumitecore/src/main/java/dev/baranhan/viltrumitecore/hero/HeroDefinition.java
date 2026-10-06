package dev.baranhan.viltrumitecore.hero;

import dev.baranhan.viltrumitecore.hero.control.ControlKind;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * One hero's shared contract. Common code asks this interface, never a concrete
 * character: no hero branches belong in the legacy mixins.
 */
public interface HeroDefinition {
   HeroId id();

   /** Whether this hero may use mod flight at all. Vanilla creative/spectator flight is unaffected. */
   boolean allowsFlight(Player player);

   /** Whether this hero may use the legacy viltrumite ability kit and stats. */
   boolean allowsLegacyAbilities(Player player);

   /** Whether the legacy three-page ability bar swap is meaningful for this hero. */
   default boolean allowsAbilityPages(Player player) {
      return this.allowsLegacyAbilities(player);
   }

   /** Whether the ability panel/menu renders for this hero. */
   default boolean hasAbilityPanel(Player player) {
      return this.id() != HeroId.HUMAN;
   }

   /** Which registry ability ids this hero owns (and may equip). */
   boolean ownsAbility(String abilityId);

   /** Default slot contents applied on hero enter. Length 18, "" for empty. */
   String[] defaultLoadout();

   /** May this target be placed under the given control kind right now. */
   boolean allowsExternalControl(LivingEntity target, ControlKind kind);

   /** Multiplier on this hero's ordinary melee attack damage (heart bonus). */
   default float meleeDamageFactor(Player player) {
      return 1.0F;
   }

   /** Whether food exhaustion is currently suspended for this hero (Lion). */
   default boolean preventsExhaustion(Player player) {
      return false;
   }

   /** May the hero start or continue this action right now. */
   boolean canAct(ServerPlayer player, HeroAction action);

   void tick(ServerPlayer player);

   void handleInput(ServerPlayer player, HeroAction action, boolean pressed);

   /**
    * Enter the hero after an actual id change (not on restore/clone): default
    * loadout, grants, baseline snapshot. Never called when the id is unchanged.
    */
   void enter(ServerPlayer player);

   /** Server-authoritative public snapshot; written to the synced data for all trackers. */
   HeroPublicSnapshot snapshot(Player player);

   /**
    * Hero-private data saved with the entity's HeroData on logout (cooldowns,
    * charges — duration bookkeeping only). Default: nothing to save.
    */
   default void saveHeroState(Player player, CompoundTag nbt) {
   }

   /**
    * Load counterpart of saveHeroState on login. The default drops the
    * transient hero state object, matching the previous rebuild-empty path.
    */
   default void loadHeroState(Player player, CompoundTag nbt) {
      if (player instanceof HeroPlayer heroPlayer) {
         heroPlayer.viltrumitecore$setHeroState(null);
      }
   }

   /** On respawn the clone receives whatever the hero wants carried. Default: nothing. */
   default void cloneHeroState(Player original, Player clone) {
   }

   /**
    * Idempotent cleanup that precedes transient-state discard. Reasons
    * distinguish death, disconnect and an explicit hero change.
    */
   void cleanup(ServerPlayer player, CleanupReason reason);
}
