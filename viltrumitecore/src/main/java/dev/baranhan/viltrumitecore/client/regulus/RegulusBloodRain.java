package dev.baranhan.viltrumitecore.client.regulus;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * Madness screen blood, procedural: drops gather in a thin wet band along the
 * top edge of the screen and run down like rain on glass — stop-and-go,
 * speeding up as they grow, leaving a thin trail that slowly dries out.
 * Denser near the sides so the centre of the view stays readable, and
 * translucent overall. Simulated in real time (frame-rate independent).
 */
public final class RegulusBloodRain {
   private static final List<Drop> DROPS = new ArrayList<>();
   private static final Random RANDOM = new Random();
   private static final int MAX_DROPS = 34;
   private static long lastNanos;
   private static float spawnBudget;
   private static float presence;
   private static int lastWidth;

   private RegulusBloodRain() {
   }

   public static void reset() {
      DROPS.clear();
      presence = 0.0F;
      lastNanos = 0L;
   }

   /** One frame: simulate, then draw. {@code active} fades the whole effect in/out. */
   public static void render(GuiGraphics g, int width, int height, boolean active, float pulse) {
      long now = System.nanoTime();
      float dt = lastNanos == 0L ? 0.0F : Math.min(0.1F, (now - lastNanos) / 1.0E9F);
      lastNanos = now;
      presence = Mth.clamp(presence + (active ? dt * 0.8F : -dt * 0.6F), 0.0F, 1.0F);
      if (presence <= 0.0F) {
         DROPS.clear();
         return;
      }

      if (width != lastWidth) {
         lastWidth = width;
         DROPS.clear();
      }

      simulate(dt, width, height, active, pulse);
      float alpha = presence * (0.5F + 0.15F * pulse);
      drawBand(g, width, alpha, now);
      for (Drop drop : DROPS) {
         drawDrop(g, drop, alpha);
      }
   }

   private static void simulate(float dt, int width, int height, boolean active, float pulse) {
      if (active) {
         // Heartbeat pushes a few extra drops over the edge.
         spawnBudget += dt * (5.0F + 6.0F * pulse);
         while (spawnBudget >= 1.0F && DROPS.size() < MAX_DROPS) {
            spawnBudget -= 1.0F;
            DROPS.add(spawn(width));
         }
         spawnBudget = Math.min(spawnBudget, 2.0F);
      }

      for (int i = DROPS.size() - 1; i >= 0; i--) {
         Drop d = DROPS.get(i);
         d.age += dt;
         if (d.pause > 0.0F) {
            d.pause -= dt;
         } else {
            // Bigger drops run faster; friction on the glass makes them stall.
            float target = 25.0F + d.size * 28.0F;
            d.speed = Mth.lerp(Math.min(1.0F, dt * 3.0F), d.speed, target);
            d.y += d.speed * dt;
            d.x += Mth.sin(d.age * d.wobbleRate + d.phase) * d.wobble * dt;
            if (RANDOM.nextFloat() < dt * 0.9F) {
               d.pause = 0.15F + RANDOM.nextFloat() * 0.7F;
               d.speed *= 0.2F;
            }
            // The drop slowly loses mass into its trail.
            d.size = Math.max(0.6F, d.size - dt * 0.05F);
         }

         boolean gone = d.y - d.trail() > height + 4;
         if (gone || d.age > 14.0F) {
            DROPS.remove(i);
         }
      }
   }

   private static Drop spawn(int width) {
      Drop d = new Drop();
      // Edge-weighted: most drops run near the sides, a few cross the centre.
      float edge = (float)Math.pow(RANDOM.nextFloat(), 1.7);
      float half = width * 0.5F;
      d.x = RANDOM.nextBoolean() ? edge * half : width - edge * half;
      d.y = -2.0F - RANDOM.nextFloat() * 4.0F;
      d.startY = d.y;
      d.size = 1.0F + RANDOM.nextFloat() * 1.4F;
      d.speed = 0.0F;
      d.pause = RANDOM.nextFloat() * 0.6F;
      d.wobble = RANDOM.nextFloat() * 3.0F;
      d.wobbleRate = 1.0F + RANDOM.nextFloat() * 2.0F;
      d.phase = RANDOM.nextFloat() * 6.28F;
      return d;
   }

   /** The wet rim along the top edge, gently uneven. */
   private static void drawBand(GuiGraphics g, int width, float alpha, long now) {
      float t = (now / 1.0E9F) * 0.4F;
      for (int x = 0; x < width; x += 2) {
         float n = Mth.sin(x * 0.07F + t) * 0.5F + Mth.sin(x * 0.023F - t * 0.6F) * 0.5F;
         float edgeBoost = 1.0F + 1.4F * (float)Math.pow(Math.abs(x - width * 0.5F) / (width * 0.5F), 2.0);
         int h = Math.max(1, (int)((2.0F + 2.2F * n) * edgeBoost));
         g.fillGradient(x, 0, x + 2, h, argb(alpha * 0.9F, 0x7A0710), argb(alpha * 0.25F, 0x9C0E18));
      }
   }

   private static void drawDrop(GuiGraphics g, Drop d, float alpha) {
      int w = Math.max(1, Math.round(d.size));
      int x = Math.round(d.x - w * 0.5F);
      int head = Math.round(d.y);
      int top = Math.round(Math.max(0.0F, d.y - d.trail()));
      // Trail: thin and drying out from the top down.
      if (head - top > 1) {
         int tw = Math.max(1, w - 1);
         int tx = x + (w - tw) / 2;
         g.fillGradient(tx, top, tx + tw, head - 1, argb(alpha * 0.18F, 0x6E0610), argb(alpha * 0.6F, 0x8A0A14));
      }

      // Head: a rounded bead, darker rim, one glossy pixel.
      int hh = w + 1 + Math.round(d.size);
      g.fill(x, head - hh + 1, x + w, head + 1, argb(alpha * 0.85F, 0x8E0B16));
      g.fill(x - (w > 1 ? 1 : 0), head - hh / 2, x + w + (w > 1 ? 1 : 0), head, argb(alpha * 0.7F, 0x6A050D));
      g.fill(x, head - hh + 1, x + 1, head - hh + 2, argb(alpha * 0.65F, 0xFF7070));
   }

   private static int argb(float alpha, int rgb) {
      int a = Mth.clamp((int)(alpha * 255.0F), 0, 255);
      return a << 24 | rgb;
   }

   private static final class Drop {
      float x;
      float y;
      float startY;
      float size;
      float speed;
      float pause;
      float wobble;
      float wobbleRate;
      float phase;
      float age;

      /** Trail length: everything travelled, minus what has already dried. */
      float trail() {
         float travelled = this.y - this.startY;
         return Math.max(0.0F, Math.min(travelled, 70.0F + this.size * 30.0F - this.age * 6.0F));
      }
   }
}
