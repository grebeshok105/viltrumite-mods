package dev.baranhan.viltrumitecore.hero.ironman.mark.sig;

import dev.baranhan.viltrumitecore.entity.RocketFistEntity;
import dev.baranhan.viltrumitecore.entity.ViltrumiteEntities;
import dev.baranhan.viltrumitecore.hero.ironman.IronManMarkSounds;
import dev.baranhan.viltrumitecore.hero.ironman.IronManState;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkSignature;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * MARK_42: rocket fist. The right glove flies to the crosshair target, hits and
 * returns to the hand (spec §13.3). The hand is unavailable while it is away.
 */
public final class RocketFist implements MarkSignature {
   /** The hand can throw only while the glove is on it. */
   public static boolean canLaunch(int entityId) {
      return entityId < 0;
   }

   @Override
   public void press(ServerPlayer player, IronManState state) {
      if (!canLaunch(state.signature.entityId)) {
         return;
      }

      ServerLevel level = player.serverLevel();
      SignatureTrace.Hit hit = SignatureTrace.shoot(player, player.getLookAngle(), SignatureRules.FIST_RANGE);
      RocketFistEntity fist = new RocketFistEntity(ViltrumiteEntities.ROCKET_FIST.get(), level);
      Vec3 hand = SignatureTrace.hand(player, true);
      fist.setPos(hand.x, hand.y, hand.z);
      fist.setOwner(player);
      fist.launch(hit.target(), hit.end());
      level.addFreshEntity(fist);
      state.signature.entityId = fist.getId();
      state.signature.active = true;
      SignatureTrace.sound(player, IronManMarkSounds.ROCKET_FIST_LAUNCH.get(), 1.0F, 1.0F);
   }

   @Override
   public void tick(ServerPlayer player, IronManState state) {
      int id = state.signature.entityId;
      if (id < 0) {
         return;
      }

      Entity fist = player.serverLevel().getEntity(id);
      if (!(fist instanceof RocketFistEntity) || !fist.isAlive()) {
         reattach(state);
      }
   }

   @Override
   public void stop(ServerPlayer player, IronManState state) {
      if (state.signature.entityId >= 0 && player.serverLevel().getEntity(state.signature.entityId) instanceof RocketFistEntity fist) {
         fist.discard();
      }

      state.signature.clear();
   }

   /** The glove is back on the hand (called by the fist when it lands on the hand, or when it was lost). */
   public static void reattach(IronManState state) {
      state.signature.entityId = -1;
      state.signature.active = false;
   }

   /** The fist returned to its owner: the hand is free again. */
   public static void reattach(ServerPlayer owner) {
      IronManState state = IronManState.of(owner);
      if (state != null) {
         reattach(state);
      }
   }
}
