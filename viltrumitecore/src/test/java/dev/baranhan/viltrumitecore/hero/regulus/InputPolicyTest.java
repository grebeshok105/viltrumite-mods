package dev.baranhan.viltrumitecore.hero.regulus;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.baranhan.viltrumitecore.hero.HeroAction;
import org.junit.jupiter.api.Test;

/**
 * Spec 6.2/10-11/16 input policy: jump always passes; Lion locks every slot
 * except its off-toggle; a running cast, channel or ritual locks everything
 * else; Counter is a madness-only slot.
 */
class InputPolicyTest {

   private static final HeroAction[] SLOTS = {
      HeroAction.LIONS_HEART, HeroAction.DEBRIS_KICK, HeroAction.MANIA,
      HeroAction.GREEDS_EMBRACE, HeroAction.COUNTER, HeroAction.RITUAL
   };

   @Test
   void jumpIsAlwaysAllowed() {
      assertTrue(RegulusHero.actionPermitted(HeroAction.JUMP, false, false, false, false, false));
      assertTrue(RegulusHero.actionPermitted(HeroAction.JUMP, true, true, true, true, true), "jump survives every lock");
   }

   @Test
   void lionLeavesOnlyOffToggle() {
      assertTrue(RegulusHero.actionPermitted(HeroAction.LIONS_HEART, true, false, false, false, false), "the off-toggle stays live");
      // The ritual has no input dispatch — the book path bypasses this policy.
      assertFalse(RegulusHero.actionPermitted(HeroAction.RITUAL, true, false, false, false, false), "no dead ritual permission inside Lion");

      for (HeroAction action : new HeroAction[]{HeroAction.DEBRIS_KICK, HeroAction.MANIA, HeroAction.GREEDS_EMBRACE, HeroAction.COUNTER}) {
         assertFalse(RegulusHero.actionPermitted(action, true, false, false, false, true), action + " greyed inside Lion");
      }
   }

   @Test
   void busyChannelAndRitualLockEverySlot() {
      for (HeroAction action : SLOTS) {
         assertFalse(RegulusHero.actionPermitted(action, false, true, false, false, false), "busy locks " + action);
         assertFalse(RegulusHero.actionPermitted(action, false, false, true, false, false), "channel locks " + action);
         assertFalse(RegulusHero.actionPermitted(action, false, false, false, true, false), "running ritual locks " + action);
      }
   }

   @Test
   void counterRequiresMadness() {
      assertFalse(RegulusHero.actionPermitted(HeroAction.COUNTER, false, false, false, false, false), "no madness = grey slot");
      assertTrue(RegulusHero.actionPermitted(HeroAction.COUNTER, false, false, false, false, true));
   }

   @Test
   void ritualCannotStartDuringMadness() {
      assertFalse(RegulusHero.actionPermitted(HeroAction.RITUAL, false, false, false, false, true), "no ritual re-entry while mad");
      assertTrue(RegulusHero.actionPermitted(HeroAction.RITUAL, false, false, false, false, false));
   }

   @Test
   void idleHeroUsesEveryNormalSlot() {
      for (HeroAction action : new HeroAction[]{HeroAction.LIONS_HEART, HeroAction.DEBRIS_KICK, HeroAction.MANIA, HeroAction.GREEDS_EMBRACE, HeroAction.RITUAL}) {
         assertTrue(RegulusHero.actionPermitted(action, false, false, false, false, false), action + " free when idle");
      }
   }
}
