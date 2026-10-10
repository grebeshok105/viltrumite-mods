package dev.baranhan.viltrumitecore.hero.ironman;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.baranhan.viltrumitecore.hero.ironman.mark.EquipTimeline;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkId;
import dev.baranhan.viltrumitecore.hero.ironman.mark.SuitPart;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class MarkSuitTest {
   private static Suit equipped(MarkId mark) {
      Suit suit = new Suit();
      suit.startEquip(mark, true, 0);
      for (int i = 0; i < EquipTimeline.DELIVERY; i++) {
         suit.tick();
      }

      return suit;
   }

   private static Suit partial(MarkId mark) {
      Suit suit = new Suit();
      suit.startEquip(mark, true, 0);
      for (int i = 0; i < 30; i++) {
         suit.tick();
      }

      suit.interrupt();
      return suit;
   }

   @Test
   void equipTakes50AndThenWorn() {
      Suit suit = new Suit();
      assertTrue(suit.startEquip(MarkId.MARK_7, true, 0));
      assertTrue(suit.busy());
      assertFalse(suit.worn(), "vulnerable window: not worn, no flight");
      assertFalse(suit.armored());
      Suit.Event last = Suit.Event.NONE;
      for (int i = 0; i < EquipTimeline.DELIVERY; i++) {
         last = suit.tick();
      }

      assertSame(Suit.Event.MARK_ON, last);
      assertTrue(suit.markWorn());
      assertTrue(suit.worn());
      assertTrue(suit.armored());
      assertEquals(SuitPart.fullMask(MarkId.MARK_7), suit.parts());
   }

   @Test
   void enteringEmptySuitTakes20() {
      Suit suit = new Suit();
      suit.startEquip(MarkId.MARK_15, false, 0);
      for (int i = 0; i < EquipTimeline.ENTER - 1; i++) {
         assertSame(Suit.Event.NONE, suit.tick());
      }

      assertSame(Suit.Event.MARK_ON, suit.tick());
   }

   @Test
   void controlInterruptsEquipLockedPartsStay() {
      Suit suit = partial(MarkId.MARK_7);
      int locked = suit.parts();
      assertTrue(locked != 0 && locked != SuitPart.fullMask(MarkId.MARK_7));
      assertSame(SuitState.MARK_PARTIAL, suit.state());
      assertTrue(suit.markOn(), "partial: durability absorbs");
      assertFalse(suit.worn(), "partial: no flight or weapons");
   }

   @Test
   void controlBeforeFirstLockSendsEverythingBack() {
      Suit suit = new Suit();
      suit.startEquip(MarkId.MARK_42, true, 0);
      suit.tick();
      assertSame(MarkId.MARK_42, suit.interrupt());
      assertSame(SuitState.NONE, suit.state());
   }

   @Test
   void rechooseSendsOnlyMissingParts() {
      Suit suit = partial(MarkId.MARK_7);
      int kept = suit.parts();
      assertTrue(suit.startEquip(MarkId.MARK_7, true, kept));
      assertEquals(kept, suit.parts(), "already locked parts are on from the start");
   }

   @Test
   void exitEmitsExitedAfter30() {
      Suit suit = equipped(MarkId.MARK_39);
      assertTrue(suit.startExit(Suit.EXIT_TICKS));
      assertFalse(suit.worn());
      assertEquals(0, suit.parts());
      for (int i = 0; i < Suit.EXIT_TICKS - 1; i++) {
         assertSame(Suit.Event.NONE, suit.tick());
      }

      assertSame(Suit.Event.EXITED, suit.tick());
      assertSame(SuitState.NONE, suit.state());
      assertNull(suit.mark());
   }

   @Test
   void autoNanoIsArmoredAtOnce() {
      Suit suit = equipped(MarkId.IRON_HEART_MK3);
      suit.autoNano();
      assertTrue(suit.worn());
      assertTrue(suit.armored(), "no fall when a mark breaks mid-air");
      assertTrue(suit.nano());
      assertEquals(IronManRules.SUIT_DEPLOY_TICKS, suit.autoNanoTicks());
   }

   @Test
   void nanoToggleIgnoredInMark() {
      Suit suit = equipped(MarkId.MARK_17);
      assertFalse(suit.toggle());
      assertTrue(suit.markWorn());
   }

   @Test
   void saveLoadMarkPartialAndDelivery() {
      CompoundTag tag = new CompoundTag();
      equipped(MarkId.MARK_42).save(tag);
      Suit loaded = new Suit();
      loaded.load(tag);
      assertTrue(loaded.markWorn());
      assertSame(MarkId.MARK_42, loaded.mark());

      Suit partial = partial(MarkId.MARK_7);
      CompoundTag ptag = new CompoundTag();
      partial.save(ptag);
      Suit ploaded = new Suit();
      ploaded.load(ptag);
      assertTrue(ploaded.partial());
      assertEquals(partial.parts(), ploaded.parts());

      Suit equipping = new Suit();
      equipping.startEquip(MarkId.MARK_7, true, 0);
      CompoundTag etag = new CompoundTag();
      equipping.save(etag);
      Suit eloaded = new Suit();
      eloaded.load(etag);
      assertSame(SuitState.NONE, eloaded.state(), "a running delivery never survives a relog");
   }

   @Test
   void stageThreeSaveStillLoads() {
      CompoundTag old = new CompoundTag();
      old.putString("Suit", "NANO");
      Suit suit = new Suit();
      suit.load(old);
      assertTrue(suit.nano());
      assertNull(suit.mark());
   }
}
