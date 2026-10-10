package dev.baranhan.viltrumitecore.hero.ironman;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.baranhan.viltrumitecore.hero.ironman.mark.EquipTimeline;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkId;
import dev.baranhan.viltrumitecore.hero.ironman.mark.SuitPart;
import java.util.List;
import org.junit.jupiter.api.Test;

class EquipTimelineTest {
   @Test
   void orderLegsArmsChestBackHelmet() {
      for (List<SuitPart> set : List.of(SuitPart.SEVEN, SuitPart.NINE, SuitPart.FOURTEEN)) {
         assertTrue(set.get(0).bone().name().endsWith("_LEG"), "legs first");
         assertEquals("helmet", set.get(set.size() - 1).name(), "helmet last");
         int lastLeg = -1;
         int firstArm = Integer.MAX_VALUE;
         int chest = -1;
         int back = -1;
         for (int i = 0; i < set.size(); i++) {
            SuitPart part = set.get(i);
            if (part.bone().name().endsWith("_LEG")) {
               lastLeg = i;
            } else if (part.bone().name().endsWith("_ARM")) {
               firstArm = Math.min(firstArm, i);
            } else if (part.name().equals("chest")) {
               chest = i;
            } else if (part.name().equals("back")) {
               back = i;
            }
         }

         assertTrue(lastLeg < firstArm && firstArm < chest && chest < back);
      }
   }

   @Test
   void mark42Has14Parts() {
      assertEquals(14, SuitPart.of(MarkId.MARK_42).size());
      assertEquals(7, SuitPart.of(MarkId.MARK_15).size());
      assertEquals(9, SuitPart.of(MarkId.MARK_7).size());
   }

   @Test
   void deliveryLocksInOrderAndEndsAt50() {
      int count = 9;
      int previous = -1;
      for (int i = 0; i < count; i++) {
         int lock = EquipTimeline.lockTick(i, count, true);
         assertTrue(lock > previous);
         assertTrue(EquipTimeline.launchTick(i, count, true) <= EquipTimeline.arriveTick(i, count, true));
         assertEquals(EquipTimeline.WRAP, lock - EquipTimeline.arriveTick(i, count, true));
         previous = lock;
      }

      assertEquals(50, EquipTimeline.DELIVERY);
      assertEquals(EquipTimeline.DELIVERY, EquipTimeline.lockTick(count - 1, count, true));
      assertSame(EquipTimeline.Phase.FLYING, EquipTimeline.phase(0, count, true, 1.0F));
      assertSame(EquipTimeline.Phase.LOCKED, EquipTimeline.phase(count - 1, count, true, 50.0F));
   }

   @Test
   void lockedMaskGrows() {
      int count = 14;
      int previous = 0;
      for (int t = 0; t <= EquipTimeline.DELIVERY; t++) {
         int mask = EquipTimeline.lockedMask(count, true, t, 0);
         assertEquals(previous, mask & previous);
         previous = mask;
      }

      assertEquals((1 << count) - 1, previous);
   }

   @Test
   void enterHasNoFlight() {
      for (int i = 0; i < 7; i++) {
         assertEquals(EquipTimeline.launchTick(i, 7, false), EquipTimeline.arriveTick(i, 7, false));
      }

      assertEquals(EquipTimeline.ENTER, EquipTimeline.lockTick(6, 7, false));
   }
}
