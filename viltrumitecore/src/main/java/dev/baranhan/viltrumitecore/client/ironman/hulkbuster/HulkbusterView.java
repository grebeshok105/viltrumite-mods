package dev.baranhan.viltrumitecore.client.ironman.hulkbuster;

import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import dev.baranhan.viltrumitecore.hero.ironman.IronManFlags;
import dev.baranhan.viltrumitecore.hero.ironman.hulkbuster.HulkbusterLayer;

/** Hulkbuster phase as the client reads it from the synced snapshot (pure). */
public final class HulkbusterView {
   private HulkbusterView() {
   }

   public static HulkbusterLayer.Phase phase(HeroPublicSnapshot snapshot) {
      int ordinal = IronManFlags.get(snapshot.heroFlags(), IronManFlags.Field.HULKBUSTER_PHASE);
      HulkbusterLayer.Phase[] values = HulkbusterLayer.Phase.values();
      return ordinal < values.length ? values[ordinal] : HulkbusterLayer.Phase.NONE;
   }

   /** Body is replaced by the Mark 48 model (and drawn at 1.7x). */
   public static boolean big(HulkbusterLayer.Phase phase) {
      return phase == HulkbusterLayer.Phase.ACTIVE || phase == HulkbusterLayer.Phase.EXITING;
   }

   /** Parts dock over the normal-size player. */
   public static boolean docking(HulkbusterLayer.Phase phase) {
      return phase == HulkbusterLayer.Phase.DROPPING || phase == HulkbusterLayer.Phase.ASSEMBLING || phase == HulkbusterLayer.Phase.PARTIAL;
   }
}
