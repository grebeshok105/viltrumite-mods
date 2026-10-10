package dev.baranhan.viltrumitecore.client.ironman.veronica;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class PartFlightTest {
   private static final Vec3 LAUNCH = new Vec3(0.0, 40.0, 0.0);
   private static final Vec3 ANCHOR = new Vec3(3.0, 1.0, -2.0);

   @Test
   void flightStartsAtLaunchAndArrivesAtAnchor() {
      Vec3 start = PartFlight.position(LAUNCH, ANCHOR, 0.0F);
      Vec3 end = PartFlight.position(LAUNCH, ANCHOR, 1.0F);
      assertEquals(LAUNCH.x, start.x, 1.0E-9);
      assertEquals(LAUNCH.y, start.y, 1.0E-9);
      assertEquals(ANCHOR.x, end.x, 1.0E-9);
      assertEquals(ANCHOR.y, end.y, 1.0E-9);
      assertEquals(ANCHOR.z, end.z, 1.0E-9);
   }

   @Test
   void tumbleEndsAlignedWithTheBody() {
      assertEquals(1.0F, PartFlight.tumble(0.0F), 1.0E-6F);
      assertEquals(0.0F, PartFlight.tumble(1.0F), 1.0E-6F);
   }
}
