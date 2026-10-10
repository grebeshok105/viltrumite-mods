package dev.baranhan.viltrumitecore.client.ironman.mark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.baranhan.viltrumitecore.hero.ironman.IronManFlags;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkId;
import dev.baranhan.viltrumitecore.hero.ironman.mark.SuitPart;
import org.junit.jupiter.api.Test;

class MarkStateTest {
   private static MarkState state(MarkId mark, boolean worn, int phase, int parts, float durability, float max) {
      return new MarkState(mark, worn, phase, parts, durability, max, true, false, 0, 0, null);
   }

   @Test
   void wholeSuitOnIsFullAndHasNoPlates() {
      MarkState s = state(MarkId.MARK_7, true, IronManFlags.EQUIP_NONE, SuitPart.fullMask(MarkId.MARK_7), 120.0F, 120.0F);
      assertTrue(s.markOn());
      assertTrue(s.full());
   }

   @Test
   void lostMarkFortyTwoPartIsNotFull() {
      int mask = SuitPart.fullMask(MarkId.MARK_42) & ~(1 << 13);
      MarkState s = state(MarkId.MARK_42, true, IronManFlags.EQUIP_NONE, mask, 50.0F, 100.0F);
      assertTrue(s.markOn());
      assertFalse(s.full());
      assertFalse(s.partPresent(13));
      assertTrue(s.partPresent(0));
   }

   @Test
   void exitingIsOnButNotFull() {
      MarkState s = state(MarkId.MARK_17, false, IronManFlags.EQUIP_EXITING, 0, 110.0F, 110.0F);
      assertTrue(s.markOn());
      assertFalse(s.full());
   }

   @Test
   void durabilityFractionClamps() {
      assertEquals(1.0F, state(MarkId.MARK_7, true, 0, 0, 500.0F, 120.0F).durabilityFraction());
      assertEquals(0.0F, state(MarkId.MARK_7, true, 0, 0, -3.0F, 120.0F).durabilityFraction());
   }
}
