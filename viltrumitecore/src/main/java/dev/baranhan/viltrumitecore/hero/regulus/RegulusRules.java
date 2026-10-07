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

   public static final int LION_WINDUP_TICKS = 14;
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
   public static final int DEBRIS_RAYS = 9;
   public static final double DEBRIS_CONE_DEGREES = 35.0;
   public static final double DEBRIS_RANGE = 16.0;
   public static final int DEBRIS_BLOCK_QUOTA = 3;
   public static final int DEBRIS_COOLDOWN = 400;
   // Ray path cell kinds for debrisRayTraversal.
   public static final int DEBRIS_BLOCK_AIR = 0;
   public static final int DEBRIS_BLOCK_BREAKABLE = 1;
   public static final int DEBRIS_BLOCK_UNBREAKABLE = 2;

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
   public static final double COUNTER_LIFT_VELOCITY = 0.3;
   public static final double COUNTER_BEHIND_DISTANCE = 1.5;
   public static final int COUNTER_ATTACKER_WINDOW = 240;
   public static final double COUNTER_RANGE = 40.0;
   public static final int COUNTER_CRATER_DEPTH = 8;
   public static final int COUNTER_CRATER_RADIUS = 3;
   public static final int COUNTER_COOLDOWN = 800;
   public static final float COUNTER_BASE_DAMAGE = 15.0F;
   public static final float COUNTER_CAP = 45.0F;

   public static final int RITUAL_TICKS = 60;
   public static final int RITUAL_CANCEL_COOLDOWN = 400;
   public static final int MADNESS_TICKS = 900;
   public static final float BLOOD_PRICE_PER_20_TICKS = 0.6F;
   public static final int EVANGELIUM_COOLDOWN = 1800;
   public static final float RITUAL_INTERRUPT_DAMAGE = 4.0F;

   public static final int JUMP_CHARGE_TICKS = 60;
   public static final float JUMP_MAX_VELOCITY = 1.32F;
   public static final float SHOCKWAVE_MIN_FALL = 8.0F;
   public static final double SHOCKWAVE_RADIUS = 5.0;
   public static final float SHOCKWAVE_DAMAGE = 5.0F;

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
      return vanillaNamespace && creature && !player && !hostile && !heartless;
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

   public static boolean lionForcedOff(float health) {
      return health <= LION_FORCED_OFF_HP;
   }

   public static int lionCooldownBase(boolean forced) {
      return forced ? LION_FORCED_COOLDOWN : LION_MANUAL_COOLDOWN;
   }

   /** 7 -> 2 linear falloff between d=4 and d=16, flat 7 inside 4. */
   public static float debrisDamage(double distance, int hearts) {
      float base;
      if (distance <= 4.0) {
         base = 7.0F;
      } else if (distance >= DEBRIS_RANGE) {
         base = 2.0F;
      } else {
         base = 7.0F - 5.0F * (float)(distance - 4.0) / 12.0F;
      }

      return base * heartBonus(hearts);
   }

   /**
    * Ray fan offsets (spec 7.2): returns DEBRIS_RAYS [yawDeg, pitchDeg] pairs.
    * Index 0 is the center ray along the view; the other eight sit on the
    * cone edge at half the cone angle, spaced evenly around it.
    */
   public static double[][] debrisRayOffsets() {
      double[][] offsets = new double[DEBRIS_RAYS][2];
      double half = DEBRIS_CONE_DEGREES / 2.0;
      for (int i = 1; i < DEBRIS_RAYS; i++) {
         double azimuth = Math.toRadians((i - 1) * 360.0 / (DEBRIS_RAYS - 1));
         offsets[i][0] = half * Math.cos(azimuth);
         offsets[i][1] = half * Math.sin(azimuth);
      }

      return offsets;
   }

   /**
    * How far a ray travels through an ordered list of solid blocks on its
    * path. Each breakable cell consumes one of DEBRIS_BLOCK_QUOTA; an
    * unbreakable cell or the (quota+1)-th solid stops the ray before it.
    * hitIndex bounds the walk: it is the count of solid cells in front of the
    * first living target (cells behind the target are not on the path).
    * Returns the number of leading cells the ray passes — those are the cells
    * to destroy. The entity is hit iff the return equals hitIndex.
    */
   public static int debrisRayTraversal(int[] solidCells, int hitIndex) {
      int bound = Math.min(solidCells.length, Math.max(0, hitIndex));
      int broken = 0;
      for (int i = 0; i < bound; i++) {
         if (solidCells[i] == DEBRIS_BLOCK_UNBREAKABLE) {
            return i;
         }

         if (solidCells[i] == DEBRIS_BLOCK_BREAKABLE) {
            if (broken >= DEBRIS_BLOCK_QUOTA) {
               return i;
            }

            broken++;
         }
      }

      return bound;
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

   /** The held-book rework does not interrupt Evangelium on external damage. */
   public static boolean ritualDamageInterrupts(float lostHealth, boolean lionActive) {
      return false;
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
