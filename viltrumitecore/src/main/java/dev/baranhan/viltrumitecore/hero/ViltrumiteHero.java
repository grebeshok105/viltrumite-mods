package dev.baranhan.viltrumitecore.hero;

import dev.baranhan.viltrumitecore.hero.control.ControlKind;
import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * The viltrumite hero delegates to the legacy kit. It owns the "viltrumite:*"
 * ability ids and, on exit, resets every legacy action flag/timer and the owned
 * strength modifier so a restored stale state cannot keep running.
 */
public class ViltrumiteHero implements HeroDefinition {
   private static final UUID BASE_STRENGTH_MODIFIER_ID = UUID.fromString("e7208d13-6453-4cae-908c-9c3f508a6b12");

   @Override
   public HeroId id() {
      return HeroId.VILTRUMITE;
   }

   @Override
   public boolean allowsFlight(Player player) {
      return true;
   }

   @Override
   public boolean allowsLegacyAbilities(Player player) {
      return true;
   }

   @Override
   public boolean ownsAbility(String abilityId) {
      return abilityId != null && abilityId.startsWith("viltrumite:");
   }

   @Override
   public String[] defaultLoadout() {
      return new String[]{"viltrumite:punch", "viltrumite:dash", "viltrumite:grab", "viltrumite:lock", "viltrumite:block", "viltrumite:speed"};
   }

   @Override
   public boolean allowsExternalControl(LivingEntity target, ControlKind kind) {
      return true;
   }

   @Override
   public boolean canAct(ServerPlayer player, HeroAction action) {
      return true;
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
   public void cleanup(ServerPlayer player, CleanupReason reason) {
      if (reason != CleanupReason.HERO_CHANGE || !(player instanceof ViltrumiteCorePlayer corePlayer)) {
         return;
      }

      corePlayer.setPunchTicks(0);
      corePlayer.setPunchCooldown(0);
      corePlayer.setChopTicks(0);
      corePlayer.setThunderclapTicks(0);
      corePlayer.setBlocking(false);
      corePlayer.setTryingToGrab(false);
      corePlayer.releaseTarget();
      corePlayer.setSuperSpeed(false);
      corePlayer.setBarraging(false);
      corePlayer.setBarrageTicks(0);
      corePlayer.setBarrageCooldown(0);
      corePlayer.setBlockCooldown(0);
      corePlayer.setDashing(false);

      AttributeInstance damageAttribute = player.getAttribute(Attributes.ATTACK_DAMAGE);
      if (damageAttribute != null && damageAttribute.getModifier(BASE_STRENGTH_MODIFIER_ID) != null) {
         damageAttribute.removeModifier(BASE_STRENGTH_MODIFIER_ID);
      }
   }

   @Override
   public HeroPublicSnapshot snapshot(Player player) {
      return HeroPublicSnapshot.EMPTY;
   }
}
