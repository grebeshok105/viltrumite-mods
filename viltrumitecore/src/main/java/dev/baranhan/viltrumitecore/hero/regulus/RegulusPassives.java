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
      addModifier(player, Attributes.ATTACK_DAMAGE, MADNESS_ATTACK_MODIFIER_ID, "Regulus madness strength", 0.4, Operation.MULTIPLY_TOTAL);
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 2100, 2, true, false, true));
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 2100, 2, true, false, true));
      player.addEffect(new MobEffectInstance(MobEffects.JUMP, 2100, 2, true, false, true));
      player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 2100, 0, true, false, true));
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 2100, 0, true, false, true));
   }

   public static void removeMadness(Player player) {
      removeModifier(player, Attributes.ARMOR, MADNESS_ARMOR_MODIFIER_ID);
      removeModifier(player, Attributes.MAX_HEALTH, MADNESS_HEALTH_MODIFIER_ID);
      removeModifier(player, Attributes.ATTACK_DAMAGE, MADNESS_ATTACK_MODIFIER_ID);
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
