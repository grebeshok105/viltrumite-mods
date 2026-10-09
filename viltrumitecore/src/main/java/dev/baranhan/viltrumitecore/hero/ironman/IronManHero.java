package dev.baranhan.viltrumitecore.hero.ironman;

import dev.baranhan.viltrumitecore.hero.CleanupReason;
import dev.baranhan.viltrumitecore.hero.HeroAction;
import dev.baranhan.viltrumitecore.hero.HeroDamage;
import dev.baranhan.viltrumitecore.hero.HeroDefinition;
import dev.baranhan.viltrumitecore.hero.HeroId;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import dev.baranhan.viltrumitecore.hero.control.ControlKind;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Tony Stark (spec docs/design/2026-10-09-ironman-design.md). No Viltrumite
 * kit: everything comes from the suit.
 */
public class IronManHero implements HeroDefinition {
   private static final UUID ARMOR_ID = UUID.fromString("7c3e9a51-2f6d-4b8e-a1c4-3d5f6e7a8b01");
   private static final UUID TOUGHNESS_ID = UUID.fromString("7c3e9a51-2f6d-4b8e-a1c4-3d5f6e7a8b02");
   private static final UUID KNOCKBACK_ID = UUID.fromString("7c3e9a51-2f6d-4b8e-a1c4-3d5f6e7a8b03");

   @Override
   public HeroId id() {
      return HeroId.IRON_MAN;
   }

   /**
    * Mod flight only while the suit is fully worn. Read on BOTH sides (the
    * flight mixin and input check the local player): the client reads the
    * synced snapshot flag, the server its own state.
    */
   @Override
   public boolean allowsFlight(Player player) {
      if (player != null && player.level().isClientSide()) {
         return suitWornFlag(player);
      }

      IronManState state = IronManState.of(player);
      return state != null && state.wantsFlight();
   }

   @Override
   public boolean grantsFlightAbility(Player player) {
      IronManState state = IronManState.of(player);
      return state != null && state.wantsFlight();
   }

   /** Synced SUIT_WORN bit of an Iron Man snapshot (any side). */
   static boolean suitWornFlag(Player player) {
      if (!(player instanceof dev.baranhan.viltrumitecore.hero.HeroPlayer heroPlayer)) {
         return false;
      }

      HeroPublicSnapshot snapshot = heroPlayer.getHeroSnapshot();
      return snapshot.heroId() == HeroId.IRON_MAN && IronManFlags.is(snapshot.heroFlags(), IronManFlags.Field.SUIT_WORN);
   }

   @Override
   public boolean allowsLegacyAbilities(Player player) {
      return false;
   }

   @Override
   public boolean allowsLegacyAbility(Player player, String abilityId) {
      return false;
   }

   @Override
   public boolean hasAbilityPanel(Player player) {
      return true;
   }

   @Override
   public boolean allowsAbilityPages(Player player) {
      return true;
   }

   /** No fall damage while armored, including the retract wave (spec §8.6). */
   @Override
   public boolean cancelsFallDamage(Player player) {
      IronManState state = IronManState.of(player);
      return state != null && state.suit.armored();
   }

   @Override
   public boolean ownsAbility(String abilityId) {
      return IronManAbilities.owns(abilityId);
   }

   @Override
   public java.util.List<dev.baranhan.viltrumitecore.ability.ViltrumiteAbility> panelAbilities() {
      return IronManAbilities.panelAbilities();
   }

   @Override
   public net.minecraft.resources.ResourceLocation abilityIcon(String abilityId) {
      return IronManAbilities.icon(abilityId);
   }

   @Override
   public String[] heroInputSlots() {
      return IronManAbilities.slotIds();
   }

   @Override
   public HeroAction heroActionFor(String abilityId) {
      return IronManAbilities.actionFor(abilityId);
   }

   @Override
   public String[] defaultLoadout() {
      return IronManAbilities.defaultLoadout();
   }

   @Override
   public boolean allowsExternalControl(LivingEntity target, ControlKind kind) {
      return true;
   }

   /** Control (spec §16): nothing starts while anchored or dead. */
   static boolean controlled(ServerPlayer player) {
      return HeroDamage.isAnchored(player) || !player.isAlive();
   }

   @Override
   public boolean canAct(ServerPlayer player, HeroAction action) {
      if (controlled(player)) {
         return false;
      }

      return action == HeroAction.SUIT;
   }

   @Override
   public void tick(ServerPlayer player) {
      IronManState state = IronManState.ensure(player);
      if (controlled(player)) {
         state.suit.interrupt();
      }

      state.suit.tick();
      state.energy.tick();
      syncArmor(player, state.suit.armored());
   }

   @Override
   public void handleInput(ServerPlayer player, HeroAction action, boolean pressed) {
      if (action == HeroAction.SUIT && pressed && this.canAct(player, action)) {
         IronManState.ensure(player).suit.toggle();
      }
   }

   @Override
   public void enter(ServerPlayer player) {
      IronManState.ensure(player);
   }

   @Override
   public HeroPublicSnapshot snapshot(Player player) {
      IronManState state = IronManState.of(player);
      return snapshotOf(state == null ? new IronManState() : state);
   }

   /** Pure snapshot: flags, wave timeline, energy x10 and the weapon lock. */
   static HeroPublicSnapshot snapshotOf(IronManState state) {
      Suit suit = state.suit;
      int flags = 0;
      flags = IronManFlags.set(flags, IronManFlags.Field.SUIT_WORN, suit.worn());
      flags = IronManFlags.set(flags, IronManFlags.Field.DEPLOYING, suit.state() == SuitState.DEPLOYING);
      flags = IronManFlags.set(flags, IronManFlags.Field.RETRACTING, suit.state() == SuitState.RETRACTING);
      flags = IronManFlags.set(flags, IronManFlags.Field.GLIDE, state.glide);
      boolean wave = suit.transitioning();
      return new HeroPublicSnapshot(HeroId.IRON_MAN, wave ? HeroAction.SUIT.ordinal() : -1, wave ? suit.ticks() : 0, wave ? suit.waveLength() : 0,
         0, false, 0, 0, false, false, 0, 0, -1, new int[HeroPublicSnapshot.COOLDOWN_COUNT], false,
         -1, null, Math.round(state.energy.value() * 10.0F), state.energy.weaponsLocked(), flags);
   }

   @Override
   public void saveHeroState(Player player, CompoundTag nbt) {
      IronManState state = IronManState.of(player);
      if (state != null) {
         state.save(nbt);
      }
   }

   @Override
   public void loadHeroState(Player player, CompoundTag nbt) {
      IronManState.ensure(player).load(nbt);
   }

   @Override
   public void cloneHeroState(Player original, Player clone) {
      IronManState previous = IronManState.of(original);
      if (previous != null && clone instanceof dev.baranhan.viltrumitecore.hero.HeroPlayer heroPlayer) {
         heroPlayer.viltrumitecore$setHeroState(IronManState.cloneForRespawn(previous));
      }
   }

   @Override
   public void cleanup(ServerPlayer player, CleanupReason reason) {
      IronManState state = IronManState.of(player);
      if (state != null) {
         state.onCleanup(reason);
      }

      if (reason != CleanupReason.DISCONNECT) {
         syncArmor(player, false);
      }
   }

   /** Spec §16: the suit on the player stays through a dimension change. */
   @Override
   public void onDimensionChange(ServerPlayer player) {
   }

   /** Nano armor attributes with fixed ids: present exactly while armored. */
   static void syncArmor(ServerPlayer player, boolean armored) {
      modifier(player, Attributes.ARMOR, ARMOR_ID, "Iron Man nano armor", IronManRules.NANO_ARMOR, armored);
      modifier(player, Attributes.ARMOR_TOUGHNESS, TOUGHNESS_ID, "Iron Man nano toughness", IronManRules.NANO_TOUGHNESS, armored);
      modifier(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, "Iron Man nano stability", IronManRules.NANO_KNOCKBACK_RES, armored);
   }

   private static void modifier(ServerPlayer player, Attribute attribute, UUID id, String name, double amount, boolean present) {
      AttributeInstance instance = player.getAttribute(attribute);
      if (instance == null) {
         return;
      }

      boolean has = instance.getModifier(id) != null;
      if (present && !has) {
         instance.addPermanentModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.ADDITION));
      } else if (!present && has) {
         instance.removeModifier(id);
      }
   }
}
