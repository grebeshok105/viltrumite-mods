package dev.baranhan.viltrumitecore.hero.ironman;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.baranhan.viltrumitecore.hero.HeroAction;
import dev.baranhan.viltrumitecore.hero.HeroId;
import org.junit.jupiter.api.Test;

class IronManHeroTest {
   private final IronManHero hero = new IronManHero();

   @Test
   void fromKeyIronman() {
      assertSame(HeroId.IRON_MAN, HeroId.fromKey("ironman"));
      assertEquals("ironman", HeroId.IRON_MAN.key());
      assertEquals(HeroId.IRON_MAN, this.hero.id());
   }

   @Test
   void ownsOnlyOwnIds() {
      assertTrue(this.hero.ownsAbility(IronManAbilities.SUIT));
      for (String id : new String[]{"viltrumite:punch", "viltrumite:dash", "homelander:lasers", "regulus:mania", "ironman:unknown", "", null}) {
         assertFalse(this.hero.ownsAbility(id), String.valueOf(id));
      }
   }

   @Test
   void noLegacyKit() {
      assertFalse(this.hero.allowsLegacyAbilities(null));
      assertFalse(this.hero.allowsLegacyAbility(null, "viltrumite:punch"));
      assertTrue(this.hero.hasAbilityPanel(null));
      assertTrue(this.hero.allowsAbilityPages(null));
   }

   @Test
   void loadoutPutsSuitOnPage2SlotB() {
      String[] loadout = this.hero.defaultLoadout();
      assertEquals(18, loadout.length);
      // Page 2 starts at slot 6; the fifth key (B) is index 4 of the page.
      assertEquals(IronManAbilities.SUIT, loadout[6 + 4]);
      for (int i = 0; i < 18; i++) {
         if (i != 10) {
            assertEquals("", loadout[i], "slot " + i);
         }
      }
   }

   @Test
   void suitSlotSendsSuitAction() {
      assertEquals(1, this.hero.heroInputSlots().length);
      assertEquals(IronManAbilities.SUIT, this.hero.heroInputSlots()[0]);
      assertSame(HeroAction.SUIT, this.hero.heroActionFor(IronManAbilities.SUIT));
      assertEquals(null, this.hero.heroActionFor("viltrumite:punch"));
   }

   @Test
   void noFlightWithoutSuit() {
      assertFalse(this.hero.allowsFlight(null));
   }
}
