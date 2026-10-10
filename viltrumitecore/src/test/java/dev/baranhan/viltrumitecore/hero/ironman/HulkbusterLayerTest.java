package dev.baranhan.viltrumitecore.hero.ironman;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.baranhan.viltrumitecore.hero.BodyScale;
import dev.baranhan.viltrumitecore.hero.CleanupReason;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import dev.baranhan.viltrumitecore.hero.ironman.hulkbuster.HulkbusterKit;
import dev.baranhan.viltrumitecore.hero.ironman.hulkbuster.HulkbusterLayer;
import dev.baranhan.viltrumitecore.hero.ironman.veronica.VeronicaView;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkRoster;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class HulkbusterLayerTest {
   static HulkbusterLayer active() {
      HulkbusterLayer layer = new HulkbusterLayer();
      assertTrue(layer.start());
      for (int i = 0; i < HulkbusterLayer.DROP_TICKS; i++) {
         layer.tick();
      }

      HulkbusterLayer.Event event = HulkbusterLayer.Event.NONE;
      for (int i = 0; i < HulkbusterLayer.ASSEMBLE_TICKS; i++) {
         event = layer.tick();
      }

      assertSame(HulkbusterLayer.Event.READY, event);
      layer.activate();
      return layer;
   }

   @Test
   void dropAssembleActivate() {
      HulkbusterLayer layer = active();
      assertTrue(layer.active());
      assertTrue(layer.big());
      assertEquals(HulkbusterLayer.FULL_PARTS, layer.parts());
      assertEquals(HulkbusterLayer.DURABILITY, layer.durability());
   }

   @Test
   void refusedOnCooldown() {
      HulkbusterLayer layer = active();
      layer.breakNow();
      assertEquals(HulkbusterLayer.COOLDOWN, layer.cooldown());
      assertFalse(layer.start());
   }

   @Test
   void exitStartsCooldownAfterClimbingOut() {
      HulkbusterLayer layer = active();
      assertTrue(layer.exit());
      assertTrue(layer.big(), "still big while climbing out");
      HulkbusterLayer.Event event = HulkbusterLayer.Event.NONE;
      for (int i = 0; i < HulkbusterLayer.EXIT_TICKS; i++) {
         event = layer.tick();
      }

      assertSame(HulkbusterLayer.Event.EXITED, event);
      assertFalse(layer.present());
      assertEquals(HulkbusterLayer.COOLDOWN, layer.cooldown());
   }

   @Test
   void hitsGoToDurabilityAndBreak() {
      HulkbusterLayer layer = active();
      assertTrue(layer.absorb(100.0F));
      assertTrue(layer.durability() < HulkbusterLayer.DURABILITY);
      assertTrue(layer.absorb(10000.0F), "the emptying hit is still absorbed (no spill)");
      assertTrue(layer.broken());
   }

   @Test
   void controlInterruptsAssemblyPartialNoCooldown() {
      HulkbusterLayer layer = new HulkbusterLayer();
      layer.start();
      for (int i = 0; i < HulkbusterLayer.DROP_TICKS + 35; i++) {
         layer.tick();
      }

      layer.interrupt();
      assertSame(HulkbusterLayer.Phase.PARTIAL, layer.phase());
      assertEquals(0b11, layer.parts(), "legs and torso locked at 35 t");
      assertFalse(layer.big(), "partial: no scale, no kit");
      assertFalse(layer.absorb(5.0F), "partial: durability not active");
      assertTrue(layer.exit());
      assertFalse(layer.present());
      assertEquals(0, layer.cooldown(), "never active: no cooldown");
   }

   @Test
   void refuseWithoutRoomHasNoCooldown() {
      HulkbusterLayer layer = new HulkbusterLayer();
      layer.start();
      for (int i = 0; i < HulkbusterLayer.DROP_TICKS + HulkbusterLayer.ASSEMBLE_TICKS; i++) {
         layer.tick();
      }

      layer.refuse();
      assertFalse(layer.present());
      assertEquals(0, layer.cooldown());
   }

   @Test
   void saveLoadInsideKeepsLayerAndChecksSpace() {
      HulkbusterLayer layer = active();
      layer.absorb(50.0F);
      CompoundTag tag = new CompoundTag();
      layer.save(tag);
      HulkbusterLayer loaded = new HulkbusterLayer();
      loaded.load(tag);
      assertTrue(loaded.active());
      assertTrue(loaded.needsFitCheck(), "the first tick checks the space for the big body");
      assertEquals(layer.durability(), loaded.durability(), 1.0E-5F);
      assertEquals(0, loaded.cooldown(), "no double cooldown");
   }

   @Test
   void deathBreaksWithCooldownHeroChangeResets() {
      IronManState state = new IronManState();
      HulkbusterLayer layer = state.hulkbuster;
      layer.start();
      for (int i = 0; i < HulkbusterLayer.DROP_TICKS + HulkbusterLayer.ASSEMBLE_TICKS; i++) {
         layer.tick();
      }

      layer.activate();
      state.onCleanup(CleanupReason.DEATH);
      assertFalse(layer.present());
      assertEquals(HulkbusterLayer.COOLDOWN, layer.cooldown());
      IronManState respawned = IronManState.cloneForRespawn(state);
      assertEquals(HulkbusterLayer.COOLDOWN, respawned.hulkbuster.cooldown());
      respawned.onCleanup(CleanupReason.HERO_CHANGE);
      assertEquals(0, respawned.hulkbuster.cooldown());
   }

   @Test
   void noFlightWithHulkbuster() {
      IronManState state = new IronManState();
      state.suit.toggle();
      for (int i = 0; i < IronManRules.SUIT_DEPLOY_TICKS; i++) {
         state.suit.tick();
      }

      assertTrue(state.wantsFlight());
      state.hulkbuster.start();
      assertFalse(state.wantsFlight(), "the hop is not flight (spec §14.3)");
   }

   @Test
   void kitRules() {
      assertFalse(HulkbusterKit.jackhammerHit(0));
      assertTrue(HulkbusterKit.jackhammerHit(HulkbusterKit.JACKHAMMER_INTERVAL));
      assertFalse(HulkbusterKit.jackhammerHit(HulkbusterKit.JACKHAMMER_INTERVAL + 1));
      assertEquals(HulkbusterKit.SLAM_DAMAGE, HulkbusterKit.slamDamage(0.0), 1.0E-5F);
      assertEquals(HulkbusterKit.SLAM_DAMAGE * 0.5F, HulkbusterKit.slamDamage(HulkbusterKit.SLAM_RADIUS), 1.0E-5F);
      assertEquals(0.0F, HulkbusterKit.slamDamage(HulkbusterKit.SLAM_RADIUS + 0.1));
      assertFalse(HulkbusterKit.slowRepulsorCharged(HulkbusterKit.SLOW_REPULSOR_CHARGE - 1));
      assertTrue(HulkbusterKit.slowRepulsorCharged(HulkbusterKit.SLOW_REPULSOR_CHARGE));
   }

   @Test
   void bodyScaleFindsFreeSpotOrRefuses() {
      Vec3 feet = new Vec3(0.5, 64.0, 0.5);
      // A low ceiling right here, room one block east.
      Vec3 spot = BodyScale.findFree(feet, 1.02F, 3.06F, box -> box.minX >= 1.0);
      assertNotNull(spot);
      assertTrue(spot.x >= 1.0);
      assertNull(BodyScale.findFree(feet, 1.02F, 3.06F, box -> false));
      AABB box = BodyScale.box(feet, 1.02F, 3.06F);
      assertEquals(3.06, box.getYsize(), 1.0E-6);
      assertEquals(feet, BodyScale.findFree(feet, 1.02F, 3.06F, b -> true));
   }

   @Test
   void snapshotSyncsPhaseAndDurability() {
      IronManState state = new IronManState();
      state.suit.toggle();
      for (int i = 0; i < IronManRules.SUIT_DEPLOY_TICKS; i++) {
         state.suit.tick();
      }

      HulkbusterLayer layer = state.hulkbuster;
      layer.start();
      for (int i = 0; i < HulkbusterLayer.DROP_TICKS + HulkbusterLayer.ASSEMBLE_TICKS; i++) {
         layer.tick();
      }

      layer.activate();
      layer.absorb(200.0F);
      HeroPublicSnapshot snapshot = IronManHero.snapshotOf(state);
      assertEquals(HulkbusterLayer.Phase.ACTIVE.ordinal(), IronManFlags.get(snapshot.heroFlags(), IronManFlags.Field.HULKBUSTER_PHASE));
      assertEquals(HulkbusterLayer.FULL_PARTS, IronManVariant.hulkParts(snapshot.variant()));
      assertEquals(layer.durability() / HulkbusterLayer.DURABILITY, IronManVariant.hulkDurability(snapshot.variant()), 0.01F);
   }

   @Test
   void veronicaViewCarriesHulkbusterCard() {
      VeronicaView view = VeronicaView.of(new MarkRoster(), VeronicaView.HULKBUSTER_ON, 300);
      FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
      view.write(buffer);
      VeronicaView read = VeronicaView.read(buffer);
      assertEquals(VeronicaView.HULKBUSTER_ON, read.hulkbuster());
      assertEquals(300, read.podTicksLeft());
   }
}
