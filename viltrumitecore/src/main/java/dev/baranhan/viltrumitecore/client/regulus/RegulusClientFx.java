package dev.baranhan.viltrumitecore.client.regulus;

import net.minecraft.client.Minecraft;

/**
 * Small shared decaying state for Regulus screen FX. The VFX tick detector
 * writes the edges (heart burned, debris cone hit); GameRendererDashMixin and
 * the HUD read them every frame. Reset whenever the level unloads.
 */
public final class RegulusClientFx {
   /** Counts down from RegulusVfxMath.HEART_FLASH_TICKS after a heart burns. */
   public static int heartFlashTicks;
   /** Counts down while the debris kick shake is applied to the camera. */
   public static int debrisShakeTicks;
   /** Cone factor of the most recent debris shake (0..1). */
   public static float debrisShakePower;
   /** Last game tick the madness heartbeat was played on. */
   public static long lastHeartbeatTick = -1L;

   private RegulusClientFx() {
   }

   public static void tickClient(Minecraft client) {
      if (client.level == null) {
         reset();
         return;
      }

      if (heartFlashTicks > 0) {
         heartFlashTicks--;
      }

      if (debrisShakeTicks > 0) {
         debrisShakeTicks--;
         if (debrisShakeTicks == 0) {
            debrisShakePower = 0.0F;
         }
      }
   }

   public static void reset() {
      heartFlashTicks = 0;
      debrisShakeTicks = 0;
      debrisShakePower = 0.0F;
      lastHeartbeatTick = -1L;
   }
}
