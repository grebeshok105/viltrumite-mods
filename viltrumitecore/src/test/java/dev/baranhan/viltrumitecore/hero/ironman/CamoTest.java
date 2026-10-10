package dev.baranhan.viltrumitecore.hero.ironman;

import static org.junit.jupiter.api.Assertions.*;

import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkId;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkSpec;
import dev.baranhan.viltrumitecore.hero.ironman.mark.sig.Camo;
import dev.baranhan.viltrumitecore.hero.ironman.mark.sig.SignatureRules;
import org.junit.jupiter.api.Test;

class CamoTest {
   @Test
   void lasts160() {
      assertFalse(Camo.lapsed(159));
      assertTrue(Camo.lapsed(160));
      assertEquals(160, SignatureRules.CAMO_TICKS);
      assertEquals(400, SignatureRules.CAMO_COOLDOWN);
   }

   @Test
   void mobsBeyond8Lose() {
      assertTrue(SignatureRules.camoLosesPlayer(8.5));
      assertFalse(SignatureRules.camoLosesPlayer(8.0));
      assertFalse(SignatureRules.camoLosesPlayer(3.0));
   }

   @Test
   void firstHitDoubleEndsCamo() {
      assertEquals(2.0F, SignatureRules.camoOutgoing(true), 1.0E-9F);
      assertEquals(1.0F, SignatureRules.camoOutgoing(false), 1.0E-9F);
   }

   @Test
   void hiddenFromFocusAndScanSilentFlight() {
      MarkSpec spec = MarkSpec.of(MarkId.MARK_15);
      assertTrue(spec.stealth(), "hidden from focus and scan");
      assertTrue(spec.silentFlight(), "no thruster sound and no flames");
   }
}
