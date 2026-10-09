package dev.baranhan.viltrumitecore.client.ironman;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import dev.baranhan.viltrumitecore.hero.ironman.IronManFlags;
import dev.baranhan.viltrumitecore.hero.ironman.IronManRules;
import java.io.IOException;
import java.io.InputStream;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.slf4j.Logger;

/**
 * Nano damage and repair (spec §4.2), visual only. A heavy hit sets a zone
 * bit (DAMAGED_ZONES: 0 helmet, 1 right shoulder, 2 chest); the zone shows
 * broken suit pixels with Tony's skin underneath and a cyan nanite edge, and
 * the holes close over NANO_REPAIR_TICKS. Textures are baked once per
 * resource reload: 7 zone masks x {@link #LEVELS} repair levels, skin layout
 * (drawn as a cutout pass over the body and over the helmet geo).
 */
public final class NanoDamageVisuals implements ResourceManagerReloadListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final int LEVELS = 4;
   private static final float DENSITY = 0.55F;
   public static final NanoDamageVisuals INSTANCE = new NanoDamageVisuals();
   private final ResourceLocation[][] textures = new ResourceLocation[8][LEVELS];
   private final WeakHashMap<AbstractClientPlayer, long[]> since = new WeakHashMap<>();
   private boolean built;
   private boolean failed;

   private NanoDamageVisuals() {
   }

   @Override
   public void onResourceManagerReload(ResourceManager manager) {
      this.built = false;
      this.failed = false;
   }

   /** Skin-layout rectangles {x0, y0, x1, y1} of a zone (both skin layers). */
   static int[][] zone(int bit) {
      return switch (bit) {
         case 0 -> new int[][]{{0, 0, 32, 16}, {32, 0, 64, 16}};
         case 1 -> new int[][]{{40, 16, 56, 26}, {40, 32, 56, 42}};
         default -> new int[][]{{16, 20, 40, 32}, {16, 36, 40, 48}};
      };
   }

   /** Blocky per-texel noise 0..1 (2x2 cells), stable across bakes. */
   static float noise(int x, int y) {
      int h = (x / 2) * 73856093 ^ (y / 2) * 19349663 ^ 0x5bd1e995;
      h ^= h >>> 13;
      h *= 0x85ebca6b;
      h ^= h >>> 16;
      return (h & 0xFFFF) / 65536.0F;
   }

   /** A texel is a hole at repair level 0..LEVELS-1 (holes shrink as the level grows). */
   static boolean hole(int mask, int level, int x, int y) {
      boolean inZone = false;
      for (int bit = 0; bit < 3 && !inZone; bit++) {
         if ((mask & 1 << bit) == 0) {
            continue;
         }

         for (int[] r : zone(bit)) {
            if (x >= r[0] && x < r[2] && y >= r[1] && y < r[3]) {
               inZone = true;
               break;
            }
         }
      }

      return inZone && noise(x, y) < DENSITY * (1.0F - level / (float)LEVELS);
   }

   /** Repair level from ticks since the zone mask appeared. */
   public static int level(long elapsedTicks) {
      int level = (int)(elapsedTicks * LEVELS / Math.max(1, IronManRules.NANO_REPAIR_TICKS));
      return Math.max(0, Math.min(LEVELS - 1, level));
   }

   @Nullable
   public ResourceLocation texture(AbstractClientPlayer player, HeroPublicSnapshot snapshot) {
      if (dev.baranhan.viltrumitecore.client.ironman.mark.MarkState.of(snapshot).markOn()) {
         this.since.remove(player);
         return null;
      }

      int mask = IronManFlags.get(snapshot.heroFlags(), IronManFlags.Field.DAMAGED_ZONES);
      if (mask == 0 || !IronManView.worn(snapshot)) {
         this.since.remove(player);
         return null;
      }

      long now = player.level().getGameTime();
      long[] entry = this.since.computeIfAbsent(player, p -> new long[]{-1L, now});
      if (entry[0] != mask) {
         // New zone: the repair restarts (the server restarts its 60 t too).
         entry[0] = mask;
         entry[1] = now;
      }

      if (!this.ensure()) {
         return null;
      }

      return this.textures[mask][level(now - entry[1])];
   }

   private boolean ensure() {
      if (this.built) {
         return true;
      }

      if (this.failed) {
         return false;
      }

      Minecraft minecraft = Minecraft.getInstance();
      try (InputStream stream = minecraft.getResourceManager().open(IronManClient.SKIN); NativeImage tony = NativeImage.read(stream)) {
         this.bake(minecraft.getTextureManager(), tony);
         this.built = true;
         return true;
      } catch (IOException | RuntimeException exception) {
         LOGGER.error("Iron Man nano damage textures could not be built", exception);
         this.failed = true;
         return false;
      }
   }

   private void bake(TextureManager manager, NativeImage tony) {
      for (int mask = 1; mask < 8; mask++) {
         for (int level = 0; level < LEVELS; level++) {
            NativeImage out = new NativeImage(64, 64, true);
            for (int y = 0; y < 64; y++) {
               for (int x = 0; x < 64; x++) {
                  int color = 0;
                  if (hole(mask, level, x, y)) {
                     int skin = x < tony.getWidth() && y < tony.getHeight() ? tony.getPixelRGBA(x, y) : 0;
                     color = (skin >>> 24) == 0 ? 0 : skin;
                  } else if (edge(mask, level, x, y)) {
                     // Nanite crawl at the hole edge (ABGR cyan).
                     color = 0xFFFFE070;
                  }

                  out.setPixelRGBA(x, y, color);
               }
            }

            ResourceLocation location = new ResourceLocation("viltrumitecore", "dynamic/ironman/damage_" + mask + "_" + level);
            manager.release(location);
            manager.register(location, new DynamicTexture(out));
            this.textures[mask][level] = location;
         }
      }
   }

   private static boolean edge(int mask, int level, int x, int y) {
      return hole(mask, level, x + 1, y) || hole(mask, level, x - 1, y) || hole(mask, level, x, y + 1) || hole(mask, level, x, y - 1);
   }
}
