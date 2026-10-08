package dev.baranhan.viltrumitecore.client.homelander;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FocusPaletteTest {

   @Test
   void eightDistinctColors() {
      Set<Integer> colors = new HashSet<>();
      for (int i = 0; i < FocusPalette.SIZE; i++) {
         colors.add(FocusPalette.colorFor(i));
      }
      assertEquals(8, FocusPalette.SIZE);
      assertEquals(8, colors.size());
   }

   @Test
   void noRed() {
      for (int i = 0; i < FocusPalette.SIZE; i++) {
         int c = FocusPalette.colorFor(i);
         float[] hsb = java.awt.Color.RGBtoHSB(c >> 16 & 255, c >> 8 & 255, c & 255, null);
         float hue = hsb[0] * 360.0F;
         boolean red = hsb[1] > 0.2F && (hue >= 345.0F || hue <= 15.0F);
         assertFalse(red, "slot " + i + " is red: " + Integer.toHexString(c));
      }
   }

   @Test
   void colorSticks() {
      FocusPalette palette = new FocusPalette();
      palette.update(List.of(10, 20, 30));
      int c20 = palette.colorOf(20);
      int c30 = palette.colorOf(30);
      palette.update(List.of(30, 20, 40));
      assertEquals(c20, palette.colorOf(20));
      assertEquals(c30, palette.colorOf(30));
      // The freed slot of 10 is reused by the newcomer, never a colour still in use.
      assertNotEquals(c20, palette.colorOf(40));
      assertNotEquals(c30, palette.colorOf(40));
      assertEquals(-1, palette.colorOf(10));
   }
}
