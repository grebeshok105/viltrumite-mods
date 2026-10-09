package dev.baranhan.viltrumitecore.client.ironman.mark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkId;
import dev.baranhan.viltrumitecore.hero.ironman.mark.SuitPart;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PlateKeysTest {
   @Test
   void everySuitPartHasItsGeneratedPlate() {
      Path root = Path.of("src/main/resources/assets/viltrumitecore");
      for (MarkId mark : MarkId.values()) {
         for (SuitPart part : SuitPart.of(mark)) {
            Path file = root.resolve("geo/ironman/marks/plates/" + PlateParts.key(part) + ".geo.json");
            assertTrue(Files.isRegularFile(file), file.toString());
         }
      }
   }

   @Test
   void keyNamesBoneSliceAndSide() {
      assertEquals("left_arm_25_100_all", PlateParts.key(SuitPart.NINE.get(2)));
      assertEquals("body_55_100_front", PlateParts.key(SuitPart.FOURTEEN.get(10)));
      assertEquals("body_0_100_back", PlateParts.key(SuitPart.SEVEN.get(5)));
   }
}
