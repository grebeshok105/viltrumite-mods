package dev.baranhan.viltrumitecore.hero.ironman;

import dev.baranhan.viltrumitecore.hero.CleanupReason;
import dev.baranhan.viltrumitecore.hero.HeroAction;
import dev.baranhan.viltrumitecore.hero.HeroDefinition;
import dev.baranhan.viltrumitecore.hero.HeroId;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import dev.baranhan.viltrumitecore.hero.control.ControlKind;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Tony Stark (spec docs/design/2026-10-09-ironman-design.md). No Viltrumite
 * kit: everything comes from the suit.
 */
public class IronManHero implements HeroDefinition {

   @Override
   public HeroId id() {
      return HeroId.IRON_MAN;
   }

   /** Mod flight only while a suit is worn (Task 5 wires the suit). */
   @Override
   public boolean allowsFlight(Player player) {
      return false;
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

   @Override
   public boolean canAct(ServerPlayer player, HeroAction action) {
      return false;
   }

   @Override
   public void tick(ServerPlayer player) {
   }

   @Override
   public void handleInput(ServerPlayer player, HeroAction action, boolean pressed) {
   }

   @Override
   public void enter(ServerPlayer player) {
   }

   @Override
   public HeroPublicSnapshot snapshot(Player player) {
      return new HeroPublicSnapshot(HeroId.IRON_MAN, -1, 0, 0, 0, false, 0, 0, false, false, 0, 0, -1,
         new int[HeroPublicSnapshot.COOLDOWN_COUNT], false);
   }

   @Override
   public void cleanup(ServerPlayer player, CleanupReason reason) {
   }
}
