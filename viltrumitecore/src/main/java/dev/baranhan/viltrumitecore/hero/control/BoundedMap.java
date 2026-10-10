package dev.baranhan.viltrumitecore.hero.control;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Insertion-ordered map that evicts its eldest entry past a fixed capacity.
 * Used for deferred restore bookkeeping so entries for entities that never
 * rejoin cannot accumulate without bound.
 */
public final class BoundedMap<K, V> extends LinkedHashMap<K, V> {
   private final int capacity;

   public BoundedMap(int capacity) {
      super(16, 0.75F, false);
      this.capacity = capacity;
   }

   @Override
   protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
      return this.size() > this.capacity;
   }
}
