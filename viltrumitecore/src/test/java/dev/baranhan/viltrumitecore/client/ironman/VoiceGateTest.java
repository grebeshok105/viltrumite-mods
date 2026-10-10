package dev.baranhan.viltrumitecore.client.ironman;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.baranhan.viltrumitecore.client.ironman.jarvis.VoiceGate;
import dev.baranhan.viltrumitecore.hero.ironman.IronManRules;
import org.junit.jupiter.api.Test;

class VoiceGateTest {
   @Test
   void globalGapAndPerLineCooldown() {
      VoiceGate gate = new VoiceGate();
      assertTrue(gate.tryPlay("a", 0, 40, 600));
      assertFalse(gate.tryPlay("b", 10, 40, 600), "never two at once");
      assertFalse(gate.tryPlay("b", 40 + IronManRules.JARVIS_GLOBAL_GAP - 1, 40, 600), "global gap after the line");
      assertTrue(gate.tryPlay("b", 40 + IronManRules.JARVIS_GLOBAL_GAP, 40, 600));
      assertFalse(gate.tryPlay("a", 400, 40, 600), "per-line cooldown");
      assertTrue(gate.tryPlay("a", 600, 40, 600));
      assertTrue(IronManRules.JARVIS_GLOBAL_GAP >= 80, "≥ 4 s");
   }
}
