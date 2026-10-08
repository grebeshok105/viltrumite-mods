package dev.baranhan.viltrumitecore.client.homelander;

import dev.baranhan.viltrumitecore.effect.ViltrumiteEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

/**
 * The watched player's side of focus (spec §6.4): dark pulsing vignette,
 * heartbeat, and a title once per fear episode.
 */
@EventBusSubscriber(
   modid = "viltrumitecore",
   bus = Bus.FORGE,
   value = {Dist.CLIENT}
)
public final class FearClient {
   private static final int HEARTBEAT_TICKS = 18;
   /** A new episode needs this many fear-free ticks first. */
   private static final int EPISODE_GAP = 200;
   private static boolean afraid;
   private static int calmTicks = EPISODE_GAP;
   private static int beat;
   private static float strength;

   private FearClient() {
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent event) {
      if (event.phase != Phase.END) {
         return;
      }

      Minecraft client = Minecraft.getInstance();
      LocalPlayer player = client.player;
      boolean now = player != null && player.hasEffect(ViltrumiteEffects.FEAR.get());
      if (now && !afraid) {
         client.gui.setTimes(10, 50, 20);
         client.gui.setTitle(Component.empty());
         client.gui.setSubtitle(Component.translatable("hud.viltrumitecore.homelander.fear"));
      }

      afraid = now;
      calmTicks = now ? 0 : Math.min(EPISODE_GAP, calmTicks + 1);
      if (now && !client.isPaused() && beat-- <= 0) {
         beat = HEARTBEAT_TICKS;
         player.playNotifySound(SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.9F, 1.15F);
      }
   }

   @SubscribeEvent
   public static void onRenderGui(RenderGuiEvent.Post event) {
      Minecraft client = Minecraft.getInstance();
      strength = Mth.lerp(0.08F, strength, afraid ? 1.0F : 0.0F);
      if (strength < 0.01F || client.player == null) {
         return;
      }

      float phase = ((client.player.tickCount + event.getPartialTick()) % HEARTBEAT_TICKS) / HEARTBEAT_TICKS;
      float pulse = 0.75F + 0.25F * (float)Math.exp(-Math.pow((phase - 0.1F) * 9.0F, 2.0));
      float dark = 0.85F * strength * pulse;
      FocusClient.drawVignette(event.getGuiGraphics(), dark, dark, dark);
   }
}
