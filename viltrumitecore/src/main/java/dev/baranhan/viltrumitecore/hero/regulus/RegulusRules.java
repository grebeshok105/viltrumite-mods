package dev.baranhan.viltrumitecore.hero.regulus;

/**
 * Pure numeric rules for Regulus (spec §5-§12). No Minecraft objects in
 * signatures so JUnit exercises production math directly.
 */
public final class RegulusRules {
   public static final int MAX_HEARTS = 12;
   public static final int HEART_SCAN_PERIOD = 20;
   public static final double HEART_SCAN_RADIUS = 20.0;
   public static final int WEAKNESS_TICKS_ON_HEART_DEATH = 60;
   public static final String HEARTLESS_TAG = "heartless";

   /** Base chassis (user balance pass): 60 HP total, 20 armor, fist ~3. */
   public static final double BASE_ARMOR = 20.0;
   public static final double BONUS_MAX_HEALTH = 40.0;
   public static final double BONUS_ATTACK = 2.0;

   public static final int LION_WINDUP_TICKS = 14;
   /** Overheat drains once per second, never per tick. */
   public static final int LION_OVERHEAT_PERIOD = 20;
   /** Lion switches itself off after this much overheat (3 drains). */
   public static final int LION_OVERHEAT_MAX_TICKS = 60;
   /** A burned/lost heart never cuts the window to less than this from now. */
   public static final int LION_HEART_LOSS_GRACE = 40;
   public static final int LION_BASE_WINDOW = 60;
   public static final int LION_WINDOW_PER_HEART = 40;
   public static final int LION_MANUAL_COOLDOWN = 100;
   public static final int LION_FORCED_COOLDOWN = 600;
   public static final float LION_FORCED_OFF_HP = 4.0F;
   public static final double LION_PROJECTILE_RADIUS = 4.0;
   public static final double LION_REPULSE_RADIUS = 4.0;
   public static final double LION_RELEASE_IMPULSE = 1.5;

   public static final int DEBRIS_ANIM_TICKS = 44;
   public static final int DEBRIS_EVENT_TICK = 14;
   public static final int DEBRIS_RISE_TICK = 11;
   public static final double DEBRIS_RANGE = 24.0;
   /** Shotgun spray: shard count and a gaussian spread clamped to the cone. */
   public static final int DEBRIS_SHARDS = 20;
   public static final double DEBRIS_CONE_DEGREES = 50.0;
   public static final double DEBRIS_YAW_SIGMA = 11.0;
   public static final double DEBRIS_MIN_ELEVATION = -3.0;
   public static final double DEBRIS_MAX_ELEVATION = 12.0;
   public static final double DEBRIS_ELEVATION_SIGMA = 4.5;
   /** Visual shard speed in blocks per tick (damage still resolves instantly). */
   public static final double DEBRIS_VISUAL_SPEED = 7.0;
   public static final float DEBRIS_PITCH_UP_LIMIT = -30.0F;
   public static final float DEBRIS_PITCH_DOWN_LIMIT = 30.0F;
   public static final int DEBRIS_SHARD_PIERCE = 2;
   public static final float DEBRIS_SHARD_DAMAGE = 2.0F;
   public static final int DEBRIS_MAX_SHARDS_PER_TARGET = 4;
   public static final double DEBRIS_KNOCKBACK = 1.3;
   public static final int DEBRIS_COOLDOWN = 400;

   public static final int MANIA_WINDUP_TICKS = 19;
   public static final double MANIA_TARGET_RANGE = 100.0;
   public static final double MANIA_PULL_PER_TICK = 0.6;
   public static final int MANIA_CHANNEL_TICKS = 120;
   public static final int MANIA_FREEZE_TICKS = 80;
   public static final int MANIA_COOLDOWN = 500;

   public static final int EMBRACE_LOCK_TICK = 13;
   public static final int EMBRACE_APPEAR_TICK = 18;
   public static final int EMBRACE_RECOVER_TICK = 32;
   public static final double EMBRACE_RANGE = 40.0;
   public static final double EMBRACE_RADIUS = 8.0;
   public static final int EMBRACE_DURATION_TICKS = 80;
   public static final int EMBRACE_COOLDOWN = 700;
   public static final double EMBRACE_RELEASE_IMPULSE = 1.2;

   public static final int COUNTER_LIFT_TICKS = 20;
   public static final int COUNTER_SLAM_TICKS = 7;
   /** Uppercut lands on this tick and throws the target into the sky. */
   public static final int COUNTER_LAUNCH_TICK = 3;
   /** How high the target is thrown (blocks above its start). */
   public static final double COUNTER_LAUNCH_HEIGHT = 14.0;
   /** Speed of the slam dive, blocks per tick. */
   public static final double COUNTER_DIVE_SPEED = 3.0;
   /** Longest dive before the slam is resolved where the target is. */
   public static final int COUNTER_DIVE_MAX_TICKS = 20;
   /** Gaze fallback range when nobody attacked Regulus recently. */
   public static final double COUNTER_GAZE_RANGE = 24.0;
   public static final double COUNTER_BEHIND_DISTANCE = 1.5;
   public static final int COUNTER_ATTACKER_WINDOW = 240;
   public static final double COUNTER_RANGE = 40.0;
   public static final int COUNTER_CRATER_DEPTH = 5;
   public static final int COUNTER_CRATER_RADIUS = 3;
   public static final int COUNTER_COOLDOWN = 800;
   public static final float COUNTER_BASE_DAMAGE = 15.0F;
   public static final float COUNTER_CAP = 45.0F;

   public static final int RITUAL_TICKS = 60;
   public static final int RITUAL_CANCEL_COOLDOWN = 400;
   public static final int MADNESS_TICKS = 900;
   public static final float BLOOD_PRICE_PER_20_TICKS = 0.6F;
   public static final int EVANGELIUM_COOLDOWN = 1800;

   public static final int JUMP_CHARGE_TICKS = 60;
   public static final float JUMP_MAX_VELOCITY = 1.32F;
   /** Holding jump shorter than this is a normal hop, not a charged launch. */
   public static final int JUMP_MIN_CHARGE_TICKS = 5;
   public static final float SHOCKWAVE_MIN_FALL = 8.0F;
   public static final double SHOCKWAVE_RADIUS = 5.0;
   public static final double SHOCKWAVE_MAX_RADIUS = 12.0;
   public static final float SHOCKWAVE_DAMAGE = 5.0F;
   public static final float SHOCKWAVE_MAX_DAMAGE = 14.0F;

   /** Regulus fist (Viltrumite punch pipeline, own damage). */
   public static final float PUNCH_DAMAGE = 14.0F;

   /** Madness: the authority runs wild. */
   public static final float MADNESS_PUNCH_MULTIPLIER = 1.5F;
   public static final double MADNESS_SHOCKWAVE_MULTIPLIER = 1.5;
   public static final double MADNESS_SHARD_MULTIPLIER = 1.5;
   public static final int MADNESS_SHARD_PIERCE = 4;
   /** Cooldowns run this many ticks per server tick while mad. */
   public static final int MADNESS_COOLDOWN_RATE = 2;
   /** Air blade: a punch in madness freezes the air in front into a blade. */
   public static final double AIR_BLADE_RANGE = 22.0;
   public static final double AIR_BLADE_HALF_WIDTH = 1.6;
   public static final float AIR_BLADE_DAMAGE = 10.0F;
   public static final int AIR_BLADE_CUT_DEPTH = 1;

   private RegulusRules() {
   }

   public static int clampHearts(int hearts) {
      return Math.max(0, Math.min(MAX_HEARTS, hearts));
   }

   /**
    * Spec 5.1: a carrier is a vanilla living, non-player, non-Enemy, untagged
    * real creature (Mob). Decoration entities like armor stands are not
    * "живое существо" — otherwise they grant free permanent hearts.
    */
   public static boolean carrierEligible(boolean vanillaNamespace, boolean player, boolean hostile, boolean heartless, boolean creature) {
      // Hostile mobs carry hearts too (an angry golem, a zombie): only
      // players, decorations and heartless-tagged entities are refused.
      return vanillaNamespace && creature && !player && !heartless;
   }

   /** Spec 6.4: the release impulse pushes opponents only, never allies or own carriers. */
   public static boolean repulseTarget(boolean opponent, boolean allied, boolean ownCarrier) {
      return opponent && !allied && !ownCarrier;
   }

   /**
    * Full push gate for the Lion release impulse and equivalent shoves: the
    * opponent filter must pass, the target must accept external impulses
    * (spec 6.2 — a Lion-active Regulus is never shoved) and must not already
    * be pinned by a control (spec 8.3/9.2).
    */
   public static boolean repulsePushable(boolean opponent, boolean allied, boolean ownCarrier, boolean impulseAllowed, boolean anchored) {
      return repulseTarget(opponent, allied, ownCarrier) && impulseAllowed && !anchored;
   }

   /**
    * Section-4 landing shockwave stays flat: the +2%/heart bonus is scoped to
    * abilities and melee (spec 5.4), so hearts are accepted but ignored.
    */
   public static float shockwaveDamage(int hearts) {
      return SHOCKWAVE_DAMAGE;
   }

   /** 0 at the minimum fall, 1 at a 32-block drop: drives radius, damage and FX. */
   public static float shockwavePower(float fallDistance) {
      return Math.max(0.0F, Math.min(1.0F, (fallDistance - SHOCKWAVE_MIN_FALL) / 24.0F));
   }

   public static double shockwaveRadius(float fallDistance, boolean madness) {
      double radius = SHOCKWAVE_RADIUS + (SHOCKWAVE_MAX_RADIUS - SHOCKWAVE_RADIUS) * shockwavePower(fallDistance);
      return madness ? radius * MADNESS_SHOCKWAVE_MULTIPLIER : radius;
   }

   public static float shockwaveDamage(float fallDistance, boolean madness) {
      float damage = SHOCKWAVE_DAMAGE + (SHOCKWAVE_MAX_DAMAGE - SHOCKWAVE_DAMAGE) * shockwavePower(fallDistance);
      return madness ? damage * (float)MADNESS_SHOCKWAVE_MULTIPLIER : damage;
   }

   public static int debrisShardCount(boolean madness) {
      return madness ? (int)Math.round(DEBRIS_SHARDS * MADNESS_SHARD_MULTIPLIER) : DEBRIS_SHARDS;
   }

   public static int debrisPierce(boolean madness) {
      return madness ? MADNESS_SHARD_PIERCE : DEBRIS_SHARD_PIERCE;
   }

   public static float punchDamage(int hearts, boolean madness) {
      float damage = PUNCH_DAMAGE * heartBonus(hearts);
      return madness ? damage * MADNESS_PUNCH_MULTIPLIER : damage;
   }

   public static float airBladeDamage(int hearts) {
      return AIR_BLADE_DAMAGE * heartBonus(hearts);
   }

   /**
    * Shotgun spread: a gaussian sample (sigma in degrees) clamped to
    * [-limit, limit]. Pure so the spray shape is testable.
    */
   public static double clampedSpread(double gaussian, double sigma, double limit) {
      return Math.max(-limit, Math.min(limit, gaussian * sigma));
   }

   /** Shard elevation: low and forward, a long tail of higher shards. */
   public static double shardElevation(double gaussian) {
      double e = DEBRIS_MIN_ELEVATION + 2.0 + gaussian * DEBRIS_ELEVATION_SIGMA;
      return Math.max(DEBRIS_MIN_ELEVATION, Math.min(DEBRIS_MAX_ELEVATION, e));
   }

   /** Remaining ticks of a stashed external Slowness once our own slow ends. */
   public static int slownessRemainder(int savedTicks, int elapsedTicks) {
      return Math.max(0, savedTicks - elapsedTicks);
   }

   /**
    * A new cast needs its slot equipped and off cooldown; the Lion off-toggle
    * is not a new cast and always passes (spec 6.4 "повторное нажатие").
    */
   public static boolean mayStartAbility(boolean equippedOnActivePage, boolean onCooldown, boolean lionOffToggle) {
      return lionOffToggle || (equippedOnActivePage && !onCooldown);
   }

   /** cd = ceil(base * (1 - 0.03 * H)), H sampled when the cooldown starts. */
   public static int cooldown(int base, int hearts) {
      int h = clampHearts(hearts);
      return (int)Math.ceil(base * (1.0 - 0.03 * h));
   }

   public static float heartBonus(int hearts) {
      return 1.0F + 0.02F * clampHearts(hearts);
   }

   public static int lionWindow(int hearts) {
      return LION_BASE_WINDOW + LION_WINDOW_PER_HEART * clampHearts(hearts);
   }

   /** Recomputed every tick, shrink-only: lost hearts shorten, new never extend. */
   public static int shrinkLionWindow(int currentWindow, int previousHearts, int newHearts) {
      int lost = Math.max(0, clampHearts(previousHearts) - clampHearts(newHearts));
      return Math.max(0, currentWindow - lost * LION_WINDOW_PER_HEART);
   }

   public static float overheatDps(int overheatTicks) {
      return 1.5F + 0.5F * (float)(Math.max(0, overheatTicks) / 40);
   }

   /** Overheat drain due this tick: one second worth, on every full period. */
   public static float overheatDrain(int overheatTicks) {
      return overheatTicks > 0 && overheatTicks % LION_OVERHEAT_PERIOD == 0 ? overheatDps(overheatTicks) : 0.0F;
   }

   public static boolean overheatExhausted(int overheatTicks) {
      return overheatTicks >= LION_OVERHEAT_MAX_TICKS;
   }

   /** How an active Lion's Heart ends after this tick's drains, if at all. */
   public enum LionEnd {
      NONE,
      /** HP collapsed to the floor: forced off, LION_FORCED_COOLDOWN. */
      FORCED,
      /** Overheat ran its course with HP above the floor: LION_MANUAL_COOLDOWN. */
      EXHAUSTED
   }

   /** HP collapse always wins over overheat exhaustion on the same tick. */
   public static LionEnd lionEndAfterTick(float health, int overheatTicks) {
      if (lionForcedOff(health)) {
         return LionEnd.FORCED;
      }

      return overheatExhausted(overheatTicks) ? LionEnd.EXHAUSTED : LionEnd.NONE;
   }

   /** Heart loss shortens the window, but always leaves a short warning grace. */
   public static int windowAfterHeartLoss(int previousWindow, int shrunkWindow, int elapsed) {
      if (shrunkWindow >= previousWindow) {
         return shrunkWindow;
      }

      return Math.max(shrunkWindow, Math.min(previousWindow, elapsed + LION_HEART_LOSS_GRACE));
   }

   public static boolean lionForcedOff(float health) {
      return health <= LION_FORCED_OFF_HP;
   }

   public static int lionCooldownBase(boolean forced) {
      return forced ? LION_FORCED_COOLDOWN : LION_MANUAL_COOLDOWN;
   }

   /**
    * Damage of the n-th shard (0-based) that lands on one target in one kick:
    * flat per shard, capped at DEBRIS_MAX_SHARDS_PER_TARGET, hearts scale it.
    */
   public static float debrisShardDamage(int shardsAlreadyLanded, int hearts) {
      if (shardsAlreadyLanded < 0 || shardsAlreadyLanded >= DEBRIS_MAX_SHARDS_PER_TARGET) {
         return 0.0F;
      }

      return DEBRIS_SHARD_DAMAGE * heartBonus(hearts);
   }

   /** min(45, 15 + 15% maxHP) * (1 + 0.02H) */
   public static float counterDamage(float targetMaxHealth, int hearts) {
      return Math.min(COUNTER_CAP, COUNTER_BASE_DAMAGE + 0.15F * targetMaxHealth) * heartBonus(hearts);
   }

   public static float freezeDeferredCap(float maxHealth) {
      return 0.4F * maxHealth;
   }

   public static float domeDeferredCap(float maxHealth) {
      return 0.35F * maxHealth;
   }

   /** Deferred queue math: damage accumulates but never crosses the cap. */
   public static float deferredAccumulate(float queued, float incoming, float cap) {
      return Math.min(cap, Math.max(0.0F, queued + incoming));
   }

   /** Spec 8.2: the channel ends at 120 ticks at the latest. */
   public static boolean maniaChannelDone(int channelTicks) {
      return channelTicks >= MANIA_CHANNEL_TICKS;
   }

   /** Spec 8.2: only the fast flight states outrun the pull; hover/ground cannot. */
   public static boolean pullEscapes(dev.baranhan.viltrumiteflight.util.FlightState state) {
      return state == dev.baranhan.viltrumiteflight.util.FlightState.CRUISE
         || state == dev.baranhan.viltrumiteflight.util.FlightState.SONIC;
   }

   public static float heartBacklash(float maxHealth) {
      return 0.1F * maxHealth;
   }

   public static float jumpVelocity(int chargeTicks) {
      float charge = Math.min(chargeTicks, JUMP_CHARGE_TICKS) / (float)JUMP_CHARGE_TICKS;
      return 0.42F + (JUMP_MAX_VELOCITY - 0.42F) * charge;
   }

   public static boolean attackerValid(long attackerTick, long nowTick) {
      return nowTick - attackerTick <= COUNTER_ATTACKER_WINDOW;
   }

   /**
    * HP lost to EXTERNAL causes only: hero-internal drains (blood price, heart
    * backlash, overheat) recorded since the last read are subtracted so
    * self-inflicted damage never passes as an interrupting hit (spec 7.1/10/11).
    */
   public static float externalHealthLoss(float previousHealth, float currentHealth, float internalDamage) {
      return Math.max(0.0F, previousHealth - currentHealth - Math.max(0.0F, internalDamage));
   }
}
