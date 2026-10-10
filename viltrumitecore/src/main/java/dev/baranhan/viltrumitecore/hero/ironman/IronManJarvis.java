package dev.baranhan.viltrumitecore.hero.ironman;

import dev.baranhan.viltrumitecore.entity.FlareEntity;
import dev.baranhan.viltrumitecore.entity.Homing;
import dev.baranhan.viltrumitecore.hero.HeroOwnerSnapshot;
import dev.baranhan.viltrumitecore.hero.HeroRegistry;
import dev.baranhan.viltrumitecore.hero.OwnerSection;
import dev.baranhan.viltrumitecore.hero.ironman.jarvis.ThreatScan;
import dev.baranhan.viltrumitecore.hero.ironman.scan.ScanAnalyzer;
import dev.baranhan.viltrumitecore.hero.ironman.scan.ScanProgress;
import dev.baranhan.viltrumitecore.mixin.ShulkerBulletAccessor;
import dev.baranhan.viltrumitecore.network.CoreMessages;
import dev.baranhan.viltrumitecore.network.packet.ScanCardS2CPacket;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Iron Man Stage 3 server driver (spec §10, §11): helmet toggle and auto-close,
 * JARVIS threat list, scan, countermeasure flares, missile auto-lock.
 */
public final class IronManJarvis {
   private IronManJarvis() {
   }

   /** Owner-only sound (UI feedback of the helmet systems). */
   static void notify(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
      player.playNotifySound(sound, SoundSource.PLAYERS, volume, pitch);
   }

   // ---- helmet (§10) ----

   static void helmetPress(ServerPlayer player, IronManState state) {
      state.helmet.toggle();
      IronManCombat.sound(player, state.helmet.closed() ? IronManJarvisSounds.HELMET_CLOSE.get() : IronManJarvisSounds.HELMET_OPEN.get(), 1.0F);
      if (!state.helmet.closed()) {
         helmetOpened(player, state);
      }
   }

   /** Open helmet: no JARVIS targeting — marks, scan and threats drop at once. */
   static void helmetOpened(ServerPlayer player, IronManState state) {
      if (state.missiles.held() && !state.missiles.marks().isEmpty()) {
         for (int id : state.missiles.marks()) {
            state.missiles.drop(id);
         }

         IronManCombat.pushMarks(player, List.of());
      }

      if (state.scan.active()) {
         state.scan.cancel();
      }

      HeroRegistry.pushOwnerSection(player, OwnerSection.SCAN, HeroOwnerSnapshot.Section.EMPTY);
   }

   /** Combat hit closes an open helmet (spec §10): living attacker or projectile only. */
   static void onHurt(ServerPlayer player, IronManState state, DamageSource source) {
      if (!state.suit.worn()) {
         return;
      }

      boolean living = source.getEntity() instanceof LivingEntity attacker && attacker != player;
      boolean projectile = source.getDirectEntity() instanceof Projectile;
      if (Helmet.combatHit(living, projectile) && state.helmet.onHit()) {
         IronManCombat.sound(player, IronManJarvisSounds.HELMET_CLOSE.get(), 1.1F);
      }
   }

   // ---- scan (§11.2) ----

   static void scanPress(ServerPlayer player, IronManState state) {
      if (!state.helmet.closed()) {
         IronManCombat.sound(player, IronManCombatSounds.REPULSOR_FIZZLE.get(), 1.4F);
         return;
      }

      if (state.scan.toggle()) {
         notify(player, IronManJarvisSounds.SCAN_LOOP.get(), 0.7F, 1.0F);
      }
   }

   static boolean scannable(Entity entity, Player self) {
      if (!(entity instanceof LivingEntity living) || entity == self) {
         return false;
      }

      boolean hidden = living instanceof Player other && HeroRegistry.get(other).hiddenFromScan(other);
      return ScanProgress.scannable(living.isAlive(), living.isSpectator(), hidden);
   }

   /** Scannable entity under the crosshair within SCAN_RANGE (blocks stop the ray). */
   @Nullable
   static LivingEntity aimed(ServerPlayer player) {
      Vec3 eye = player.getEyePosition();
      Vec3 end = eye.add(player.getLookAngle().scale(IronManRules.SCAN_RANGE));
      HitResult block = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
      if (block.getType() != HitResult.Type.MISS) {
         end = block.getLocation();
      }

      EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, new AABB(eye, end).inflate(1.0),
         e -> scannable(e, player), eye.distanceToSqr(end));
      return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
   }

   /**
    * Auto lock for one-press scans: the entity under the crosshair, else the
    * visible scannable entity closest to the view direction inside
    * {@link IronManRules#SCAN_LOCK_CONE_DEG} and SCAN_RANGE.
    */
   @Nullable
   static LivingEntity lockCandidate(ServerPlayer player) {
      LivingEntity aimed = aimed(player);
      if (aimed != null) {
         return aimed;
      }

      Vec3 eye = player.getEyePosition();
      Vec3 look = player.getLookAngle();
      double minCos = Math.cos(Math.toRadians(IronManRules.SCAN_LOCK_CONE_DEG));
      LivingEntity best = null;
      double bestCos = minCos;
      AABB box = player.getBoundingBox().inflate(IronManRules.SCAN_RANGE);
      for (LivingEntity living : player.level().getEntitiesOfClass(LivingEntity.class, box, e -> scannable(e, player))) {
         Vec3 to = living.getBoundingBox().getCenter().subtract(eye);
         double length = to.length();
         if (length < 1.0E-3 || length > IronManRules.SCAN_RANGE) {
            continue;
         }

         double cos = to.scale(1.0 / length).dot(look);
         if (cos > bestCos && player.hasLineOfSight(living)) {
            bestCos = cos;
            best = living;
         }
      }

      return best;
   }

   static void scanTick(ServerPlayer player, IronManState state) {
      if (!state.scan.active()) {
         return;
      }

      LivingEntity offered = state.scan.seeking() ? lockCandidate(player) : null;
      int aimedId = offered == null ? -1 : offered.getId();
      int id = state.scan.resolve(aimedId);
      Entity target = id < 0 ? null : player.level().getEntity(id);
      boolean alive = target instanceof LivingEntity living && scannable(living, player);
      double distance = target == null ? Double.MAX_VALUE : target.distanceTo(player);
      boolean los = target != null && player.hasLineOfSight(target);
      ScanProgress.Event event = state.scan.tick(aimedId, alive, distance, los, state.helmet.closed() && state.suit.worn());
      if (event == ScanProgress.Event.DONE && target instanceof LivingEntity living) {
         CoreMessages.sendToPlayer(new ScanCardS2CPacket(ScanAnalyzer.card(living)), player);
         // Sections are deduplicated by content: a re-scan of the same target must restart the highlight.
         HeroRegistry.pushOwnerSection(player, OwnerSection.SCAN, HeroOwnerSnapshot.Section.EMPTY);
         HeroRegistry.pushOwnerSection(player, OwnerSection.SCAN,
            new HeroOwnerSnapshot.Section(new int[]{living.getId()}, new int[]{IronManRules.SCAN_HIGHLIGHT_TICKS}));
         notify(player, IronManJarvisSounds.SCAN_COMPLETE.get(), 0.9F, 1.0F);
      } else if (event == ScanProgress.Event.CANCELLED) {
         notify(player, IronManCombatSounds.REPULSOR_FIZZLE.get(), 0.6F, 1.5F);
      }
   }

   // ---- countermeasures (§11.3) ----

   static boolean boss(Entity entity) {
      return entity instanceof WitherBoss || entity instanceof EnderDragon;
   }

   static void countermeasuresPress(ServerPlayer player, IronManState state) {
      if (!state.countermeasures.fire()) {
         IronManCombat.sound(player, IronManCombatSounds.REPULSOR_FIZZLE.get(), 1.2F);
         return;
      }

      ServerLevel level = player.serverLevel();
      Vec3 look = player.getLookAngle();
      Vec3 flat = new Vec3(look.x, 0.0, look.z).lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : new Vec3(look.x, 0.0, look.z).normalize();
      Vec3 side = new Vec3(-flat.z, 0.0, flat.x);
      Vec3 back = player.position().add(0.0, player.getBbHeight() * 0.75, 0.0).subtract(flat.scale(0.35));
      List<FlareEntity> flares = new ArrayList<>(IronManRules.FLARE_COUNT);
      for (int i = 0; i < IronManRules.FLARE_COUNT; i++) {
         // Fan from the back and shoulders: half left, half right, rising then falling.
         double spread = (i - (IronManRules.FLARE_COUNT - 1) / 2.0) / (IronManRules.FLARE_COUNT / 2.0);
         Vec3 velocity = flat.scale(-0.35).add(side.scale(spread * 0.6)).add(0.0, 0.35 + 0.1 * Math.abs(spread), 0.0)
            .add(player.getDeltaMovement().scale(0.5));
         flares.add(FlareEntity.launch(level, back.add(side.scale(spread * 0.3)), velocity));
      }

      IronManCombat.sound(player, IronManJarvisSounds.FLARE_LAUNCH.get(), 1.0F);
      IronManCombat.sound(player, IronManJarvisSounds.FLARE_BURN.get(), 1.0F);
      AABB homingBox = player.getBoundingBox().inflate(IronManRules.FLARE_HOMING_RANGE);
      for (Entity entity : level.getEntities(player, homingBox, e -> e instanceof Homing || e instanceof ShulkerBullet)) {
         if (entity instanceof Homing homing && Countermeasures.retargets(homing.homingTarget() == null ? -1 : homing.homingTarget().getId(), player.getId())) {
            homing.retarget(nearest(flares, entity.position()));
         } else if (entity instanceof ShulkerBullet bullet && ((ShulkerBulletAccessor)bullet).viltrumitecore$getFinalTarget() == player) {
            FlareEntity flare = nearest(flares, entity.position());
            ((ShulkerBulletAccessor)bullet).viltrumitecore$setFinalTarget(flare);
            ((ShulkerBulletAccessor)bullet).viltrumitecore$setTargetId(flare == null ? null : flare.getUUID());
         }
      }

      AABB mobBox = player.getBoundingBox().inflate(IronManRules.FLARE_MOB_RANGE);
      for (Mob mob : level.getEntitiesOfClass(Mob.class, mobBox, m -> m.getTarget() == player)) {
         if (Countermeasures.affects(boss(mob))) {
            state.countermeasures.forget(mob.getId());
            forgetPlayer(mob, player);
         }
      }
   }

   @Nullable
   static FlareEntity nearest(List<FlareEntity> flares, Vec3 from) {
      FlareEntity best = null;
      double bestDistance = Double.MAX_VALUE;
      for (FlareEntity flare : flares) {
         double distance = flare.position().distanceToSqr(from);
         if (distance < bestDistance) {
            bestDistance = distance;
            best = flare;
         }
      }

      return best;
   }

   static void forgetPlayer(Mob mob, ServerPlayer player) {
      if (mob.getTarget() == player) {
         mob.setTarget(null);
      }

      // Most goal-based mobs have no ATTACK_TARGET memory registered: getMemory would throw (crash on flares).
      net.minecraft.world.entity.ai.Brain<?> brain = mob.getBrain();
      if (brain.hasMemoryValue(MemoryModuleType.ATTACK_TARGET) && brain.getMemory(MemoryModuleType.ATTACK_TARGET).filter(t -> t == player).isPresent()) {
         brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
      }
   }

   static void countermeasuresTick(ServerPlayer player, IronManState state) {
      state.countermeasures.tick();
      if (state.countermeasures.forgetting().isEmpty()) {
         return;
      }

      for (int id : List.copyOf(state.countermeasures.forgetting())) {
         if (player.level().getEntity(id) instanceof Mob mob && mob.isAlive()) {
            forgetPlayer(mob, player);
         }
      }
   }

   // ---- missiles auto-lock (§11.1) ----

   /** No manual marks and the helmet closed: the nearest live threat gets the first missile. */
   @Nullable
   static Integer autoLock(ServerPlayer player, IronManState state) {
      if (!state.canMarkTargets()) {
         return null;
      }

      for (int id : state.threats) {
         Entity entity = player.level().getEntity(id);
         if (entity instanceof LivingEntity living && living.isAlive() && player.hasLineOfSight(living)) {
            return id;
         }
      }

      return null;
   }

   // ---- per tick ----

   static void tick(ServerPlayer player, IronManState state) {
      boolean worn = state.suit.worn();
      if (worn) {
         state.helmet.tick();
      } else if (!state.helmet.closed() && state.suit.state() == SuitState.DEPLOYING) {
         // Suit put on → helmet closed (spec §10).
         state.helmet.reset();
      }

      if (!worn && state.scan.active()) {
         state.scan.cancel();
      }

      ThreatScan.tick(player, state, worn && state.helmet.closed() && !IronManHero.controlled(player));
      scanTick(player, state);
      countermeasuresTick(player, state);
   }
}
