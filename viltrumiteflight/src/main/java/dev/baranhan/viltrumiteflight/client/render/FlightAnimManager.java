package dev.baranhan.viltrumiteflight.client.render;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FlightAnimManager {
   private static final Map<UUID, FlightAnimManager.AnimState> STATES = new HashMap<>();

   public static FlightAnimManager.AnimState getState(UUID uuid) {
      return STATES.computeIfAbsent(uuid, k -> new FlightAnimManager.AnimState());
   }

   public static class AnimState {
      public float currentPitch = 0.0F;
      public float currentRoll = 0.0F;
      public float transition = 0.0F;
      public float lastAge = -1.0F;
      public float deltaSeconds = 0.016F;
      public float lastRenderYaw = 0.0F;
      public float smoothedTurnSpeed = 0.0F;
      public boolean wasFlying = false;
      public float smoothedHoverForward = 0.0F;
      public float smoothedHoverSideways = 0.0F;
      public float rippleTime = -1.0F;
      public boolean wasAboveSonic = false;
      public float hoverTime = 0.0F;
      public float currentSupermanFactor = 0.0F;
      public float currentDescendFactor = 0.0F;
      public float smoothedMovePitch = 0.0F;
      public float movePitchVelocity = 0.0F;
      public float smoothedMoveRoll = 0.0F;
      public float moveRollVelocity = 0.0F;
      public float lastLookYaw = 0.0F;
      public float smoothedYawVelocity = 0.0F;
   }
}
