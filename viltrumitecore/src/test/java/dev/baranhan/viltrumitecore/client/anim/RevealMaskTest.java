package dev.baranhan.viltrumitecore.client.anim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.baranhan.viltrumitecore.client.anim.render.RevealMask;
import org.junit.jupiter.api.Test;

class RevealMaskTest {
   private static final RevealMask.Field FIELD = RevealMask.field();

   @Test
   void zeroShowsNone() {
      forEachUsed((x, y, d) -> assertFalse(RevealMask.visible(d, 0.0F), x + "," + y));
   }

   @Test
   void fullProgressShowsAll() {
      int[] used = {0};
      forEachUsed((x, y, d) -> {
         assertTrue(RevealMask.visible(d, 1.0F), x + "," + y);
         used[0]++;
      });
      // 6 boxes x 2 layers of the classic layout.
      assertTrue(used[0] > 2000, "texels mapped: " + used[0]);
   }

   @Test
   void revealGrowsFromChest() {
      float reactorTexel = FIELD.at(23, 23);
      float footTexel = FIELD.at(5, 31);
      float handTexel = FIELD.at(45, 31);
      assertTrue(reactorTexel < 0.1F, "reactor " + reactorTexel);
      assertTrue(reactorTexel < handTexel && handTexel < footTexel, "chest < hand < foot");
      for (int frame = 0; frame < RevealMask.FRAMES; frame++) {
         float now = RevealMask.progressOf(frame);
         float next = RevealMask.progressOf(frame + 1);
         forEachUsed((x, y, d) -> {
            if (RevealMask.visible(d, now)) {
               assertTrue(RevealMask.visible(d, next), "texel hides again at " + x + "," + y);
            }
         });
      }
   }

   @Test
   void helmetRevealsLast() {
      float[] bodyMax = {0.0F};
      float[] headMin = {Float.MAX_VALUE};
      forEachUsed((x, y, d) -> {
         if (FIELD.isHead(x, y)) {
            headMin[0] = Math.min(headMin[0], d);
         } else {
            bodyMax[0] = Math.max(bodyMax[0], d);
         }
      });
      assertTrue(headMin[0] > bodyMax[0], "head " + headMin[0] + " body " + bodyMax[0]);
      assertEquals(headMin[0], FIELD.headStart(), 1.0E-6F);
      assertTrue(FIELD.headStartFrame() > 1 && FIELD.headStartFrame() < RevealMask.FRAMES);
   }

   @Test
   void unusedTexelsStayHidden() {
      // (0,0) is outside every box of the player layout.
      assertTrue(Float.isNaN(FIELD.at(0, 0)));
      assertFalse(RevealMask.visible(FIELD.at(0, 0), 1.0F));
   }

   @Test
   void rimFollowsTheFront() {
      float p = 0.5F;
      forEachUsed((x, y, d) -> {
         if (RevealMask.rim(d, p)) {
            assertTrue(d <= p && d > p - RevealMask.RIM_BAND);
         }
      });
      forEachUsed((x, y, d) -> assertFalse(RevealMask.rim(d, 1.0F)));
   }

   @Test
   void framesCoverProgress() {
      assertEquals(0, RevealMask.frame(0.0F));
      assertEquals(1, RevealMask.frame(0.01F));
      assertEquals(RevealMask.FRAMES, RevealMask.frame(1.0F));
      assertEquals(8, RevealMask.frame(0.5F));
   }

   private interface Visitor {
      void visit(int x, int y, float distance);
   }

   private static void forEachUsed(Visitor visitor) {
      for (int y = 0; y < RevealMask.SIZE; y++) {
         for (int x = 0; x < RevealMask.SIZE; x++) {
            float d = FIELD.at(x, y);
            if (!Float.isNaN(d)) {
               visitor.visit(x, y, d);
            }
         }
      }
   }
}
