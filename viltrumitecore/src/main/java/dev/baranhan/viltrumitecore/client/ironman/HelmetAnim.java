package dev.baranhan.viltrumitecore.client.ironman;

import dev.baranhan.viltrumitecore.client.anim.render.RevealMask;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import dev.baranhan.viltrumitecore.hero.ironman.IronManFlags;
import dev.baranhan.viltrumitecore.hero.ironman.IronManRules;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

/**
 * Client helmet fold animation (spec §10): follows the synced HELMET_CLOSED
 * bit over {@link IronManRules#HELMET_TOGGLE_TICKS}. The mask reuses the
 * baked reveal frames along the head bone (RevealMask: the head reveals last),
 * so the nanites flow over the face the same way as in the suit wave.
 */
@EventBusSubscriber(
   modid = "viltrumitecore",
   bus = Bus.FORGE,
   value = {Dist.CLIENT}
)
public final class HelmetAnim {
   private static final WeakHashMap<Entity, float[]> STATE = new WeakHashMap<>();

   private HelmetAnim() {
   }

   public static boolean closedFlag(HeroPublicSnapshot snapshot) {
      return IronManFlags.is(snapshot.heroFlags(), IronManFlags.Field.HELMET_CLOSED);
   }

   /** One tick toward the target (pure). */
   public static float step(float progress, boolean closed) {
      float delta = 1.0F / IronManRules.HELMET_TOGGLE_TICKS;
      return closed ? Math.min(1.0F, progress + delta) : Math.max(0.0F, progress - delta);
   }

   /** Helmet closure 0 (open, face visible) .. 1 (closed). */
   public static float progress(Entity entity, HeroPublicSnapshot snapshot, float partialTick) {
      float[] s = STATE.get(entity);
      if (s == null) {
         return closedFlag(snapshot) ? 1.0F : 0.0F;
      }

      return s[0] + (s[1] - s[0]) * partialTick;
   }

   /** 0..1 through a running toggle, -1 when still. */
   public static float toggleProgress(Entity entity) {
      float[] s = STATE.get(entity);
      if (s == null || s[0] == s[1]) {
         return -1.0F;
      }

      return s[2] > 0.5F ? s[1] : 1.0F - s[1];
   }

   /** Helmet part frame: reveal frames from the head start up to the full mask (0 = not drawn). */
   public static int frame(float progress) {
      if (progress <= 0.0F) {
         return 0;
      }

      int start = RevealMask.field().headStartFrame();
      return Math.min(RevealMask.FRAMES, start + Math.round(progress * (RevealMask.FRAMES - start)));
   }

   @SubscribeEvent
   public static void onClientTick(TickEvent.ClientTickEvent event) {
      Minecraft client = Minecraft.getInstance();
      if (event.phase != TickEvent.Phase.END || client.level == null || client.isPaused()) {
         return;
      }

      for (Player player : client.level.players()) {
         HeroPublicSnapshot snapshot = IronManView.of(player);
         if (snapshot == null) {
            STATE.remove(player);
            continue;
         }

         boolean closed = closedFlag(snapshot);
         float[] s = STATE.computeIfAbsent(player, p -> new float[]{closed ? 1.0F : 0.0F, closed ? 1.0F : 0.0F, closed ? 1.0F : 0.0F});
         s[0] = s[1];
         s[1] = step(s[1], closed);
         s[2] = closed ? 1.0F : 0.0F;
      }
   }
}
