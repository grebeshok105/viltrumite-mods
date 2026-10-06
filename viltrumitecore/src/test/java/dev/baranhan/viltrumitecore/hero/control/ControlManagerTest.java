package dev.baranhan.viltrumitecore.hero.control;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ControlManagerTest {

   @Test
   void freezeRecordDropsOnlyWhenProvablyGone() {
      // A resolved projectile keeps its freeze record.
      assertFalse(ControlManager.shouldDropFreezeRecord(true, true));
      assertFalse(ControlManager.shouldDropFreezeRecord(true, false));
      // An unloaded chunk keeps the record too: the projectile is still there
      // and re-resolves on reload, so its original gravity survives.
      assertFalse(ControlManager.shouldDropFreezeRecord(false, false));
      // Loaded chunk but no entity = removed or moved away; the record drops
      // and the gravity restore defers to the next entity join.
      assertTrue(ControlManager.shouldDropFreezeRecord(false, true));
   }
}
