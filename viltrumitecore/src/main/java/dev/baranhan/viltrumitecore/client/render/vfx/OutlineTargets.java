package dev.baranhan.viltrumitecore.client.render.vfx;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.ToIntFunction;
import net.minecraft.world.entity.Entity;

/**
 * Shared through-wall outline on the vanilla glowing path (OutlineGlowMixin /
 * OutlineTeamColorMixin): sources register a colour function (RGB, -1 = not
 * a target). The first source in registration order with a colour wins, so
 * the Homelander focus (registered first) keeps its exact colours.
 */
public final class OutlineTargets {
   private static final List<ToIntFunction<Entity>> SOURCES = new CopyOnWriteArrayList<>();

   private OutlineTargets() {
   }

   public static void register(ToIntFunction<Entity> source) {
      SOURCES.add(source);
   }

   /** RGB outline colour of the entity, -1 when no source claims it. */
   public static int colorOf(Entity entity) {
      for (ToIntFunction<Entity> source : SOURCES) {
         int color = source.applyAsInt(entity);
         if (color >= 0) {
            return color;
         }
      }

      return -1;
   }
}
