package dev.baranhan.viltrumitecore.hero.regulus;

import java.util.UUID;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;

/**
 * Regulus base-passive chassis: +70 armor (capped by vanilla's effective 30),
 * 100% knockback resistance, ambient Regen I / Speed II / Strength II /
 * Jump II / Fire Resistance. Applies and removes ONLY its own modifiers and
 * ambient effect instances.
 */
public final class RegulusPassives {
   private static final UUID ARMOR_MODIFIER_ID = UUID.fromString("a9f38f5d-1908-4b6e-8e4e-4e93f0d0a001");
   private static final UUID KB_MODIFIER_ID = UUID.fromString("a9f38f5d-1908-4b6e-8e4e-4e93f0d0a002");
   private static final UUID MADNESS_ARMOR_MODIFIER_ID = UUID.fromString("a9f38f5d-1908-4b6e-8e4e-4e93f0d0a003");
   private static final UUID MADNESS_HEALTH_MODIFIER_ID = UUID.fromString("a9f38f5d-1908-4b6e-8e4e-4e93f0d0a004");
   private static final UUID MADNESS_ATTACK_MODIFIER_ID = UUID.fromString("a9f38f5d-1908-4b6e-8e4e-4e93f0d0a005");

   private static final MobEffect[] AMBIENT = {
      MobEffects.REGENERATION, MobEffects.MOVEMENT_SPEED, MobEffects.DAMAGE_BOOST, MobEffects.JUMP, MobEffects.FIRE_RESISTANCE
   };
   private static final int[] AMBIENT_AMPLIFIER = {0, 1, 1, 1, 0};

   // Spec 11.2 madness set: Speed III / Strength III / Jump III / Regen I /
   // Resistance I, granted only for the madness duration.
   private static final MobEffect[] MADNESS_EFFECTS = {
      MobEffects.MOVEMENT_SPEED, MobEffects.DAMAGE_BOOST, MobEffects.JUMP, MobEffects.REGENERATION, MobEffects.DAMAGE_RESISTANCE
   };
   private static final int[] MADNESS_AMPLIFIER = {2, 2, 2, 0, 0};

   private RegulusPassives() {
   }

   public static void apply(Player player) {
      addModifier(player, Attributes.ARMOR, ARMOR_MODIFIER_ID, "Regulus armor", 70.0, Operation.ADDITION);
      addModifier(player, Attributes.KNOCKBACK_RESISTANCE, KB_MODIFIER_ID, "Regulus knockback resistance", 1.0, Operation.ADDITION);
      refreshAmbient(player);
   }

   public static void refreshAmbient(Player player) {
      for (int i = 0; i < AMBIENT.length; i++) {
         MobEffectInstance current = player.getEffect(AMBIENT[i]);
         if (current == null || (current.isAmbient() && current.getDuration() < 100)) {
            player.addEffect(new MobEffectInstance(AMBIENT[i], 2100, AMBIENT_AMPLIFIER[i], true, false, true));
         }
      }
   }

   public static void applyMadness(Player player) {
      addModifier(player, Attributes.ARMOR, MADNESS_ARMOR_MODIFIER_ID, "Regulus madness armor", 10.0, Operation.ADDITION);
      addModifier(player, Attributes.MAX_HEALTH, MADNESS_HEALTH_MODIFIER_ID, "Regulus madness vitality", 0.2, Operation.MULTIPLY_TOTAL);
      // Flat +0.4 attack (spec 11.2, Codex canon) — not a multiplier.
      addModifier(player, Attributes.ATTACK_DAMAGE, MADNESS_ATTACK_MODIFIER_ID, "Regulus madness strength", 0.4, Operation.ADDITION);
      for (int i = 0; i < MADNESS_EFFECTS.length; i++) {
         player.addEffect(new MobEffectInstance(MADNESS_EFFECTS[i], 2100, MADNESS_AMPLIFIER[i], true, false, true));
      }
   }

   public static void removeMadness(Player player) {
      removeModifier(player, Attributes.ARMOR, MADNESS_ARMOR_MODIFIER_ID);
      removeModifier(player, Attributes.MAX_HEALTH, MADNESS_HEALTH_MODIFIER_ID);
      removeModifier(player, Attributes.ATTACK_DAMAGE, MADNESS_ATTACK_MODIFIER_ID);

      // The madness effect instances end with the bonuses — ambient-only so an
      // externally applied effect is never stripped. The base ambient set is
      // re-applied by refreshAmbient on the next tick.
      for (MobEffect effect : MADNESS_EFFECTS) {
         MobEffectInstance instance = player.getEffect(effect);
         if (instance != null && instance.isAmbient()) {
            player.removeEffect(effect);
         }
      }
   }

   /** Remove only this hero's own modifiers and ambient effect instances. */
   public static void remove(Player player) {
      removeModifier(player, Attributes.ARMOR, ARMOR_MODIFIER_ID);
      removeModifier(player, Attributes.KNOCKBACK_RESISTANCE, KB_MODIFIER_ID);
      removeMadness(player);

      for (MobEffect effect : AMBIENT) {
         MobEffectInstance instance = player.getEffect(effect);
         if (instance != null && instance.isAmbient()) {
            player.removeEffect(effect);
         }
      }
   }

   private static void addModifier(LivingEntity entity, net.minecraft.world.entity.ai.attributes.Attribute attribute, UUID id, String name, double amount, Operation operation) {
      AttributeInstance instance = entity.getAttribute(attribute);
      if (instance == null || instance.getModifier(id) != null) {
         return;
      }

      instance.addTransientModifier(new AttributeModifier(id, name, amount, operation));
   }

   private static void removeModifier(LivingEntity entity, net.minecraft.world.entity.ai.attributes.Attribute attribute, UUID id) {
      AttributeInstance instance = entity.getAttribute(attribute);
      if (instance != null && instance.getModifier(id) != null) {
         instance.removeModifier(id);
      }
   }
}
