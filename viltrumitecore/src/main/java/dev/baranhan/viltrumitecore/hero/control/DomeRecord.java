package dev.baranhan.viltrumitecore.hero.control;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * World-owned Greed's Embrace dome. Lives in the level manager, never in
 * RegulusState: it outlives the caster's death but not a dimension unload.
 */
public record DomeRecord(UUID id, UUID caster, ResourceKey<Level> dimension, Vec3 center, double radius, long createdAt, long expiresAt, Set<UUID> captured) {
   public static DomeRecord create(UUID caster, ResourceKey<Level> dimension, Vec3 center, double radius, long now, long duration, Set<UUID> captured) {
      return new DomeRecord(UUID.randomUUID(), caster, dimension, center, radius, now, now + duration, ConcurrentHashMap.newKeySet());
   }
}
