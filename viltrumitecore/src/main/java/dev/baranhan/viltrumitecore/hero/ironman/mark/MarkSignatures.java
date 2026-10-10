package dev.baranhan.viltrumitecore.hero.ironman.mark;

import dev.baranhan.viltrumitecore.hero.ironman.mark.sig.MicroLaser;
import dev.baranhan.viltrumitecore.hero.ironman.mark.sig.PulseUnibeam;
import dev.baranhan.viltrumitecore.hero.ironman.mark.sig.RocketFist;
import dev.baranhan.viltrumitecore.hero.ironman.mark.sig.ShoulderGun;
import dev.baranhan.viltrumitecore.hero.ironman.mark.sig.Slam;
import dev.baranhan.viltrumitecore.hero.ironman.mark.sig.Starboost;
import dev.baranhan.viltrumitecore.hero.ironman.mark.sig.Camo;
import java.util.EnumMap;
import java.util.Map;

/** One signature per mark (spec §13). */
public final class MarkSignatures {
   private static final Map<MarkId, MarkSignature> TABLE = new EnumMap<>(MarkId.class);

   static {
      TABLE.put(MarkId.MARK_7, new MicroLaser());
      TABLE.put(MarkId.MARK_42, new RocketFist());
      TABLE.put(MarkId.MARK_15, new Camo());
      TABLE.put(MarkId.MARK_39, new Starboost());
      TABLE.put(MarkId.MARK_17, new PulseUnibeam());
      TABLE.put(MarkId.WAR_MACHINE_MK2, new ShoulderGun());
      TABLE.put(MarkId.IRON_HEART_MK3, new Slam());
   }

   private MarkSignatures() {
   }

   public static MarkSignature of(MarkId id) {
      return TABLE.get(id);
   }
}
