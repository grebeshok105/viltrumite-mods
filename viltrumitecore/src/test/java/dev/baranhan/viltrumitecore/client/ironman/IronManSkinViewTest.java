package dev.baranhan.viltrumitecore.client.ironman;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.baranhan.viltrumitecore.client.anim.render.RevealMask;
import dev.baranhan.viltrumitecore.hero.ironman.IronManFlags;
import org.junit.jupiter.api.Test;

class IronManSkinViewTest {
   @Test
   void waveFrameFollowsSnapshot() {
      int deploy = IronManFlags.set(0, IronManFlags.Field.DEPLOYING, true);
      int retract = IronManFlags.set(0, IronManFlags.Field.RETRACTING, true);
      int worn = IronManFlags.set(0, IronManFlags.Field.SUIT_WORN, true);
      assertEquals(0, IronManSkinView.frame(0, 0, 0, 0.0F));
      assertEquals(RevealMask.FRAMES, IronManSkinView.frame(worn, 0, 0, 0.0F));
      assertEquals(8, IronManSkinView.frame(deploy, 10, 20, 0.0F));
      assertEquals(RevealMask.FRAMES, IronManSkinView.frame(deploy, 20, 20, 0.0F));
      assertEquals(RevealMask.FRAMES, IronManSkinView.frame(retract, 0, 20, 0.0F));
      assertEquals(0, IronManSkinView.frame(retract, 20, 20, 0.0F));
   }
}
