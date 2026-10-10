package dev.baranhan.viltrumitecore.client.ironman.mark;

import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkId;
import javax.annotation.Nullable;

/**
 * Primary plate colour of each suit, measured from the dominant colour of the
 * mark skins (tools/assets/make_ironman_forearm_launchers.py notes), slightly
 * lifted for the near-black suits so the tinted parts keep their shading.
 * Tints the light-grayscale forearm launchers and the empty-suit lining.
 */
public final class SuitPalette {
   /** Nano suit (Mark 50 skin). */
   public static final int NANO = 0xA3141E;

   private SuitPalette() {
   }

   public static int of(@Nullable MarkId mark) {
      if (mark == null) {
         return NANO;
      }

      return switch (mark) {
         case MARK_7 -> 0xA3141E;
         case MARK_42 -> 0x8A1C2E;
         case MARK_15 -> 0x2E2E33;
         case MARK_39 -> 0xD6D6DA;
         case MARK_17 -> 0xA0121D;
         case WAR_MACHINE_MK2 -> 0x3C3D40;
         case IRON_HEART_MK3 -> 0xC8C8CC;
      };
   }

   /** Colour of the suit the snapshot shows: the worn (or arriving) mark, otherwise the nano suit. */
   public static int of(HeroPublicSnapshot snapshot) {
      MarkState mark = MarkState.of(snapshot);
      return of(mark.markOn() ? mark.mark() : null);
   }

   /** Lining: a darker shade of the plates (the inside of the shell is in shadow). */
   public static int lining(@Nullable MarkId mark) {
      int c = of(mark);
      return scale(c, 0.8F);
   }

   public static int scale(int rgb, float k) {
      int r = Math.min(255, Math.round(((rgb >> 16) & 255) * k));
      int g = Math.min(255, Math.round(((rgb >> 8) & 255) * k));
      int b = Math.min(255, Math.round((rgb & 255) * k));
      return r << 16 | g << 8 | b;
   }

   public static float r(int rgb) {
      return ((rgb >> 16) & 255) / 255.0F;
   }

   public static float g(int rgb) {
      return ((rgb >> 8) & 255) / 255.0F;
   }

   public static float b(int rgb) {
      return (rgb & 255) / 255.0F;
   }
}
