package dev.baranhan.viltrumitecore.hero.regulus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RegulusRulesTest {

   @Test
   void cooldownFormulaHitsExactEndpoints() {
      // cd = ceil(base * (1 - 0.03 * H)); H=0 -> base, H=12 -> base * 0.64
      assertEquals(400, RegulusRules.cooldown(400, 0));
      assertEquals(100, RegulusRules.cooldown(100, 0));
      assertEquals(256, RegulusRules.cooldown(400, 12));   // 400 * 0.64 = 256
      assertEquals(1152, RegulusRules.cooldown(1800, 12)); // 1800 * 0.64 = 1152
      assertEquals(64, RegulusRules.cooldown(100, 12));    // 100 * 0.64 = 64
      assertEquals(1, RegulusRules.cooldown(1, 5));        // ceil(0.85) = 1
   }

   @Test
   void cooldownClampsOutOfRangeHearts() {
      assertEquals(RegulusRules.cooldown(500, 0), RegulusRules.cooldown(500, -3));
      assertEquals(RegulusRules.cooldown(500, 12), RegulusRules.cooldown(500, 99));
   }

   @Test
   void lionWindowScalesWithHearts() {
      assertEquals(60, RegulusRules.lionWindow(0));
      assertEquals(540, RegulusRules.lionWindow(12)); // 60 + 480
   }

   @Test
   void lionWindowShrinksButNeverGrows() {
      int window = RegulusRules.lionWindow(10);
      int shrunk = RegulusRules.shrinkLionWindow(window, 10, 6);
      assertEquals(window - 40 * 4, shrunk);
      // gaining hearts mid-window does not extend it
      assertEquals(window, RegulusRules.shrinkLionWindow(window, 10, 12));
      // never below zero
      assertEquals(0, RegulusRules.shrinkLionWindow(30, 12, 0));
   }

   @Test
   void internalDamageIsNotAnInterruptingLoss() {
      // Blood price (0.6 HP per 20t of madness), heart backlash and overheat are
      // self-inflicted internal drains: they lower HP but must never read as an
      // interrupting hit for casts or the ritual (spec 7.1/10.4/11.1).
      assertEquals(0.0F, RegulusRules.externalHealthLoss(20.0F, 19.5F, 0.5F));
      assertEquals(0.0F, RegulusRules.externalHealthLoss(20.0F, 19.4F, 0.6F), 1.0E-6);
      // A real hit alongside the drain still counts for its own share.
      assertEquals(5.0F, RegulusRules.externalHealthLoss(20.0F, 14.4F, 0.6F), 1.0E-6);
      // No loss at all, healing or garbage input never produces an interrupt.
      assertEquals(0.0F, RegulusRules.externalHealthLoss(20.0F, 20.0F, 0.0F));
      assertEquals(0.0F, RegulusRules.externalHealthLoss(20.0F, 21.0F, 0.0F));
      assertEquals(0.0F, RegulusRules.externalHealthLoss(20.0F, 19.0F, 5.0F));
      // External loss without any internal damage passes through untouched.
      assertEquals(3.5F, RegulusRules.externalHealthLoss(20.0F, 16.5F, 0.0F), 1.0E-6);
   }

   @Test
   void heartBonusIsTwoPercentPerHeart() {
      assertEquals(1.0F, RegulusRules.heartBonus(0), 1.0E-6);
      assertEquals(1.24F, RegulusRules.heartBonus(12), 1.0E-6);
   }

   @Test
   void debrisShardDamageIsFlatAndCappedPerTarget() {
      assertEquals(2.0F, RegulusRules.debrisShardDamage(0, 0), 1.0E-6);
      assertEquals(2.0F, RegulusRules.debrisShardDamage(3, 0), 1.0E-6);
      assertEquals(0.0F, RegulusRules.debrisShardDamage(4, 0), 1.0E-6, "fifth shard on one target deals nothing");
      assertEquals(2.0F * 1.24F, RegulusRules.debrisShardDamage(0, 12), 1.0E-5);
   }

   @Test
   void overheatDrainsOncePerSecondAndLionStopsAfterThreeDrains() {
      assertEquals(0.0F, RegulusRules.overheatDrain(1), 1.0E-6);
      assertEquals(0.0F, RegulusRules.overheatDrain(19), 1.0E-6);
      assertEquals(1.5F, RegulusRules.overheatDrain(20), 1.0E-6);
      assertEquals(2.0F, RegulusRules.overheatDrain(40), 1.0E-6);
      assertFalse(RegulusRules.overheatExhausted(59));
      assertTrue(RegulusRules.overheatExhausted(60));
   }

   @Test
   void hpCollapseOnTheLastOverheatTickIsAForcedOff() {
      // Third drain lands Regulus at exactly 4 HP on overheat tick 60: the
      // forced-off (600t) must win over the normal exhaustion end (100t).
      assertEquals(RegulusRules.LionEnd.FORCED, RegulusRules.lionEndAfterTick(4.0F, 60));
      assertEquals(RegulusRules.LionEnd.FORCED, RegulusRules.lionEndAfterTick(3.0F, 20));
      assertEquals(RegulusRules.LionEnd.EXHAUSTED, RegulusRules.lionEndAfterTick(4.5F, 60));
      assertEquals(RegulusRules.LionEnd.NONE, RegulusRules.lionEndAfterTick(10.0F, 59));
      assertEquals(RegulusRules.LionEnd.NONE, RegulusRules.lionEndAfterTick(10.0F, 0));
   }

   @Test
   void heartLossLeavesAWarningGrace() {
      // 2 hearts lost at elapsed 100 from a 140 window would cut to 60 (instant
      // overheat); the grace keeps 40 ticks from now instead.
      assertEquals(140, RegulusRules.windowAfterHeartLoss(220, 140, 100), "shrink above grace is kept");
      assertEquals(140, RegulusRules.windowAfterHeartLoss(220, 60, 100), "grace = elapsed + 40");
      assertEquals(110, RegulusRules.windowAfterHeartLoss(110, 30, 100), "grace never extends past the old window");
      assertEquals(260, RegulusRules.windowAfterHeartLoss(220, 260, 100), "no loss, no change");
   }

   @Test
   void counterDamageCapsAtFortyFiveAndScales() {
      // min(45, 15 + 0.15 * maxHP) * (1 + 0.02H)
      assertEquals(30.0F, RegulusRules.counterDamage(100.0F, 0), 1.0E-6);
      assertEquals(45.0F, RegulusRules.counterDamage(400.0F, 0), 1.0E-6);
      assertEquals(45.0F * 1.24F, RegulusRules.counterDamage(400.0F, 12), 1.0E-5);
   }

   @Test
   void overheatDpsStepsUpEveryFortyTicks() {
      assertEquals(1.5F, RegulusRules.overheatDps(0), 1.0E-6);
      assertEquals(1.5F, RegulusRules.overheatDps(39), 1.0E-6);
      assertEquals(2.0F, RegulusRules.overheatDps(40), 1.0E-6);
      assertEquals(3.0F, RegulusRules.overheatDps(120), 1.0E-6);
   }

   @Test
   void deferredCapsMatchSpec() {
      assertEquals(8.0F, RegulusRules.freezeDeferredCap(20.0F), 1.0E-6);
      assertEquals(7.0F, RegulusRules.domeDeferredCap(20.0F), 1.0E-6);
   }

   @Test
   void heartBacklashIsTenPercentMaxHealth() {
      assertEquals(2.0F, RegulusRules.heartBacklash(20.0F), 1.0E-6);
   }

   @Test
   void jumpVelocityReachesTenBlocksAtFullCharge() {
      float full = RegulusRules.jumpVelocity(RegulusRules.JUMP_CHARGE_TICKS);
      float partial = RegulusRules.jumpVelocity(RegulusRules.JUMP_CHARGE_TICKS / 2);
      assertTrue(partial > 0.0F && partial < full);
      // h ~= v^2 / (2*g), g=0.08: v >= 1.26 -> ~10 blocks
      assertTrue(full >= 1.26F && full <= 1.40F, "full charge should give ~10 blocks: " + full);
      assertEquals(full, RegulusRules.jumpVelocity(RegulusRules.JUMP_CHARGE_TICKS * 4), 1.0E-6);
   }

   @Test
   void attackerRecordExpiresAfterTwoHundredFortyTicks() {
      assertTrue(RegulusRules.attackerValid(1000, 1000 + 240));
      assertTrue(RegulusRules.attackerValid(1000, 1000 + 241 - 1));
      assertTrue(!RegulusRules.attackerValid(1000, 1000 + 241));
   }

   @Test
   void repulsePushesOpponentsOnly() {
      assertTrue(RegulusRules.repulseTarget(true, false, false));
      // Allies (scoreboard team) are never opponents for the impulse.
      assertTrue(!RegulusRules.repulseTarget(true, true, false));
      // Own heart carriers are never pushed.
      assertTrue(!RegulusRules.repulseTarget(true, false, true));
      // Non-opponents (animals, decoration entities) are never pushed.
      assertTrue(!RegulusRules.repulseTarget(false, false, false));
   }

   @Test
   void repulsePushNeverMovesTheImmuneOrAnchored() {
      // Spec 6.2/8.3: the impulse gate and the anchor guard cap the push even
      // when the opponent filter passes.
      assertTrue(RegulusRules.repulsePushable(true, false, false, true, false));
      assertTrue(!RegulusRules.repulsePushable(true, false, false, false, false), "impulse-immune target");
      assertTrue(!RegulusRules.repulsePushable(true, false, false, true, true), "already anchored");
      // Opponent rules still gate first.
      assertTrue(!RegulusRules.repulsePushable(false, false, false, true, false));
      assertTrue(!RegulusRules.repulsePushable(true, true, false, true, false));
      assertTrue(!RegulusRules.repulsePushable(true, false, true, true, false));
   }

   @Test
   void shockwaveDamageIgnoresTheHeartBonus() {
      // Spec 5.4 scopes the +2%/heart bonus to abilities and melee; the landing
      // shockwave is a section-4 passive and stays flat.
      assertEquals(RegulusRules.SHOCKWAVE_DAMAGE, RegulusRules.shockwaveDamage(0));
      assertEquals(RegulusRules.SHOCKWAVE_DAMAGE, RegulusRules.shockwaveDamage(RegulusRules.MAX_HEARTS));
   }

   @Test
   void savedSlownessRestoresOnlyItsRemainder() {
      assertEquals(0, RegulusRules.slownessRemainder(10, 14));
      assertEquals(46, RegulusRules.slownessRemainder(60, 14));
      assertEquals(0, RegulusRules.slownessRemainder(60, 60));
      assertEquals(0, RegulusRules.slownessRemainder(0, 0));
   }

   @Test
   void lionOffToggleIgnoresEquipAndCooldown() {
      // A second press while Lion runs reaches the toggle even with the slot
      // unequipped and a cooldown set (spec 6.4).
      assertTrue(RegulusRules.mayStartAbility(false, true, true));
      assertTrue(RegulusRules.mayStartAbility(true, false, false));
      // New casts still need the slot equipped and off cooldown.
      assertTrue(!RegulusRules.mayStartAbility(false, false, false));
      assertTrue(!RegulusRules.mayStartAbility(true, true, false));
   }
}
