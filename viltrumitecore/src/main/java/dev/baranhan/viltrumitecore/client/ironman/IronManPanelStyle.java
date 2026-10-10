package dev.baranhan.viltrumitecore.client.ironman;

import dev.baranhan.viltrumitecore.client.hero.PanelStyle;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;

/**
 * JARVIS holographic panel (spec §15.1): translucent blue glass, thin cyan
 * edges with corner brackets, a slow scanline shimmer; cooldown is a blue
 * fill, empty slots are dim "offline" cells. Drawn with fills only.
 */
public final class IronManPanelStyle implements PanelStyle {
   private static final int GLASS = 0x7010243A;
   private static final int EDGE = 0x9060D8FF;
   private static final int BRACKET = 0xFFA8F0FF;
   private static final int SCAN = 0x2890E8FF;
   private static final int CELL = 0x5018405A;
   private static final int OFFLINE = 0x400A1A26;
   private static final int OFFLINE_MARK = 0x5060A0C0;
   private static final int COOLDOWN = 0x902070C0;

   @Override
   public void drawFrames(GuiGraphics graphics, Bounds flightBar, Bounds abilityBar) {
      long time = Util.getMillis();
      frame(graphics, flightBar, time);
      frame(graphics, abilityBar, time);
   }

   @Override
   public void drawSlot(GuiGraphics graphics, int x, int y, boolean empty) {
      if (empty) {
         graphics.fill(x, y, x + 16, y + 16, OFFLINE);
         // "Offline": a short dim dash in the middle of the cell.
         graphics.fill(x + 5, y + 7, x + 11, y + 8, OFFLINE_MARK);
      } else {
         graphics.fill(x, y, x + 16, y + 16, CELL);
      }
   }

   @Override
   public void drawCooldown(GuiGraphics graphics, int x, int y) {
      graphics.fill(x, y, x + 16, y + 16, COOLDOWN);
      graphics.fill(x, y + 15, x + 16, y + 16, EDGE);
   }

   @Override
   public int accentColor() {
      return 0x7FE6FF;
   }

   private static void frame(GuiGraphics graphics, Bounds b, long time) {
      int x0 = b.x();
      int y0 = b.y();
      int x1 = x0 + b.width();
      int y1 = y0 + b.height();
      graphics.fill(x0, y0, x1, y1, GLASS);
      // Thin edges.
      graphics.fill(x0, y0, x1, y0 + 1, EDGE);
      graphics.fill(x0, y1 - 1, x1, y1, EDGE);
      graphics.fill(x0, y0, x0 + 1, y1, EDGE);
      graphics.fill(x1 - 1, y0, x1, y1, EDGE);
      // Corner brackets, 4 px long, slightly outside the edge.
      int l = 4;
      graphics.fill(x0 - 1, y0 - 1, x0 + l, y0, BRACKET);
      graphics.fill(x0 - 1, y0 - 1, x0, y0 + l, BRACKET);
      graphics.fill(x1 - l, y0 - 1, x1 + 1, y0, BRACKET);
      graphics.fill(x1, y0 - 1, x1 + 1, y0 + l, BRACKET);
      graphics.fill(x0 - 1, y1, x0 + l, y1 + 1, BRACKET);
      graphics.fill(x0 - 1, y1 - l, x0, y1 + 1, BRACKET);
      graphics.fill(x1 - l, y1, x1 + 1, y1 + 1, BRACKET);
      graphics.fill(x1, y1 - l, x1 + 1, y1 + 1, BRACKET);
      // Scanline shimmer sweeping down the bar.
      int height = Math.max(1, b.height() - 2);
      int scan = y0 + 1 + (int)(time / 40L % height);
      graphics.fill(x0 + 1, scan, x1 - 1, scan + 1, SCAN);
   }
}
