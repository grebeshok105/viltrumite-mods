package dev.baranhan.viltrumitecore.hero.ironman;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.baranhan.viltrumitecore.hero.ironman.mark.Mark42Parts;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkDamage;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkId;
import dev.baranhan.viltrumitecore.hero.ironman.mark.SuitPart;
import dev.baranhan.viltrumitecore.hero.ironman.mark.SuitSpec;
import org.junit.jupiter.api.Test;

class MarkDamageTest {
   @Test
   void markAbsorbsWholeHitArmorSlowsLoss() {
      MarkDamage.Result hit = MarkDamage.apply(100.0F, 10.0F, 20.0);
      assertTrue(hit.absorbed());
      assertFalse(hit.broke());
      assertEquals(100.0F - 10.0F * 0.6F, hit.durability(), 1.0E-4F);
      assertEquals(MarkDamage.MIN_LOSS, MarkDamage.lossFactor(1000.0), 1.0E-6F);
   }

   @Test
   void breakingHitDoesNotSpill() {
      MarkDamage.Result hit = MarkDamage.apply(3.0F, 500.0F, 10.0);
      assertTrue(hit.absorbed(), "the hit that empties the mark is still fully absorbed");
      assertTrue(hit.broke());
      assertEquals(0.0F, hit.durability());
      assertFalse(MarkDamage.apply(0.0F, 5.0F, 10.0).absorbed(), "an empty mark absorbs nothing");
   }

   @Test
   void suitSpecNanoDefaultsAndMarkFactors() {
      assertFalse(SuitSpec.NANO.isMark());
      assertEquals(IronManRules.NANO_ARMOR, SuitSpec.NANO.armor(), 1.0E-9);
      assertEquals(IronManRules.MISSILE_MARKS, SuitSpec.NANO.missileMarks());
      SuitSpec starboost = SuitSpec.mark(MarkId.MARK_39, 0);
      assertEquals(1.35F, starboost.flightSpeedMul(), 1.0E-6F);
      assertEquals(0.6F, starboost.sonicDrainMul(), 1.0E-6F);
      assertEquals(0.85F, starboost.weaponMul(), 1.0E-6F);
      assertTrue(SuitSpec.mark(MarkId.MARK_15, 0).stealth());
      assertTrue(SuitSpec.mark(MarkId.MARK_15, 0).silentFlight());
      assertEquals(0.2F, SuitSpec.mark(MarkId.IRON_HEART_MK3, 0).recoilMul(), 1.0E-6F);
   }

   @Test
   void mark42LosesPartsByThreshold() {
      float max = 100.0F;
      assertEquals(0, Mark42Parts.lostCount(max, max));
      assertEquals(1, Mark42Parts.lostCount(max - max / 14.0F - 0.01F, max));
      assertEquals(13, Mark42Parts.lostCount(0.5F, max), "the last part never drops: at 0 the suit breaks");
      int lostLegs = Mark42Parts.lostMask(max * 5.0F / 14.0F, max);
      assertTrue(Mark42Parts.flightFactor(lostLegs) < 1.0F, "boots lost slow the flight");
      int lostAll = Mark42Parts.lostMask(0.5F, max);
      assertTrue(Mark42Parts.chestLost(lostAll));
      assertTrue(Mark42Parts.helmetLost(lostAll));
      SuitSpec broken = SuitSpec.mark(MarkId.MARK_42, lostAll);
      assertFalse(broken.unibeamOnline(), "lost chest: no Unibeam");
      assertTrue(broken.helmetForcedOpen());
      assertEquals(0.5F, broken.weaponMul(), 1.0E-6F);
      assertEquals(0, lostAll & ~SuitPart.fullMask(MarkId.MARK_42));
   }
}
