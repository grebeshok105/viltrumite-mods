package dev.baranhan.viltrumitecore.hero.ironman.scan;

import dev.baranhan.viltrumitecore.hero.HeroRegistry;
import dev.baranhan.viltrumitecore.hero.ScanInfo;
import dev.baranhan.viltrumitecore.hero.ScanLine;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Scan analysis (spec §11.2). Reads the target only: attributes, effects and
 * {@code isInvulnerableTo} queries with damage sources that are built but
 * never applied. Hero protections of players come only from
 * HeroDefinition.scanInfo (they live in HeroDamage / PlayerStatsMixin, not in
 * isInvulnerableTo). Nothing in the card is computed on the client.
 */
public final class ScanAnalyzer {
   private ScanAnalyzer() {
   }

   /** Read-only facts of a target (server). */
   public static ScanTraits traits(LivingEntity target) {
      boolean player = target instanceof Player;
      MobEffectInstance resistance = target.getEffect(MobEffects.DAMAGE_RESISTANCE);
      boolean explosion = false;
      boolean projectile = false;
      boolean magic = false;
      if (!player) {
         DamageSources sources = target.damageSources();
         explosion = target.isInvulnerableTo(sources.explosion(null, null));
         projectile = target.isInvulnerableTo(sources.thrown(null, null));
         magic = target.isInvulnerableTo(sources.magic());
      }

      ScanInfo hero = target instanceof Player self ? HeroRegistry.get(self).scanInfo(self) : ScanInfo.EMPTY;
      return new ScanTraits(player, target.fireImmune(), resistance == null ? 0 : resistance.getAmplifier() + 1,
         target.hasEffect(MobEffects.FIRE_RESISTANCE), attribute(target, Attributes.KNOCKBACK_RESISTANCE),
         explosion, projectile, magic, target.getMobType() == MobType.UNDEAD, target.getMobType() == MobType.ARTHROPOD,
         target.isSensitiveToWater(), hero);
   }

   /** Resist lines, in card order; the hero part last. */
   public static List<ScanLine> resists(ScanTraits traits) {
      List<ScanLine> lines = new ArrayList<>();
      if (traits.fireImmune()) {
         lines.add(ScanLine.of("scan.viltrumitecore.resist.fire_immune"));
      } else if (traits.fireResistance()) {
         lines.add(ScanLine.of("scan.viltrumitecore.resist.fire_resistance"));
      }

      if (traits.resistance() > 0) {
         // Vanilla Resistance: -20 % damage per level, 5+ = immune.
         lines.add(ScanLine.of("scan.viltrumitecore.resist.resistance", traits.resistance(), Math.min(100, traits.resistance() * 20)));
      }

      if (traits.knockbackResistance() > 0.0) {
         lines.add(ScanLine.of("scan.viltrumitecore.resist.knockback", Math.round(Math.min(1.0, traits.knockbackResistance()) * 100.0)));
      }

      if (traits.immuneExplosion()) {
         lines.add(ScanLine.of("scan.viltrumitecore.resist.explosion"));
      }

      if (traits.immuneProjectile()) {
         lines.add(ScanLine.of("scan.viltrumitecore.resist.projectile"));
      }

      if (traits.immuneMagic()) {
         lines.add(ScanLine.of("scan.viltrumitecore.resist.magic"));
      }

      lines.addAll(traits.hero().protections());
      return lines;
   }

   /** Full card of a target (server, read-only). */
   public static ScanCard card(LivingEntity target) {
      ScanTraits traits = traits(target);
      List<EffectLine> effects = new ArrayList<>();
      for (MobEffectInstance effect : target.getActiveEffects()) {
         if (effects.size() >= ScanCard.MAX_LINES) {
            break;
         }

         effects.add(new EffectLine(effect.getEffect().getDescriptionId(), effect.getAmplifier(), effect.isInfiniteDuration() ? -1 : effect.getDuration(),
            effect.getEffect().isBeneficial()));
      }

      return new ScanCard(target.getId(), target.getDisplayName(), target.getHealth(), target.getMaxHealth(), target.getArmorValue(),
         (float)attribute(target, Attributes.ARMOR_TOUGHNESS), effects, resists(traits), (float)attribute(target, Attributes.ATTACK_DAMAGE),
         (float)attribute(target, Attributes.MOVEMENT_SPEED), WeakSpots.of(traits), traits.hero().conditions());
   }

   private static double attribute(LivingEntity target, Attribute attribute) {
      AttributeInstance instance = target.getAttribute(attribute);
      return instance == null ? 0.0 : instance.getValue();
   }
}
