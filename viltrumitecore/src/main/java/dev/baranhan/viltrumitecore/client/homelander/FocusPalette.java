package dev.baranhan.viltrumitecore.client.homelander;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-target focus colours (spec §6.4): 8 fixed colours, none red (red is the
 * laser). A colour sticks to an entity id while it stays a target; freed
 * slots go to newcomers.
 */
public final class FocusPalette {
   public static final int SIZE = 8;
   private static final int[] COLORS = {
      0x55FFFF, // cyan
      0xFFE14D, // yellow
      0x66FF66, // green
      0xFF66FF, // magenta
      0xFFA033, // orange
      0x5A9BFF, // blue
      0xB487FF, // violet
      0xF2F2F2  // white
   };
   private final Map<Integer, Integer> slots = new HashMap<>();

   public static int colorFor(int slot) {
      return COLORS[Math.floorMod(slot, SIZE)];
   }

   /** Replace the target list; existing targets keep their slot. */
   public void update(List<Integer> ids) {
      this.slots.keySet().retainAll(ids);
      for (int id : ids) {
         if (this.slots.containsKey(id)) {
            continue;
         }

         for (int slot = 0; slot < SIZE; slot++) {
            if (!this.slots.containsValue(slot)) {
               this.slots.put(id, slot);
               break;
            }
         }
      }
   }

   /** RGB colour of a target, or -1 when the id is not a target. */
   public int colorOf(int id) {
      Integer slot = this.slots.get(id);
      return slot == null ? -1 : colorFor(slot);
   }

   public boolean isEmpty() {
      return this.slots.isEmpty();
   }

   public void clear() {
      this.slots.clear();
   }
}
