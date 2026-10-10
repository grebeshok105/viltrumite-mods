package dev.baranhan.viltrumitecore.client.hero;

import static org.junit.jupiter.api.Assertions.assertSame;

import dev.baranhan.viltrumitecore.hero.HeroId;
import net.minecraft.client.gui.GuiGraphics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PanelStylesTest {
   @AfterEach
   void reset() {
      PanelStyles.clear();
   }

   @Test
   void defaultStyleForUnregistered() {
      assertSame(PanelStyle.DEFAULT, PanelStyles.of(HeroId.HOMELANDER));
      assertSame(PanelStyle.DEFAULT, PanelStyles.of((HeroId)null));
   }

   @Test
   void registeredStyleReturned() {
      PanelStyle style = new PanelStyle() {
         @Override
         public void drawFrames(GuiGraphics graphics, Bounds flightBar, Bounds abilityBar) {
         }

         @Override
         public void drawSlot(GuiGraphics graphics, int x, int y, boolean empty) {
         }

         @Override
         public void drawCooldown(GuiGraphics graphics, int x, int y) {
         }

         @Override
         public int accentColor() {
            return 0x7FE6FF;
         }
      };
      PanelStyles.register(HeroId.IRON_MAN, style);
      assertSame(style, PanelStyles.of(HeroId.IRON_MAN));
      assertSame(PanelStyle.DEFAULT, PanelStyles.of(HeroId.REGULUS));
   }
}
