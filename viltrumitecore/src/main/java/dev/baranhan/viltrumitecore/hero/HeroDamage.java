package dev.baranhan.viltrumitecore.hero;

import dev.baranhan.viltrumitecore.hero.control.ControlManager;
import dev.baranhan.viltrumitecore.hero.regulus.RegulusHero;
import dev.baranhan.viltrumitecore.hero.regulus.RegulusState;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * One shared hero damage path. Interceptors at Player/LivingEntity hurt route
 * every external hit here; internal hero damage bypasses vanilla hurt entirely.
 * Ordering: capture attacker -> queue under control -> lion block -> pass.
 */
public final class HeroDamage {
   private static final ThreadLocal<Set<UUID>> ROUTING = ThreadLocal.withInitial(HashSet::new);

   private HeroDamage() {
   }

   public enum DamageKind {
      EXTERNAL,
      INTERNAL,
      DEFERRED_RELEASE
   }

   public enum DamageResult {
      PASS,
      BLOCKED,
      QUEUED,
      APPLIED
   }

   /** Re-entry guard: nested hurt() calls inside routing skip interception. */
   public static boolean isRouting(Entity entity) {
      return entity != null && ROUTING.get().contains(entity.getUUID());
   }

   public static DamageResult route(LivingEntity target, DamageSource source, float amount, DamageKind kind) {
      if (target.level().isClientSide() || !target.isAlive() || amount <= 0.0F) {
         return DamageResult.PASS;
      }

      Set<UUID> routing = ROUTING.get();
      if (!routing.add(target.getUUID())) {
         return DamageResult.PASS;
      }

      try {
         // Record a real living attacker once, including hits that get blocked.
         if (kind == DamageKind.EXTERNAL && source.getEntity() instanceof LivingEntity attacker && target instanceof ServerPlayer player) {
            RegulusState state = RegulusHero.stateOf(player);
            if (state != null) {
               state.attackerId = attacker.getUUID();
               state.attackerTick = target.level().getGameTime();
               state.attackerLastPos = attacker.position();
            }
         }

         if (target.level() instanceof ServerLevel serverLevel) {
            ControlManager manager = ControlManager.get(serverLevel);
            if (kind != DamageKind.DEFERRED_RELEASE && manager.isAnchored(target)) {
               manager.queueDamage(target, source, payableAmount(target, source, amount));
               return DamageResult.QUEUED;
            }
         }

         // Lion's Heart blocks every external hit outright (it is not Resistance).
         if (kind == DamageKind.EXTERNAL && target instanceof ServerPlayer player) {
            RegulusState state = RegulusHero.stateOf(player);
            if (state != null && state.lionActive) {
               return DamageResult.BLOCKED;
            }
         }

         return DamageResult.PASS;
      } finally {
         routing.remove(target.getUUID());
      }
   }

   /**
    * Payable amount for queueing: evaluate mitigation once without HP loss.
    * Viltrumite-hero victims reuse their legacy reduction/threshold values.
    */
   private static float payableAmount(LivingEntity target, DamageSource source, float amount) {
      if (!source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)) {
         amount = net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(
            amount, (float)target.getArmorValue(), (float)target.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS)
         );
      }

      if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_EFFECTS)) {
         return amount;
      }

      net.minecraft.world.effect.MobEffectInstance resistance = target.getEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE);
      if (resistance != null && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_RESISTANCE)) {
         int reduction = (resistance.getAmplifier() + 1) * 5;
         amount = Math.max(amount * (float)(25 - reduction) / 25.0F, 0.0F);
      }

      if (amount > 0.0F && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ENCHANTMENTS)) {
         int protection = net.minecraft.world.item.enchantment.EnchantmentHelper.getDamageProtection(target.getArmorSlots(), source);
         if (protection > 0) {
            amount = net.minecraft.world.damagesource.CombatRules.getDamageAfterMagicAbsorb(amount, (float)protection);
         }
      }

      return amount;
   }

   /**
    * Clean HP loss for hero-internal damage: ignores armor/Resistance/Lion but
    * still queues under anchor control and still runs the lethal boundary.
    */
   public static void applyInternal(ServerPlayer player, float amount) {
      if (player.level().isClientSide() || amount <= 0.0F) {
         return;
      }

      if (player.level() instanceof ServerLevel serverLevel && ControlManager.get(serverLevel).isAnchored(player)) {
         ControlManager.get(serverLevel).queueDamage(player, player.damageSources().generic(), amount);
         return;
      }

      applyCleanDamage(player, player.damageSources().generic(), amount);
   }

   /**
    * Apply damage directly (deferred release + internal causes). The lethal
    * boundary (hero totem) is handled here for every path uniformly.
    */
   public static void applyCleanDamage(LivingEntity target, DamageSource source, float amount) {
      if (amount <= 0.0F || target.level().isClientSide()) {
         return;
      }

      float newHealth = target.getHealth() - amount;
      if (newHealth <= 0.0F) {
         if (target instanceof ServerPlayer player && tryHeroTotem(player, source)) {
            return;
         }

         target.setHealth(0.0F);
         target.die(source);
         return;
      }

      target.setHealth(newHealth);
      target.hurtTime = target.hurtDuration = 10;
   }

   /**
    * Common lethal boundary for any hero fatal path (vanilla hurt, clean
    * damage, blood price). Consumes the one-per-session hero totem exactly once.
    */
   public static boolean tryHeroTotem(ServerPlayer player, DamageSource source) {
      if (!(player instanceof HeroPlayer heroPlayer) || heroPlayer.getHeroId() != HeroId.REGULUS) {
         return false;
      }

      if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         return false;
      }

      HeroSession session = heroPlayer.getHeroSession();
      if (session.totemConsumed()) {
         return false;
      }

      // Mark consumed before restoring health so a nested lethal hit cannot
      // double-dip the revival.
      heroPlayer.viltrumitecore$setHeroSession(session.consumeTotem());
      player.setHealth(1.0F);
      player.removeAllEffects();
      player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
      player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
      player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
      player.level().broadcastEntityEvent(player, (byte)35);
      return true;
   }

   /** Queue the hit under any anchor control; used by routed EXTERNAL damage. */
   public static boolean isAnchored(@Nullable LivingEntity target) {
      if (target == null || !(target.level() instanceof ServerLevel level)) {
         return false;
      }

      return ControlManager.get(level).isAnchored(target);
   }
}
