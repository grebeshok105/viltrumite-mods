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

   /** Hand-to-helmet gesture length (ticks): rise, short hold, lower. */
   public static final int GESTURE_TICKS = 22;

   /**
    * Weight 0..1 of the hand-to-helmet gesture. It plays only when the helmet
    * is toggled with the suit staying on (helmet key, or the helmet closing
    * right after the nano formed); never when the suit comes off (exit,
    * retract). Smooth in and out (no snap).
    */
   public static float gesture(Entity entity, float partialTick) {
      float[] s = STATE.get(entity);
      if (s == null || s[3] < 0.0F) {
         return 0.0F;
      }

      float x = Math.min(1.0F, (s[3] + partialTick) / GESTURE_TICKS);
      if (x < 0.36F) {
         return smooth(x / 0.36F);
      }

      if (x < 0.58F) {
         return 1.0F;
      }

      return 1.0F - smooth((x - 0.58F) / 0.42F);
   }

   public static boolean gesturing(Entity entity) {
      float[] s = STATE.get(entity);
      return s != null && s[3] >= 0.0F;
   }

   private static float smooth(float x) {
      x = Math.max(0.0F, Math.min(1.0F, x));
      return x * x * x * (x * (x * 6.0F - 15.0F) + 10.0F);
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
         float[] s = STATE.computeIfAbsent(player, p -> new float[]{closed ? 1.0F : 0.0F, closed ? 1.0F : 0.0F, closed ? 1.0F : 0.0F, -1.0F});
         boolean worn = IronManView.worn(snapshot) && dev.baranhan.viltrumitecore.client.ironman.mark.MarkState.of(snapshot).equipPhase() == 0;
         if (closed != s[2] > 0.5F) {
            // Gesture only for a toggle with the suit on; the suit coming off (exit, retract) gets none.
            s[3] = worn ? 0.0F : -1.0F;
         } else if (s[3] >= 0.0F) {
            s[3] = !worn || s[3] + 1.0F > GESTURE_TICKS ? -1.0F : s[3] + 1.0F;
         }

         s[0] = s[1];
         s[1] = step(s[1], closed);
         s[2] = closed ? 1.0F : 0.0F;
      }
   }
}
