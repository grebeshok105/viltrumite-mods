package dev.baranhan.viltrumitecore.hero;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side store of owner sections, pure (no Minecraft client): each
 * section keeps ids with an absolute expiry in game ticks (0 = none).
 */
public final class OwnerSections {
   private record Entry(int id, long expiresAt) {
   }

   private final Map<OwnerSection, List<Entry>> sections = new EnumMap<>(OwnerSection.class);

   /** Replaces one section; relative expiry ticks become absolute from {@code now}. */
   public void set(OwnerSection section, HeroOwnerSnapshot.Section value, long now) {
      List<Entry> entries = new ArrayList<>(value.ids().length);
      for (int i = 0; i < value.ids().length; i++) {
         int expire = value.expireTicks()[i];
         entries.add(new Entry(value.ids()[i], expire <= 0 ? 0L : now + expire));
      }

      if (entries.isEmpty()) {
         this.sections.remove(section);
      } else {
         this.sections.put(section, entries);
      }
   }

   /** Live ids of a section at {@code now}. */
   public int[] ids(OwnerSection section, long now) {
      List<Entry> entries = this.sections.get(section);
      if (entries == null) {
         return new int[0];
      }

      return entries.stream().filter(e -> e.expiresAt() == 0L || e.expiresAt() > now).mapToInt(Entry::id).toArray();
   }

   public void clear() {
      this.sections.clear();
   }
}
