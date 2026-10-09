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
   public static final String UNIBEAM = "ironman:unibeam";
   public static final String MISSILES = "ironman:missiles";
   public static final String NANO_ARSENAL = "ironman:nano_arsenal";
   public static final String SCAN = "ironman:scan";
   public static final String COUNTERMEASURES = "ironman:countermeasures";
   public static final String HELMET = "ironman:helmet";
   /** Page 1 (slots 0-5): Unibeam, missiles, nano arsenal (spec §6.2). */
   public static final int UNIBEAM_SLOT = 0;
   public static final int MISSILES_SLOT = 1;
   public static final int NANO_ARSENAL_SLOT = 2;
   /** Page 2 (slots 6-11), index 4 = fifth key (B): Scan, Countermeasures, Veronica, Helmet, Suit, Legion reserve. */
   public static final int SCAN_SLOT = 6;
   public static final int COUNTERMEASURES_SLOT = 6 + 1;
   /** 6 + 2 = Veronica (stage 4). */
   public static final int HELMET_SLOT = 6 + 3;
   public static final int SUIT_SLOT = 6 + 4;
   /** Own slots in panel order. Later stages append. */
   private static final String[] OWN = {UNIBEAM, MISSILES, NANO_ARSENAL, SUIT, SCAN, COUNTERMEASURES, HELMET};

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

      // Panel art: tools/assets/make_ironman_icons.py.
      return new ResourceLocation("viltrumitecore", "textures/gui/ability/ironman/" + abilityId.substring(abilityId.indexOf(':') + 1) + ".png");
   }

   public static List<ViltrumiteAbility> panelAbilities() {
      List<ViltrumiteAbility> list = new ArrayList<>();
      for (String id : OWN) {
         String name = "ironman_" + id.substring(id.indexOf(':') + 1);
         list.add(new ViltrumiteAbility(id, icon(id), "ability.viltrumitecore." + name + ".name", "ability.viltrumitecore." + name + ".desc", 0,
            player -> grey(id, player)));
      }

      return list;
   }

   public static String[] defaultLoadout() {
      String[] loadout = new String[18];
      Arrays.fill(loadout, "");
      loadout[SUIT_SLOT] = SUIT;
      loadout[UNIBEAM_SLOT] = UNIBEAM;
      loadout[MISSILES_SLOT] = MISSILES;
      loadout[NANO_ARSENAL_SLOT] = NANO_ARSENAL;
      loadout[SCAN_SLOT] = SCAN;
      loadout[COUNTERMEASURES_SLOT] = COUNTERMEASURES;
      loadout[HELMET_SLOT] = HELMET;
      return loadout;
   }

   @Nullable
   public static HeroAction actionFor(String abilityId) {
      if (abilityId == null) {
         return null;
      }

      return switch (abilityId) {
         case SUIT -> HeroAction.SUIT;
         case UNIBEAM -> HeroAction.UNIBEAM;
         case MISSILES -> HeroAction.MISSILES;
         case NANO_ARSENAL -> HeroAction.NANO_ARSENAL;
         case SCAN -> HeroAction.SCAN;
         case COUNTERMEASURES -> HeroAction.COUNTERMEASURES;
         case HELMET -> HeroAction.HELMET;
         default -> null;
      };
   }

   /** Panel grey state from the synced snapshot (client) — pure on the snapshot. */
   static boolean grey(String abilityId, net.minecraft.world.entity.player.Player player) {
      if (!(player instanceof dev.baranhan.viltrumitecore.hero.HeroPlayer heroPlayer)) {
         return false;
      }

      return greyFor(abilityId, heroPlayer.getHeroSnapshot());
   }

   public static boolean greyFor(String abilityId, @Nullable dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot snapshot) {
      if (snapshot == null || snapshot.heroId() != dev.baranhan.viltrumitecore.hero.HeroId.IRON_MAN) {
         return false;
      }

      int flags = snapshot.heroFlags();
      boolean worn = IronManFlags.is(flags, IronManFlags.Field.SUIT_WORN);
      return switch (abilityId) {
         // Nano lost after a core explosion: extra cooldown [0].
         case SUIT -> snapshot.extraCooldown(0) > 0;
         case UNIBEAM -> !worn || snapshot.resourceLocked() || IronManFlags.is(flags, IronManFlags.Field.OVERHEAT_LOCK);
         case MISSILES, NANO_ARSENAL -> !worn || snapshot.resourceLocked();
         // Scan needs the closed helmet (spec §10); flares: cooldown in extra [1], no energy.
         case SCAN -> !worn || !IronManFlags.is(flags, IronManFlags.Field.HELMET_CLOSED);
         case COUNTERMEASURES -> !worn || snapshot.extraCooldown(1) > 0;
         case HELMET -> !worn;
         default -> false;
      };
   }
}
