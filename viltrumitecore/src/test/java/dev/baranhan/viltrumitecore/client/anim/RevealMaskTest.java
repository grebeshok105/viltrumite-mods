package dev.baranhan.viltrumitecore.client.anim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.baranhan.viltrumitecore.client.anim.render.RevealMask;
import org.junit.jupiter.api.Test;

class RevealMaskTest {
   private static final float[] FIELD = RevealMask.playerSkinField(0.0F, 20.5F, -2.0F, 0.2F);

   private static float at(int u, int v) {
      return FIELD[v * 64 + u];
   }

   private static int visibleCount(float progress) {
      int n = 0;
      for (float f : FIELD) {
         if (RevealMask.visible(f, progress)) {
            n++;
         }
      }
      return n;
   }

   @Test
   void revealGrowsFromChest() {
      float chest = at(23, 23); // body front, reactor
      float foot = at(5, 31);   // right leg front, bottom row
      float hand = at(45, 31);  // right arm front, bottom row
      assertTrue(chest < hand && hand < foot, chest + " " + hand + " " + foot);
      int prev = 0;
      for (int k = 1; k <= RevealMask.FRAMES; k++) {
         int count = visibleCount(k / (float)RevealMask.FRAMES);
         assertTrue(count >= prev);
         prev = count;
      }
      assertTrue(visibleCount(0.25F) < visibleCount(0.75F));
   }

   @Test
   void fullProgressShowsAll() {
      int used = 0;
      for (float f : FIELD) {
         if (RevealMask.used(f)) {
            used++;
            assertTrue(RevealMask.visible(f, 1.0F));
         }
      }
      // 6 boxes × 2 layers of the vanilla 64×64 layout
      assertEquals(2 * (384 + 352 + 4 * 224), used);
   }

   @Test
   void zeroShowsNone() {
      assertEquals(0, visibleCount(0.0F));
      assertEquals(0, RevealMask.frame(0.0F));
      assertEquals(RevealMask.FRAMES, RevealMask.frame(1.0F));
      assertEquals(1, RevealMask.frame(0.01F));
   }

   @Test
   void helmetRevealsLast() {
      float headMin = Float.MAX_VALUE;
      float bodyMax = 0.0F;
      for (int v = 0; v < 64; v++) {
         for (int u = 0; u < 64; u++) {
            float f = at(u, v);
            if (!RevealMask.used(f)) {
               continue;
            }
            boolean head = v < 16;
            if (head) {
               headMin = Math.min(headMin, f);
            } else {
               bodyMax = Math.max(bodyMax, f);
            }
         }
      }
      assertTrue(headMin > bodyMax, headMin + " vs " + bodyMax);
      assertFalse(RevealMask.visible(at(12, 12), 0.79F), "face hidden until the last fifth");
   }
}
