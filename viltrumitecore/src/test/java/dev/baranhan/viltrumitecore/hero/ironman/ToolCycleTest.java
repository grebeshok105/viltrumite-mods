package dev.baranhan.viltrumitecore.hero.ironman;

import static org.junit.jupiter.api.Assertions.*;

import dev.baranhan.viltrumitecore.hero.ironman.combat.RightTool;
import dev.baranhan.viltrumitecore.hero.ironman.combat.ToolCycle;
import org.junit.jupiter.api.Test;

class ToolCycleTest {
   @Test
   void cycleRepulsorToFormedWeapon() {
      assertSame(RightTool.NANO_BLADE, ToolCycle.next(RightTool.REPULSOR, RightTool.NANO_BLADE, null));
      assertSame(RightTool.REPULSOR, ToolCycle.next(RightTool.NANO_BLADE, RightTool.NANO_BLADE, null));
   }

   @Test
   void cycleWithoutWeaponStays() {
      assertSame(RightTool.REPULSOR, ToolCycle.next(RightTool.REPULSOR, null, null));
   }

   @Test
   void signatureUsedWhenNoWeapon() {
      assertSame(RightTool.SIGNATURE, ToolCycle.next(RightTool.REPULSOR, null, RightTool.SIGNATURE));
   }

   @Test
   void rmbUsesServerTool() {
      // A forged RMB with no weapon formed: the server tool stays REPULSOR.
      IronManState state = new IronManState();
      assertNull(state.arsenal.weapon());
      assertSame(RightTool.REPULSOR, state.rightTool);
      assertFalse(IronManHero.claimsWeapon(true, state.arsenal.weapon() != null));
   }

   @Test
   void noClaimsWithoutSuit() {
      assertFalse(IronManHero.claimsWeapon(false, true));
      assertFalse(IronManHero.claimsPrimary(false, dev.baranhan.viltrumiteflight.util.FlightState.HOVER));
      assertFalse(IronManHero.claimsPrimary(true, dev.baranhan.viltrumiteflight.util.FlightState.NONE));
   }

   @Test
   void emptySuitInteractionWorksWithoutArmor() {
      assertSame(dev.baranhan.viltrumitecore.hero.HeroAction.INTERACT, IronManHero.secondaryAction(false, false, true));
      assertSame(dev.baranhan.viltrumitecore.hero.HeroAction.INTERACT, IronManHero.secondaryAction(true, false, true));
      assertNull(IronManHero.secondaryAction(false, false, false));
      assertSame(dev.baranhan.viltrumitecore.hero.HeroAction.SECONDARY_USE, IronManHero.secondaryAction(true, false, false));
      assertNull(IronManHero.secondaryAction(true, true, true));
   }

   @Test
   void unknownToolIdIsRepulsor() {
      assertSame(RightTool.REPULSOR, RightTool.byId(99));
      assertTrue(RightTool.NANO_HAMMER.nanoWeapon());
      assertFalse(RightTool.REPULSOR.nanoWeapon());
   }

   @Test
   void suitLossResetsTool() {
      IronManState state = new IronManState();
      state.rightTool = RightTool.NANO_HAMMER;
      state.heldTool = RightTool.NANO_HAMMER;
      state.stopCombat();
      assertSame(RightTool.REPULSOR, state.rightTool);
      assertNull(state.heldTool);
   }
}
