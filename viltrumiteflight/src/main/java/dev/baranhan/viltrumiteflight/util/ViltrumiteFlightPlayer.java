package dev.baranhan.viltrumiteflight.util;

public interface ViltrumiteFlightPlayer {
   FlightState getFlightState();

   void setFlightState(FlightState var1);

   float getFlightThrottle();

   void setFlightThrottle(float var1);

   float getMaxFlightSpeed();

   void setMaxFlightSpeed(float var1);

   float getThrottleSpeed();

   void setThrottleSpeed(float var1);

   boolean isFlightAccelerating();

   void setFlightAccelerating(boolean var1);

   float getLerpedFlightThrottle(float var1);

   float getHoverForward();

   void setHoverForward(float var1);

   float getHoverSideways();

   void setHoverSideways(float var1);

   boolean isSpeedLocked();

   void setSpeedLocked(boolean var1);

   void stopFlight();

   default void resetModFlight() {
      this.setFlightState(FlightState.NONE);
      this.setFlightThrottle(0.0F);
      this.setFlightAccelerating(false);
      this.setHoverForward(0.0F);
      this.setHoverSideways(0.0F);
      this.setSpeedLocked(false);
      this.setFlightTicks(0);
      this.setTakeoffTicks(0);
   }

   void handleFlightCollision();

   int getFlightTicks();

   void setFlightTicks(int var1);

   int getTakeoffTicks();

   void setTakeoffTicks(int var1);

   boolean isClientLocalPlayer();

   void setClientLocalPlayer(boolean var1);
}
