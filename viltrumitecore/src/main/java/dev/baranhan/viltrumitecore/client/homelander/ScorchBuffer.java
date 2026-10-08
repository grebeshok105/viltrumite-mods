package dev.baranhan.viltrumitecore.client.homelander;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/** Bounded, time-limited set of scorch keys (spec §5.1). Oldest drops first. Pure, render thread only. */
public final class ScorchBuffer<K> {
   private final int max;
   private final long lifetime;
   private final LinkedHashMap<K, Long> born = new LinkedHashMap<>();

   public ScorchBuffer(int max, long lifetime) {
      this.max = max;
      this.lifetime = lifetime;
   }

   public void add(K key, long now) {
      this.born.remove(key);
      this.born.put(key, now);
      while (this.born.size() > this.max) {
         Iterator<K> it = this.born.keySet().iterator();
         it.next();
         it.remove();
      }
   }

   public void expire(long now) {
      this.born.values().removeIf(t -> now - t >= this.lifetime);
   }

   public boolean contains(K key) {
      return this.born.containsKey(key);
   }

   public long bornAt(K key) {
      Long t = this.born.get(key);
      return t == null ? -1L : t;
   }

   public int size() {
      return this.born.size();
   }

   public Iterable<Map.Entry<K, Long>> entries() {
      return this.born.entrySet();
   }

   public void removeIf(java.util.function.Predicate<K> dead) {
      this.born.keySet().removeIf(dead);
   }

   public void clear() {
      this.born.clear();
   }
}
