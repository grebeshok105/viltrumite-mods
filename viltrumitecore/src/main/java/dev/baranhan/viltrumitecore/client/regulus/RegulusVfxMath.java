package dev.baranhan.viltrumitecore.client.regulus;

import dev.baranhan.viltrumitecore.hero.regulus.RegulusRules;

/**
 * Pure curves behind the Regulus world VFX and screen uniforms. Presentation
 * approximations only — nothing here feeds back into gameplay.
 */
public final class RegulusVfxMath {
   /** Heartbeat cycle length in ticks (~0.9s lub-dub). */
   public static final int HEARTBEAT_PERIOD_TICKS = 18;
   /** Heart flash after a heart burns, in ticks. */
   public static final float HEART_FLASH_TICKS = 12.0F;

   private RegulusVfxMath() {
   }

   /**
    * Camera-shake factor for entities standing inside the debris cone.
    * The cone half-angle comes from the 35-degree apex spec constant.
    */
   public static float debrisVictimFactor(double distance, double angleDegrees) {
      double halfAngle = RegulusRules.DEBRIS_CONE_DEGREES * 0.5;
      if (angleDegrees > halfAngle || distance >= RegulusRules.DEBRIS_RANGE) {
         return 0.0F;
      }

      return (float)(1.0 - distance / RegulusRules.DEBRIS_RANGE);
   }

   /** Overheat screen noise ramps over the first second of overheating. */
   public static float overheatFactor(int overheatTicks) {
      if (overheatTicks <= 0) {
         return 0.0F;
      }

      return Math.min(1.0F, overheatTicks / 20.0F);
   }

   /** Red edge flash when a heart burns: 12 ticks to zero. */
   public static float heartFlashFactor(float ticksSinceLoss) {
      if (ticksSinceLoss < 0.0F) {
         return 0.0F;
      }

      return Math.max(0.0F, 1.0F - ticksSinceLoss / HEART_FLASH_TICKS);
   }

   /**
    * Lub-dub heartbeat envelope over one period, normalized to [0,1].
    * phase in [0,1): strong beat ~0.08, soft echo ~0.32, quiet tail.
    */
   public static float madnessPulse(float phase) {
      float beat = bump(phase, 0.08F, 0.16F);
      float echo = 0.55F * bump(phase, 0.34F, 0.14F);
      return Math.min(1.0F, beat + echo);
   }

   private static float bump(float phase, float center, float width) {
      float d = Math.abs(phase - center);
      if (d >= width) {
         return 0.0F;
      }

      float x = 1.0F - d / width;
      return x * x;
   }

   /** Dome shell alpha: fades in over 6 ticks, out over the last 8. */
   public static float domeAlpha(long gameTime, long createdAt, long expiresAt) {
      float in = Math.min(1.0F, Math.max(0.0F, (float)(gameTime - createdAt) / 6.0F));
      float out = expiresAt <= createdAt
         ? 0.0F
         : Math.min(1.0F, Math.max(0.0F, (float)(expiresAt - gameTime) / 8.0F));
      return Math.min(in, out);
   }

   /**
    * Deterministic per-particle offset in [0,1) so suspended dust fields stay
    * frozen in place instead of jittering every frame.
    */
   public static float hashOffset(int seed, int axis) {
      int h = seed * 0x9E3779B9 + axis * 0x85EBCA6B;
      h ^= h >>> 13;
      h *= 0xC2B2AE35;
      h ^= h >>> 16;
      return (h & 0xFFFF) / 65536.0F;
   }

   /** Orbit angle for rune index i of 8 around the ritual caster. */
   public static float runeAngle(int index, float timeSeconds) {
      return (float)(index * (Math.PI * 2.0 / 8.0)) + timeSeconds * 0.9F;
   }
}
