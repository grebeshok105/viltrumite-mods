package dev.baranhan.viltrumitecore.hero.regulus;

import dev.baranhan.viltrumitecore.hero.CleanupReason;
import dev.baranhan.viltrumitecore.hero.HeroRegistry;
import dev.baranhan.viltrumitecore.hero.control.ControlKind;
import dev.baranhan.viltrumitecore.hero.control.ControlManager;
import dev.baranhan.viltrumitecore.hero.control.DomeRecord;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Greed's Embrace (spec 9): the point locks at 13 ticks into the cast, the
 * world-owned dome appears at 18, the hand-recovery pose releases at 32. The
 * dome lives in ControlManager: it survives the caster's death and captures
 * inside targets once, at appearance, for its 80-tick duration.
 */
public final class GreedsEmbrace {
   private GreedsEmbrace() {
   }

   /** Cast lock: event at 18 (dome appears), pose unlock at 32 (hand lowers). */
   public static void start(ServerPlayer player, RegulusState state) {
      state.beginAction(RegulusHero.ACTION_EMBRACE, RegulusRules.EMBRACE_RECOVER_TICK + 1, RegulusRules.EMBRACE_APPEAR_TICK, RegulusRules.EMBRACE_RECOVER_TICK);
   }

   public static void tick(ServerPlayer player, RegulusState state) {
      if (!RegulusHero.ACTION_EMBRACE.equals(state.actionId) || !(player.level() instanceof ServerLevel level)) {
         return;
      }

      if (shouldLock(state)) {
         state.actionPoint = lockPoint(player);
      }

      if (shouldAppear(state)) {
         appear(player, level, state);
      }
   }

   /** The aim point freezes at the 13-tick lock, before the 18-tick event. */
   static boolean shouldLock(RegulusState state) {
      return state.actionPoint == null && state.actionElapsed >= RegulusRules.EMBRACE_LOCK_TICK;
   }

   static boolean shouldAppear(RegulusState state) {
      return !state.eventFired && state.actionElapsed >= state.actionEventTick;
   }

   /** Look-ray point, block-clipped at 40 blocks (spec 9.1). */
   private static Vec3 lockPoint(ServerPlayer player) {
      Vec3 eye = player.getEyePosition();
      Vec3 end = eye.add(player.getLookAngle().scale(RegulusRules.EMBRACE_RANGE));
      BlockHitResult hit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
      return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
   }

   /**
    * Dome event: one dome per caster; one-shot capture excludes the caster,
    * his tames, scoreboard teammates, already-controlled and grabbed targets
    * and the control-immune. Cooldown 700 starts on appearance.
    */
   private static void appear(ServerPlayer player, ServerLevel level, RegulusState state) {
      state.eventFired = true;
      ControlManager manager = ControlManager.get(level);
      if (manager.hasDomeFrom(player.getUUID())) {
         return;
      }

      Vec3 center = state.actionPoint == null ? player.position() : state.actionPoint;
      long now = level.getGameTime();
      DomeRecord dome = DomeRecord.create(player.getUUID(), level.dimension(), center, RegulusRules.EMBRACE_RADIUS, now, RegulusRules.EMBRACE_DURATION_TICKS, java.util.Set.of());
      manager.addDome(dome);

      double radius = RegulusRules.EMBRACE_RADIUS;
      AABB area = new AABB(center.subtract(radius, radius, radius), center.add(radius, radius, radius));
      for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area,
         e -> e.position().distanceToSqr(center) <= radius * radius
            && captureFilter(e == player, isTameOf(e, player), e.isAlliedTo(player), manager.isControlled(e), e.getTags().contains("ViltrumiteGrabbed"))
            && HeroRegistry.allowsExternalControl(e, ControlKind.STASIS))) {
         if (manager.tryAcquire(entity, player.getUUID(), dome.id(), ControlKind.STASIS, dome.expiresAt())) {
            dome.captured().add(entity.getUUID());
         }
      }

      state.startCooldown(RegulusAbilities.GREEDS_EMBRACE, RegulusRules.EMBRACE_COOLDOWN);
      level.playSound(null, BlockPos.containing(center), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.9F, 0.8F);
   }

   /** Pure capture exclusions (spec 9.2); unit-tested. */
   static boolean captureFilter(boolean self, boolean tame, boolean ally, boolean alreadyControlled, boolean grabbed) {
      return !self && !tame && !ally && !alreadyControlled && !grabbed;
   }

   private static boolean isTameOf(LivingEntity entity, Player caster) {
      return entity instanceof TamableAnimal pet && pet.isOwnedBy(caster);
   }

   /**
    * World state belongs to ControlManager (a dead caster keeps his dome, a
    * hero change/disconnect removes it); nothing Regulus-side remains.
    */
   public static void cleanup(ServerPlayer player, RegulusState state, CleanupReason reason) {
   }
}
