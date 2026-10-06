package dev.baranhan.viltrumitecore.hero.regulus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.baranhan.viltrumitecore.hero.CleanupReason;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class RegulusStateTest {

   @Test
   void heroDataRoundtripsCooldowns() {
      // Relog/death must not dodge cooldowns: the book (1800t), counter (800t)
      // and slot cooldowns persist through HeroData (a2/a3 audit finding).
      RegulusState state = new RegulusState();
      state.cooldowns.put(RegulusAbilities.EVANGELIUM, 1200);
      state.cooldowns.put(RegulusAbilities.COUNTER, 300);

      CompoundTag nbt = new CompoundTag();
      state.saveHeroData(nbt);

      RegulusState restored = new RegulusState();
      restored.loadHeroData(nbt);
      assertEquals(1200, restored.cooldownOf(RegulusAbilities.EVANGELIUM));
      assertEquals(300, restored.cooldownOf(RegulusAbilities.COUNTER));
   }

   @Test
   void heroDataLoadDropsUnknownAndSpentCooldowns() {
      CompoundTag nbt = new CompoundTag();
      CompoundTag cooldowns = new CompoundTag();
      cooldowns.putInt("madeup:ability", 50);
      cooldowns.putInt(RegulusAbilities.MANIA, 0);
      cooldowns.putInt(RegulusAbilities.DEBRIS_KICK, -5);
      nbt.put(RegulusState.COOLDOWNS_TAG, cooldowns);

      RegulusState state = new RegulusState();
      state.loadHeroData(nbt);
      assertEquals(0, state.cooldownOf("madeup:ability"));
      assertEquals(0, state.cooldownOf(RegulusAbilities.MANIA));
      assertEquals(0, state.cooldownOf(RegulusAbilities.DEBRIS_KICK));
      assertTrue(state.cooldowns.isEmpty());
   }

   @Test
   void heroDataLoadAbsentTagKeepsStateEmpty() {
      RegulusState state = new RegulusState();
      state.loadHeroData(new CompoundTag());
      assertTrue(state.cooldowns.isEmpty());
   }

   @Test
   void inheritPersistentKeepsCooldownsOnly() {
      // A respawned clone inherits duration bookkeeping but no combat state:
      // carriers, madness and casts die with the body.
      RegulusState previous = new RegulusState();
      previous.cooldowns.put(RegulusAbilities.EVANGELIUM, 700);
      previous.carriers.add(UUID.randomUUID());
      previous.madnessTicksLeft = 400;

      RegulusState fresh = new RegulusState();
      fresh.inheritPersistent(previous);
      assertEquals(700, fresh.cooldownOf(RegulusAbilities.EVANGELIUM));
      assertTrue(fresh.carriers.isEmpty());
      assertEquals(0, fresh.madnessTicksLeft);
   }

   @Test
   void resetTransientClearsChannelAndHitBookkeeping() {
      RegulusState state = new RegulusState();
      state.channelTargetId = UUID.randomUUID();
      state.channelEffectId = UUID.randomUUID();
      state.channelTicks = 40;
      state.maxHitLoss = 6.0F;

      state.resetTransient(CleanupReason.DEATH);
      assertNull(state.channelTargetId);
      assertNull(state.channelEffectId, "a stale channel effect id must not survive cleanup");
      assertEquals(0, state.channelTicks);
      assertEquals(0.0F, state.maxHitLoss);
   }
}
