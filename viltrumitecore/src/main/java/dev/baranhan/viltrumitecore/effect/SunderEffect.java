package dev.baranhan.viltrumitecore.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Nano-blade sunder (spec §8.4): armor -30% and no shield blocking while it
 * lasts. Vanilla shields get the axe-style cooldown; hero shields ask
 * {@code hasEffect(SUNDER)}.
 */
public class SunderEffect extends MobEffect {
   public static final String ARMOR_ID = "3b7d1c52-9a4e-4f10-8c2d-5e6f7a8b9c01";

   public SunderEffect() {
      super(MobEffectCategory.HARMFUL, 0x5AD8FF);
      this.addAttributeModifier(Attributes.ARMOR, ARMOR_ID, -0.3, AttributeModifier.Operation.MULTIPLY_TOTAL);
   }

   @Override
   public void addAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
      super.addAttributeModifiers(entity, attributes, amplifier);
      if (entity instanceof Player player && player.isBlocking()) {
         player.disableShield(true);
      }
   }

   @Override
   public void applyEffectTick(LivingEntity entity, int amplifier) {
      if (entity instanceof Player player && player.isBlocking()) {
         player.disableShield(true);
      }
   }

   @Override
   public boolean isDurationEffectTick(int duration, int amplifier) {
      return true;
   }
}
