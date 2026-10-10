package dev.baranhan.viltrumitecore.hero;

import static org.junit.jupiter.api.Assertions.*;

import dev.baranhan.viltrumitecore.hero.ironman.IronManHero;
import org.junit.jupiter.api.Test;

class HeroGuardSeamTest {
   @Test
   void otherHeroesSwapNormally() {
      HeroDefinition human = new HumanHero();
      assertFalse(human.blocksHandSwap(null));
      assertNull(human.guardAction(null));
   }

   @Test
   void ironManWithoutSuitSwaps() {
      IronManHero hero = new IronManHero();
      assertFalse(hero.blocksHandSwap(null));
      assertNull(hero.guardAction(null));
   }

   @Test
   void guardIsAppendedAction() {
      // Append-only enum: the guard action exists and is routed as a held hero action.
      assertTrue(HeroAction.GUARD.ordinal() > HeroAction.SUIT.ordinal());
   }
}
