package dev.baranhan.viltrumitecore.client.ironman.veronica;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PartWrapAnimatorTest {
   @Test
   void platesOpenInTheMiddleAndCloseAtTheEnd() {
      assertEquals(0.0F, PartWrapAnimator.open(0.0F), 1.0E-6F);
      assertEquals(1.0F, PartWrapAnimator.open(0.5F), 1.0E-6F);
      assertEquals(0.0F, PartWrapAnimator.open(1.0F), 1.0E-6F);
      assertEquals(1.0F + PartWrapAnimator.OPEN, PartWrapAnimator.scale(0.5F), 1.0E-6F);
   }

   @Test
   void clickFlashIsLateAndNeverNegative() {
      assertEquals(0.0F, PartWrapAnimator.flash(0.5F));
      assertTrue(PartWrapAnimator.flash(0.925F) > 0.99F);
      for (int i = 0; i <= 100; i++) {
         assertTrue(PartWrapAnimator.flash(i / 100.0F) >= 0.0F);
      }
   }
}
