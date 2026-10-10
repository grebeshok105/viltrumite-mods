package dev.baranhan.viltrumitecore.hero.ironman;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.baranhan.viltrumitecore.hero.HeroAction;
import dev.baranhan.viltrumitecore.hero.HeroId;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import dev.baranhan.viltrumitecore.hero.ironman.mark.EquipTimeline;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkId;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkLocation;
import dev.baranhan.viltrumitecore.hero.ironman.mark.SuitPart;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class MarkSnapshotTest {
   @Test
   void variantPacksMarkAndParts() {
      int v = IronManVariant.pack(MarkId.WAR_MACHINE_MK2, 0x1FF);
      assertSame(MarkId.WAR_MACHINE_MK2, IronManVariant.mark(v));
      assertEquals(0x1FF, IronManVariant.parts(v));
      assertNull(IronManVariant.mark(IronManVariant.pack(null, 0)));
      assertEquals(0, IronManVariant.pack(null, 0));
   }

   @Test
   void equippingSyncsTimelineSourceAndParts() {
      IronManState state = new IronManState();
      state.roster.move(MarkId.MARK_7, MarkLocation.STORED, MarkLocation.IN_DELIVERY);
      state.suit.startEquip(MarkId.MARK_7, true, 0);
      state.equipSource = new Vec3(1, 2, 3);
      for (int i = 0; i < 25; i++) {
         state.suit.tick();
      }

      HeroPublicSnapshot snapshot = IronManHero.snapshotOf(state);
      assertEquals(IronManFlags.EQUIP_EQUIPPING, IronManFlags.get(snapshot.heroFlags(), IronManFlags.Field.EQUIP_PHASE));
      assertEquals(HeroAction.SUIT.ordinal(), snapshot.actionId());
      assertEquals(EquipTimeline.DELIVERY, snapshot.actionLength());
      assertEquals(25, snapshot.actionElapsed());
      assertEquals(new Vec3(1, 2, 3), snapshot.actionTarget());
      assertSame(MarkId.MARK_7, IronManVariant.mark(snapshot.variant()));
      assertEquals(state.suit.parts(), IronManVariant.parts(snapshot.variant()));
      assertFalse(IronManFlags.is(snapshot.heroFlags(), IronManFlags.Field.SUIT_WORN));
   }

   @Test
   void wornMarkSyncsDurabilityAndCooldowns() {
      IronManState state = new IronManState();
      state.suit.startEquip(MarkId.MARK_17, true, 0);
      for (int i = 0; i < EquipTimeline.DELIVERY; i++) {
         state.suit.tick();
      }

      state.roster.move(MarkId.MARK_17, MarkLocation.STORED, MarkLocation.WORN);
      state.roster.setDurability(MarkId.MARK_17, 55.5F);
      state.veronicaCooldown = 300;
      state.signature.cooldown = 40;
      HeroPublicSnapshot snapshot = IronManHero.snapshotOf(state);
      assertTrue(IronManFlags.is(snapshot.heroFlags(), IronManFlags.Field.SUIT_WORN));
      assertEquals(555, snapshot.resource2());
      assertEquals(SuitPart.fullMask(MarkId.MARK_17), IronManVariant.parts(snapshot.variant()));
      assertEquals(300, snapshot.extraCooldown(2));
      assertEquals(40, snapshot.extraCooldown(3));
      assertEquals(snapshot, HeroPublicSnapshot.decode(snapshot.encode()));
   }

   @Test
   void oldEncodingDecodesVariantZero() {
      HeroPublicSnapshot old = new HeroPublicSnapshot(HeroId.IRON_MAN, -1, 0, 0, 0, false, 0, 0, false, false, 0, 0, -1,
         new int[HeroPublicSnapshot.COOLDOWN_COUNT], false, -1, null, 500, false, 3, new int[]{0, 20});
      HeroPublicSnapshot decoded = HeroPublicSnapshot.decode(old.encode());
      assertEquals(0, decoded.variant());
      assertEquals(0, decoded.resource2());
      assertEquals(20, decoded.extraCooldown(1));
      HeroPublicSnapshot withVariant = new HeroPublicSnapshot(HeroId.IRON_MAN, -1, 0, 0, 0, false, 0, 0, false, false, 0, 0, -1,
         new int[HeroPublicSnapshot.COOLDOWN_COUNT], false, -1, null, 0, false, 0, new int[0], 7, 1200);
      HeroPublicSnapshot roundTrip = HeroPublicSnapshot.decode(withVariant.encode());
      assertEquals(7, roundTrip.variant());
      assertEquals(1200, roundTrip.resource2());
      assertEquals(0, roundTrip.extraCooldowns().length);
   }
}
