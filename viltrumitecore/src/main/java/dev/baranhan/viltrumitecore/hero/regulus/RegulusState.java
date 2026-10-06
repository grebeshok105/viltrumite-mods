package dev.baranhan.viltrumitecore.hero.regulus;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Server-only transient Regulus state. Persistent data (hero id, session id,
 * totem) lives on HeroPlayer/HeroData instead; this object is rebuilt empty on
 * login and is discarded on death/disconnect/hero change.
 */
public final class RegulusState {
   // Timed action lock: the current cast. Event rules live in the abilities;
   // until eventFired cancellation is free and starts no cooldown.
   @Nullable
   public String actionId;
   public int actionElapsed;
   public int actionLength;
   public int actionEventTick;
   public int actionUnlockTick;
   public boolean eventFired;
   // Per-action context (dome point, counter target...) written by abilities.
   public Vec3 actionPoint;
   public UUID actionTargetId;

   public final Map<String, Integer> cooldowns = new HashMap<>();

   // Hearts (carriers are owner-private; only the count is public).
   public final Set<UUID> carriers = new LinkedHashSet<>();
   // The dimension each carrier was bound in: the heart lives there, so death
   // checks resolve the carrier in its own level even when the owner is
   // elsewhere (spec 5.3 — a death always burns, an unload drops silently).
   public final Map<UUID, ResourceLocation> carrierLevels = new LinkedHashMap<>();
   // Carriers the owner was last notified about; snapshot pushes dedupe on it.
   public final Set<UUID> pushedCarriers = new LinkedHashSet<>();
   public long nextCarrierScan;

   // Lion's Heart.
   public boolean lionActive;
   public int lionWindowMax;
   public int lionElapsed;
   public int overheatTicks;
   public long lionStartTick;
   public int lionWindowFloorHearts = -1;

   // Attacker record for Counter: last real living damage source.
   public UUID attackerId;
   public long attackerTick = Long.MIN_VALUE;
   public Vec3 attackerLastPos;

   // Evangelium / madness.
   public int ritualTicks = -1;
   public int madnessTicksLeft;
   public Vec3 ritualStartPos;

   // Mania channel.
   public UUID channelTargetId;
   public int channelTicks;

   // Super jump / landing.
   public boolean jumpHeld;
   public int jumpCharge;
   public boolean wasOnGround = true;
   public float lastFallDistance;

   // Damage bookkeeping for interruption rules.
   public float lastSeenHealth = -1.0F;

   public boolean busy() {
      return this.actionId != null;
   }

   public int hearts() {
      return this.carriers.size();
   }

   public int cooldownOf(String abilityId) {
      return this.cooldowns.getOrDefault(abilityId, 0);
   }

   public void startCooldown(String abilityId, int baseTicks) {
      this.cooldowns.put(abilityId, RegulusRules.cooldown(baseTicks, this.hearts()));
   }

   public void tickCooldowns() {
      this.cooldowns.replaceAll((id, ticks) -> Math.max(0, ticks - 1));
   }

   public void clearAction() {
      this.actionId = null;
      this.actionElapsed = 0;
      this.actionLength = 0;
      this.actionEventTick = 0;
      this.actionUnlockTick = 0;
      this.eventFired = false;
      this.actionPoint = null;
      this.actionTargetId = null;
   }

   public void beginAction(String actionId, int length, int eventTick, int unlockTick) {
      this.actionId = actionId;
      this.actionElapsed = 0;
      this.actionLength = length;
      this.actionEventTick = eventTick;
      this.actionUnlockTick = unlockTick;
      this.eventFired = false;
      this.actionPoint = null;
      this.actionTargetId = null;
   }
}
