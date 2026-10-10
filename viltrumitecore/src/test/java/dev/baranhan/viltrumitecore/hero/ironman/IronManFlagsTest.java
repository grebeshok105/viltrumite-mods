package dev.baranhan.viltrumitecore.hero.ironman;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IronManFlagsTest {

   @Test
   void flagsDoNotOverlap() {
      long used = 0;
      for (IronManFlags.Field field : IronManFlags.Field.values()) {
         long mask = ((1L << field.width()) - 1) << field.shift();
         assertEquals(0, used & mask, field.name());
         used |= mask;
         assertTrue(field.shift() + field.width() <= 32, field + " must fit into the 32 flag bits");
      }
   }

   @Test
   void stage1BitsMatchThePlan() {
      assertEquals(0, IronManFlags.Field.SUIT_WORN.shift());
      assertEquals(1, IronManFlags.Field.DEPLOYING.shift());
      assertEquals(2, IronManFlags.Field.RETRACTING.shift());
      assertEquals(3, IronManFlags.Field.GLIDE.shift());
      assertEquals(4, IronManFlags.Field.HEAVY_LANDING.shift());
      assertEquals(5, IronManFlags.Field.OVERHEAT_COUNT.shift());
      assertEquals(2, IronManFlags.Field.OVERHEAT_COUNT.width());
      assertEquals(24, IronManFlags.Field.HULKBUSTER_PHASE.shift());
      assertEquals(3, IronManFlags.Field.HULKBUSTER_PHASE.width());
   }

   @Test
   void flagsRoundTrip() {
      int flags = 0;
      int value = 0;
      for (IronManFlags.Field field : IronManFlags.Field.values()) {
         int max = (1 << field.width()) - 1;
         flags = IronManFlags.set(flags, field, max - (value++ % (max + 1)));
      }

      value = 0;
      for (IronManFlags.Field field : IronManFlags.Field.values()) {
         int max = (1 << field.width()) - 1;
         assertEquals(max - (value++ % (max + 1)), IronManFlags.get(flags, field), field.name());
      }
   }

   @Test
   void setClampsToFieldWidthAndKeepsNeighbours() {
      int flags = IronManFlags.set(0, IronManFlags.Field.SUIT_WORN, 1);
      flags = IronManFlags.set(flags, IronManFlags.Field.OVERHEAT_COUNT, 7);
      assertEquals(3, IronManFlags.get(flags, IronManFlags.Field.OVERHEAT_COUNT));
      assertEquals(1, IronManFlags.get(flags, IronManFlags.Field.SUIT_WORN));
      assertEquals(0, IronManFlags.get(flags, IronManFlags.Field.OVERHEAT_LOCK));
      flags = IronManFlags.set(flags, IronManFlags.Field.SUIT_WORN, false);
      assertEquals(0, IronManFlags.get(flags, IronManFlags.Field.SUIT_WORN));
      assertTrue(IronManFlags.is(flags, IronManFlags.Field.OVERHEAT_COUNT));
   }
}
