package dev.baranhan.viltrumitecore.hero.ironman;

import dev.baranhan.viltrumitecore.ability.ViltrumiteAbility;
import dev.baranhan.viltrumitecore.hero.HeroAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

/** Iron Man ability ids and default slot order (spec §6.2). */
public final class IronManAbilities {
   public static final String SUIT = "ironman:suit";
   /** Page 2 (slots 6-11), index 4 = fifth key (B): Scan, Countermeasures, Veronica, Helmet, Suit, Legion reserve. */
   public static final int SUIT_SLOT = 6 + 4;
   /** Own slots in panel order. Later stages append. */
   private static final String[] OWN = {SUIT};

   private IronManAbilities() {
   }

   public static String[] slotIds() {
      return OWN.clone();
   }

   public static boolean owns(@Nullable String abilityId) {
      if (abilityId == null) {
         return false;
      }

      for (String id : OWN) {
         if (id.equals(abilityId)) {
            return true;
         }
      }

      return false;
   }

   @Nullable
   public static ResourceLocation icon(String abilityId) {
      if (!owns(abilityId)) {
         return null;
      }

      // Icons are drawn in Stage 1b (panel art).
      return new ResourceLocation("viltrumitecore", "textures/gui/ability/ironman/" + abilityId.substring(abilityId.indexOf(':') + 1) + ".png");
   }

   public static List<ViltrumiteAbility> panelAbilities() {
      List<ViltrumiteAbility> list = new ArrayList<>();
      for (String id : OWN) {
         String name = "ironman_" + id.substring(id.indexOf(':') + 1);
         list.add(new ViltrumiteAbility(id, icon(id), "ability.viltrumitecore." + name + ".name", "ability.viltrumitecore." + name + ".desc", 0));
      }

      return list;
   }

   public static String[] defaultLoadout() {
      String[] loadout = new String[18];
      Arrays.fill(loadout, "");
      loadout[SUIT_SLOT] = SUIT;
      return loadout;
   }

   @Nullable
   public static HeroAction actionFor(String abilityId) {
      return SUIT.equals(abilityId) ? HeroAction.SUIT : null;
   }
}
