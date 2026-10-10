package dev.baranhan.viltrumitecore.client.hero;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Look of the ability panel (ViltrumiteInGameHudMixin). The layout (six
 * vertical slots, three flight slots, page indicator) never moves; a style
 * only paints frames, slot cells, the cooldown overlay and the accent colour.
 * Registered per hero in {@link PanelStyles}; {@link #DEFAULT} is the
 * original drawing.
 */
public interface PanelStyle {
   PanelStyle DEFAULT = new DefaultPanelStyle();

   /** Frames of both bars: horizontal flight bar and vertical ability bar. */
   void drawFrames(GuiGraphics graphics, Bounds flightBar, Bounds abilityBar);

   /** A 16x16 slot cell before its icon; {@code empty} = no ability in it. */
   void drawSlot(GuiGraphics graphics, int x, int y, boolean empty);

   /** Overlay of a greyed (cooldown / unavailable) 16x16 icon. */
   void drawCooldown(GuiGraphics graphics, int x, int y);

   /** Page number colour (RGB). */
   int accentColor();

   record Bounds(int x, int y, int width, int height) {
   }
}
