package dev.baranhan.viltrumitecore.client.render.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ScorchBufferTest {

   @Test
   void scorchCapDropsOldest() {
      ScorchBuffer<String> buffer = new ScorchBuffer<>(3, 600);
      buffer.add("a", 0);
      buffer.add("b", 1);
      buffer.add("c", 2);
      buffer.add("d", 3);
      assertEquals(3, buffer.size());
      assertFalse(buffer.contains("a"));
      assertTrue(buffer.contains("d"));
   }

   @Test
   void expiresAfterLifetime() {
      ScorchBuffer<String> buffer = new ScorchBuffer<>(256, 600);
      buffer.add("a", 0);
      buffer.add("b", 100);
      buffer.expire(599);
      assertTrue(buffer.contains("a"));
      buffer.expire(600);
      assertFalse(buffer.contains("a"));
      assertTrue(buffer.contains("b"));
   }

   @Test
   void reAddRefreshesWithoutDuplicate() {
      ScorchBuffer<String> buffer = new ScorchBuffer<>(256, 600);
      buffer.add("a", 0);
      buffer.add("a", 500);
      assertEquals(1, buffer.size());
      buffer.expire(700);
      assertTrue(buffer.contains("a"));
      assertEquals(500L, buffer.bornAt("a"));
   }
}
