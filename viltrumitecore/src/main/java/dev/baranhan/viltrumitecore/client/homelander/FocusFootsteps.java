package dev.baranhan.viltrumitecore.client.homelander;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Louder footsteps of focus targets, from their client-side walked distance (spec §6.4). */
final class FocusFootsteps {
   private static final double STRIDE = 1.6;
   private static final float VOLUME = 0.9F;
   private static final Map<Integer, Track> TRACKS = new HashMap<>();

   private FocusFootsteps() {
   }

   static void tick(List<Integer> targets) {
      Minecraft client = Minecraft.getInstance();
      ClientLevel level = client.level;
      TRACKS.keySet().retainAll(targets);
      if (level == null || client.isPaused()) {
         return;
      }

      for (int id : targets) {
         Entity entity = level.getEntity(id);
         if (entity == null) {
            continue;
         }

         Vec3 pos = entity.position();
         Track track = TRACKS.computeIfAbsent(id, key -> new Track(pos));
         double dx = pos.x - track.last.x;
         double dz = pos.z - track.last.z;
         track.last = pos;
         if (!entity.onGround() || entity.isSilent()) {
            continue;
         }

         track.walked += Math.sqrt(dx * dx + dz * dz);
         if (track.walked < STRIDE) {
            continue;
         }

         track.walked = 0.0;
         BlockPos below = BlockPos.containing(pos.x, pos.y - 0.2, pos.z);
         BlockState state = level.getBlockState(below);
         if (state.isAir()) {
            continue;
         }

         SoundType type = state.getSoundType(level, below, entity);
         level.playLocalSound(pos.x, pos.y, pos.z, type.getStepSound(), SoundSource.NEUTRAL, type.getVolume() * VOLUME, type.getPitch() * 0.9F, false);
      }
   }

   private static final class Track {
      Vec3 last;
      double walked;

      Track(Vec3 last) {
         this.last = last;
      }
   }
}
