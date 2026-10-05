package dev.baranhan.viltrumiteflight.client;

import dev.baranhan.viltrumiteflight.client.sound.FlightWindSoundInstance;
import dev.baranhan.viltrumiteflight.config.ViltrumiteConfigClient;
import dev.baranhan.viltrumiteflight.network.ModMessages;
import dev.baranhan.viltrumiteflight.network.packet.FlightAccelerateC2SPacket;
import dev.baranhan.viltrumiteflight.network.packet.FlightSpeedLockC2SPacket;
import dev.baranhan.viltrumiteflight.network.packet.FlightToggleC2SPacket;
import dev.baranhan.viltrumiteflight.network.packet.HoverInputC2SPacket;
import dev.baranhan.viltrumiteflight.registry.ModSounds;
import dev.baranhan.viltrumiteflight.util.FlightState;
import dev.baranhan.viltrumiteflight.util.ViltrumiteFlightPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(
   modid = "viltrumiteflight",
   bus = Bus.FORGE,
   value = {Dist.CLIENT}
)
public class FlightInputHandler {
   private static boolean wasJumpPressed = false;
   private static long lastJumpTime = 0L;
   private static boolean wasAccelerating = false;
   private static float lastForward = 0.0F;
   private static float lastSideways = 0.0F;
   private static FlightWindSoundInstance windSound;
   private static boolean wasSneakPressed = false;

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent event) {
      if (event.phase == Phase.END) {
         Minecraft client = Minecraft.getInstance();
         if (client.player != null) {
            ViltrumiteFlightPlayer omniPlayer = (ViltrumiteFlightPlayer)client.player;
            omniPlayer.setClientLocalPlayer(true);
            boolean isJumpPressed = client.options.keyJump.isDown();
            if (isJumpPressed && !wasJumpPressed) {
               long now = System.currentTimeMillis();
               if (now - lastJumpTime < 300L) {
                  if (client.player.isCrouching() && omniPlayer.getFlightState() == FlightState.NONE && client.player.getAbilities().mayfly) {
                     omniPlayer.setTakeoffTicks(5);
                  }

                  ModMessages.sendToServer(new FlightToggleC2SPacket());
                  lastJumpTime = 0L;
               } else {
                  lastJumpTime = now;
               }
            }

            wasJumpPressed = isJumpPressed;
            if (omniPlayer.getFlightState() != FlightState.NONE) {
               boolean isAccelerating = client.options.keySprint.isDown() && !client.player.horizontalCollision;
               omniPlayer.setFlightAccelerating(isAccelerating);
               if (isAccelerating != wasAccelerating) {
                  ModMessages.sendToServer(new FlightAccelerateC2SPacket(isAccelerating));
                  wasAccelerating = isAccelerating;
               }
            } else {
               wasAccelerating = false;
            }

            float forward = client.player.input.up ? 1.0F : (client.player.input.down ? -1.0F : 0.0F);
            float sideways = client.player.input.left ? 1.0F : (client.player.input.right ? -1.0F : 0.0F);
            if (forward != lastForward || sideways != lastSideways) {
               lastForward = forward;
               lastSideways = sideways;
               omniPlayer.setHoverForward(forward);
               omniPlayer.setHoverSideways(sideways);
               ModMessages.sendToServer(new HoverInputC2SPacket(forward, sideways));
            }

            if (ViltrumiteConfigClient.INSTANCE.enableWindLoopSound
               && omniPlayer.getFlightState() != FlightState.NONE
               && omniPlayer.getFlightThrottle() > 0.2F
               && (windSound == null || windSound.isStopped())) {
               windSound = new FlightWindSoundInstance(client.player, (SoundEvent)ModSounds.WIND_LOOP.get());
               client.getSoundManager().play(windSound);
            }

            boolean isSneakPressed = client.options.keyShift.isDown();
            if (isSneakPressed && !wasSneakPressed && omniPlayer.getFlightState() != FlightState.NONE && omniPlayer.getFlightThrottle() > 0.2F) {
               boolean currentLock = omniPlayer.isSpeedLocked();
               omniPlayer.setSpeedLocked(!currentLock);
               ModMessages.sendToServer(new FlightSpeedLockC2SPacket());
            }

            wasSneakPressed = isSneakPressed;
         }
      }
   }
}
