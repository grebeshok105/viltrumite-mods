package dev.baranhan.viltrumitecore.hero;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class HeroIdentityTest {

   @Test
   void exposesStableThirdRaceId() {
      assertEquals("human", HeroId.HUMAN.key());
      assertEquals("viltrumite", HeroId.VILTRUMITE.key());
      assertEquals("regulus", HeroId.REGULUS.key());
      assertSame(HeroId.REGULUS, HeroId.fromKey("regulus"));
   }

   @Test
   void rejectsUnknownAndNullIds() {
      assertNull(HeroId.fromKey("omni_man"));
      assertNull(HeroId.fromKey(""));
      assertNull(HeroId.fromKey(null));
   }

   @Test
   void migratesLegacyBooleanIdentity() {
      assertSame(HeroId.VILTRUMITE, HeroId.fromLegacyBoolean(true));
      assertSame(HeroId.HUMAN, HeroId.fromLegacyBoolean(false));
   }

   @Test
   void legacySetterCannotConvertOrClearAnotherHero() {
      // setViltrumite(true/false) only ever toggles HUMAN<->VILTRUMITE.
      assertSame(HeroId.VILTRUMITE, HeroId.legacySet(HeroId.HUMAN, true));
      assertSame(HeroId.HUMAN, HeroId.legacySet(HeroId.VILTRUMITE, false));
      // A Regulus identity is immune to the legacy writer in both directions.
      assertSame(HeroId.REGULUS, HeroId.legacySet(HeroId.REGULUS, true));
      assertSame(HeroId.REGULUS, HeroId.legacySet(HeroId.REGULUS, false));
   }

   @Test
   void heroDataNbtRoundTripPreservesSessionAndTotem() {
      UUID session = UUID.randomUUID();
      HeroSession original = new HeroSession(HeroId.REGULUS, session, true);

      CompoundTag tag = new CompoundTag();
      original.save(tag);

      HeroSession restored = HeroSession.load(tag, HeroId.HUMAN);
      assertEquals(HeroId.REGULUS, restored.heroId());
      assertEquals(session, restored.sessionId());
      assertTrue(restored.totemConsumed());
   }

   @Test
   void consumedTotemSurvivesCloneRoundTrip() {
      // Clone (respawn / dimension change) serializes and restores the same
      // session: totem consumed must NOT be refreshed.
      HeroSession consumed = new HeroSession(HeroId.REGULUS, UUID.randomUUID(), true);
      CompoundTag nbt = new CompoundTag();
      consumed.save(nbt);
      HeroSession clone = HeroSession.load(nbt, HeroId.HUMAN);
      assertTrue(clone.totemConsumed());
      assertEquals(consumed.sessionId(), clone.sessionId());
   }

   @Test
   void heroDataWinsOverLegacyBoolean() {
      // A Regulus player serializes HeroData + legacy IsViltrumite=false.
      // On load the stable id wins over the projection.
      CompoundTag nbt = new CompoundTag();
      new HeroSession(HeroId.REGULUS, UUID.randomUUID(), false).save(nbt);
      nbt.putBoolean("IsViltrumite", false);
      HeroSession restored = HeroSession.load(nbt, HeroId.fromLegacyBoolean(nbt.getBoolean("IsViltrumite")));
      assertEquals(HeroId.REGULUS, restored.heroId());
   }

   @Test
   void legacyOnlyNbtMigratesOnce() {
      CompoundTag nbt = new CompoundTag();
      nbt.putBoolean("IsViltrumite", true);
      HeroSession migrated = HeroSession.load(nbt, HeroId.fromLegacyBoolean(true));
      assertEquals(HeroId.VILTRUMITE, migrated.heroId());
      assertFalse(migrated.totemConsumed());
      assertFalse(migrated.sessionId().equals(new UUID(0L, 0L)));
   }

   @Test
   void publicSnapshotRoundTripsThroughSyncedString() {
      HeroPublicSnapshot snapshot = new HeroPublicSnapshot(
         HeroId.REGULUS, 1, 7, 44, 3, true, 240, 260, false, false, 0, 0, -1, new int[]{100, 0, 0, 0, 0, 0}
      );
      String encoded = snapshot.encode();
      HeroPublicSnapshot decoded = HeroPublicSnapshot.decode(encoded);
      assertEquals(HeroId.REGULUS, decoded.heroId());
      assertEquals(1, decoded.actionId());
      assertEquals(7, decoded.actionElapsed());
      assertEquals(44, decoded.actionLength());
      assertEquals(3, decoded.hearts());
      assertTrue(decoded.lionActive());
      assertEquals(240, decoded.lionWindowLeft());
      assertEquals(260, decoded.lionWindowMax());
      assertFalse(decoded.madness());
      assertEquals(100, decoded.cooldowns()[0]);
   }

   @Test
   void malformedSnapshotStringFallsBackToEmpty() {
      HeroPublicSnapshot decoded = HeroPublicSnapshot.decode("garbage;;;");
      assertEquals(HeroId.HUMAN, decoded.heroId());
      assertFalse(decoded.madness());
   }
}
