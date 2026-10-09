package dev.baranhan.viltrumitecore.hero.ironman;

import dev.baranhan.viltrumitecore.hero.CleanupReason;
import dev.baranhan.viltrumitecore.hero.HeroId;
import dev.baranhan.viltrumitecore.hero.HeroPlayer;
import dev.baranhan.viltrumitecore.hero.ironman.combat.*;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/** Server-side per-player Iron Man state. Suit and energy survive relog (spec §16). */
public final class IronManState {
   private static final String KEY = "IronMan";
   private static final String FLARE_COOLDOWN_KEY = "FlareCooldown";
   public final Suit suit = new Suit();
   public final Energy energy = new Energy();
   /** Energy hit 0 in flight: glide profile (Task 9). */
   public boolean glide;
   /** Transient: touchdown classification and air strike arming (Task 9). */
   public final LandingTracker landing = new LandingTracker();
   public final AirStrike airStrike = new AirStrike();
   /** Transient: kneel-pose ticks left (HEAVY_LANDING flag). */
   public int heavyPoseTicks;
   /** Transient: game time of the last flight touchdown effect (no double effect with onLanded). */
   public long landedAt = Long.MIN_VALUE;
   /** Transient: sonic-ram rehit timestamps per target (bounded). */
   public final java.util.Map<java.util.UUID, Long> ramHits = new dev.baranhan.viltrumitecore.hero.control.BoundedMap<>(64);
   /** Transient: ticks until the next fly-by punch may land. */
   public int flyByCooldown;

   // ---- Stage 2: nano combat (plan Tasks 1-10) ----
   /** Persisted (NBT CoreOverheats): only a core explosion or death resets it. */
   public final CoreOverheat overheat = new CoreOverheat();
   /** Transient: current RMB tool (server authority; synced in RMB_TOOL bits). */
   public RightTool rightTool = RightTool.REPULSOR;
   /** Transient: the tool the held RMB started with (release goes there). */
   @Nullable
   public RightTool heldTool;
   public final Repulsor repulsor = new Repulsor();
   public int recoilTicks;
   public boolean recoilRight;
   public final UnibeamTimeline unibeam = new UnibeamTimeline();
   public final Overdraft overdraft = new Overdraft();
   /** Beam direction (slow turn) and the synced beam end point. */
   @Nullable
   public net.minecraft.world.phys.Vec3 beamDir;
   @Nullable
   public net.minecraft.world.phys.Vec3 beamEnd;
   /** Own core explosion window: damage from it is floored (spec §9.4). */
   public int coreExplosionWindow;
   public final MissileLock missiles = new MissileLock();
   public final NanoArsenal arsenal = new NanoArsenal();
   /** LMB weapon strike pose timeline. */
   public int strikeTicks;
   public int strikeLength;
   /** Hammer RMB charge ticks; -1 = not charging. */
   public int hammerCharge = -1;
   /** Blade dash: ticks left and the path. */
   public int dashTicks;
   @Nullable
   public net.minecraft.world.phys.Vec3 dashFrom;
   @Nullable
   public net.minecraft.world.phys.Vec3 dashTo;
   public int dashTargetId = -1;
   public final HammerLaunch hammerLaunch = new HammerLaunch();
   public final Shield shield = new Shield();
   /** Projectiles to redirect next tick after a perfect block (entity id → speed). */
   public final java.util.Map<Integer, Double> reflect = new java.util.HashMap<>();
   /** Nano damage zones (bit 0 mask, 1 shoulder, 2 chest) and repair ticks left. */
   public int damagedZones;
   public int repairTicks;

   // ---- Stage 3: helmet, JARVIS, scan, countermeasures ----
   /** Persisted (NBT HelmetOpen). */
   public final Helmet helmet = new Helmet();
   /** Transient: JARVIS threat ids (nearest first), mirrored in the THREATS owner section. */
   public final java.util.List<Integer> threats = new java.util.ArrayList<>();
   public final dev.baranhan.viltrumitecore.hero.ironman.scan.ScanProgress scan = new dev.baranhan.viltrumitecore.hero.ironman.scan.ScanProgress();
   /** Cooldown carries over death (extraCooldowns[1]). */
   public final Countermeasures countermeasures = new Countermeasures();

   /** Missile marks need the JARVIS targeting: helmet closed (spec §10). */
   public boolean canMarkTargets() {
      return this.helmet.closed();
   }

   /** Flight grant: only while fully worn. */
   public boolean wantsFlight() {
      return this.suit.worn();
   }

   /** Pure part of HeroDefinition.cleanup (spec §16). */
   public void onCleanup(CleanupReason reason) {
      this.landing.reset();
      this.airStrike.consume();
      this.heavyPoseTicks = 0;
      this.ramHits.clear();
      this.stopCombat();
      this.scan.cancel();
      this.threats.clear();
      this.countermeasures.clearForget();
      switch (reason) {
         case DEATH -> {
            this.suit.clear();
            this.glide = false;
            // Spec §9.1/§16: death cancels a pending core explosion and resets the counter.
            this.overdraft.cancel();
            this.overheat.resetByDeath();
         }
         case DISCONNECT -> {
            this.suit.resolve();
            this.overdraft.cancel();
         }
         case HERO_CHANGE -> {
            this.suit.clear();
            this.energy.reset();
            this.glide = false;
            this.overdraft.cancel();
            this.overheat.resetByDeath();
            this.suit.setNanoLock(0);
            this.countermeasures.setCooldown(0);
            this.helmet.reset();
         }
      }
   }

   /**
    * Every combat channel off at once, without shots (cleanup, suit lost).
    * The overdraft is not touched: it ends only by explosion, death or hero change.
    */
   public void stopCombat() {
      this.repulsor.cancel();
      this.heldTool = null;
      this.recoilTicks = 0;
      if (!this.overdraft.active()) {
         this.unibeam.clear();
      }

      this.beamDir = null;
      this.beamEnd = null;
      this.missiles.cancel();
      this.arsenal.clear();
      this.rightTool = RightTool.REPULSOR;
      this.strikeTicks = 0;
      this.hammerCharge = -1;
      this.dashTicks = 0;
      this.hammerLaunch.clear();
      this.shield.lower();
      this.reflect.clear();
      this.damagedZones = 0;
      this.repairTicks = 0;
   }

   /** Death: suit off, energy full; cooldowns carry over (nano lock), the overheat counter resets. */
   public static IronManState cloneForRespawn(IronManState original) {
      IronManState state = new IronManState();
      state.suit.setNanoLock(original.suit.nanoLockTicks());
      state.countermeasures.setCooldown(original.countermeasures.cooldown());
      return state;
   }

   /** End exit portal (clone without death): the suit stays on (spec §16), only live channels drop. */
   public static IronManState cloneForPortal(IronManState original) {
      CompoundTag nbt = new CompoundTag();
      original.save(nbt);
      IronManState state = new IronManState();
      state.load(nbt);
      return state;
   }

   public void save(CompoundTag nbt) {
      CompoundTag tag = new CompoundTag();
      this.suit.save(tag);
      this.energy.save(tag);
      this.overheat.save(tag);
      this.helmet.save(tag);
      tag.putInt(FLARE_COOLDOWN_KEY, this.countermeasures.cooldown());
      nbt.put(KEY, tag);
   }

   public void load(CompoundTag nbt) {
      CompoundTag tag = nbt.getCompound(KEY);
      this.suit.load(tag);
      this.energy.load(tag);
      this.overheat.load(tag);
      this.helmet.load(tag);
      this.countermeasures.setCooldown(tag.getInt(FLARE_COOLDOWN_KEY));
      this.glide = false;
   }

   @Nullable
   public static IronManState of(@Nullable Player player) {
      if (player instanceof HeroPlayer heroPlayer && heroPlayer.getHeroId() == HeroId.IRON_MAN) {
         return heroPlayer.viltrumitecore$getHeroState() instanceof IronManState state ? state : null;
      }

      return null;
   }

   public static IronManState ensure(Player player) {
      HeroPlayer heroPlayer = (HeroPlayer)player;
      if (heroPlayer.viltrumitecore$getHeroState() instanceof IronManState existing) {
         return existing;
      }

      IronManState state = new IronManState();
      heroPlayer.viltrumitecore$setHeroState(state);
      return state;
   }
}
