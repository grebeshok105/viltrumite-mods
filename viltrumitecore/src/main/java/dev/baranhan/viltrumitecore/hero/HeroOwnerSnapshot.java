package dev.baranhan.viltrumitecore.hero;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Owner-private hero snapshot: data only the hero's own client may see, in
 * independent typed sections ({@link OwnerSection}). A section update replaces
 * only that section. {@link #carrierEntityIds()} is the CARRIERS view
 * (Regulus heart carriers, Homelander focus targets). Sent to the owner only.
 */
public final class HeroOwnerSnapshot {
   public static final HeroOwnerSnapshot EMPTY = new HeroOwnerSnapshot(new EnumMap<>(OwnerSection.class));
   private final Map<OwnerSection, Section> sections;

   /**
    * One section: entity ids and, per id, ticks until it expires on the
    * client (0 = no expiry). Arrays of equal length.
    */
   public record Section(int[] ids, int[] expireTicks) {
      public static final Section EMPTY = new Section(new int[0], new int[0]);

      public Section {
         ids = ids == null ? new int[0] : ids;
         expireTicks = expireTicks == null || expireTicks.length != ids.length ? new int[ids.length] : expireTicks;
      }

      public static Section of(int[] ids) {
         return new Section(ids, new int[ids == null ? 0 : ids.length]);
      }

      public boolean isEmpty() {
         return this.ids.length == 0;
      }

      @Override
      public boolean equals(Object other) {
         return other instanceof Section section && Arrays.equals(this.ids, section.ids) && Arrays.equals(this.expireTicks, section.expireTicks);
      }

      @Override
      public int hashCode() {
         return 31 * Arrays.hashCode(this.ids) + Arrays.hashCode(this.expireTicks);
      }
   }

   /** CARRIERS only (the pre-section shape). */
   public HeroOwnerSnapshot(int[] carrierEntityIds) {
      this(single(OwnerSection.CARRIERS, Section.of(carrierEntityIds)));
   }

   private HeroOwnerSnapshot(Map<OwnerSection, Section> sections) {
      this.sections = sections;
   }

   private static Map<OwnerSection, Section> single(OwnerSection section, Section value) {
      Map<OwnerSection, Section> map = new EnumMap<>(OwnerSection.class);
      if (!value.isEmpty()) {
         map.put(section, value);
      }

      return map;
   }

   public Section section(OwnerSection section) {
      return this.sections.getOrDefault(section, Section.EMPTY);
   }

   /** A copy with one section replaced; the others stay. */
   public HeroOwnerSnapshot with(OwnerSection section, Section value) {
      Map<OwnerSection, Section> map = new EnumMap<>(OwnerSection.class);
      map.putAll(this.sections);
      if (value == null || value.isEmpty()) {
         map.remove(section);
      } else {
         map.put(section, value);
      }

      return new HeroOwnerSnapshot(map);
   }

   /** The CARRIERS view. */
   public int[] carrierEntityIds() {
      return this.section(OwnerSection.CARRIERS).ids();
   }

   @Override
   public boolean equals(Object other) {
      if (this == other) {
         return true;
      }

      if (!(other instanceof HeroOwnerSnapshot snapshot)) {
         return false;
      }

      for (OwnerSection section : OwnerSection.values()) {
         if (!Objects.equals(this.section(section), snapshot.section(section))) {
            return false;
         }
      }

      return true;
   }

   @Override
   public int hashCode() {
      int result = 1;
      for (OwnerSection section : OwnerSection.values()) {
         result = 31 * result + this.section(section).hashCode();
      }

      return result;
   }
}
