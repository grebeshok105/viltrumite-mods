package dev.baranhan.viltrumitecore.util;

import dev.baranhan.viltrumiteflight.mixin.FallingBlockEntityInvoker;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ThunderClapManager {
   public static final List<ThunderClapManager.ActiveClap> ACTIVE_CLAPS = new ArrayList<>();

   public static void startThunderclap(ServerPlayer player) {
      Vec3 origin = player.position().add(0.0, 0.5, 0.0);
      float yawRad = player.getYRot() * (float) (Math.PI / 180.0);
      Vec3 direction = new Vec3((double)(-Mth.sin(yawRad)), 0.0, (double)Mth.cos(yawRad)).normalize();
      ACTIVE_CLAPS.add(new ThunderClapManager.ActiveClap(player, origin, direction));
   }

   public static void tickThunderclaps(ServerLevel world) {
      if (!ACTIVE_CLAPS.isEmpty()) {
         Iterator<ThunderClapManager.ActiveClap> iterator = ACTIVE_CLAPS.iterator();

         while (iterator.hasNext()) {
            ThunderClapManager.ActiveClap clap = iterator.next();
            clap.age++;
            if (clap.age > 10) {
               iterator.remove();
            } else {
               double minDist = (double)(clap.age - 1) * 3.0;
               double maxDist = (double)clap.age * 3.0;
               double currentRadius = 3.0 + 7.0 * ((double)clap.age / 10.0);
               Vec3 sliceCenter = clap.origin.add(clap.direction.scale((minDist + maxDist) / 2.0));
               AABB searchBox = new AABB(
                  sliceCenter.x - currentRadius,
                  sliceCenter.y - currentRadius,
                  sliceCenter.z - currentRadius,
                  sliceCenter.x + currentRadius,
                  sliceCenter.y + currentRadius,
                  sliceCenter.z + currentRadius
               );

               for (LivingEntity target : world.getEntitiesOfClass(LivingEntity.class, searchBox, e -> e != clap.attacker)) {
                  Vec3 toTarget = target.getBoundingBox().getCenter().subtract(clap.origin);
                  double distanceAlongAxis = toTarget.dot(clap.direction);
                  if (distanceAlongAxis >= minDist && distanceAlongAxis <= maxDist) {
                     double distFromAxisSq = toTarget.lengthSqr() - distanceAlongAxis * distanceAlongAxis;
                     if (distFromAxisSq <= currentRadius * currentRadius) {
                        if (clap.attacker instanceof ViltrumiteStatHolder statHolder) {
                           target.hurt(world.damageSources().playerAttack(clap.attacker), statHolder.getBaseDamage());
                        }

                        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1));
                        Vec3 pushDir = target.position().subtract(clap.origin).normalize();
                        target.setDeltaMovement(pushDir.x * 2.5, 1.2, pushDir.z * 2.5);
                        target.hasImpulse = true;
                     }
                  }
               }

               BlockPos minPos = BlockPos.containing(searchBox.minX, searchBox.minY, searchBox.minZ);
               BlockPos maxPos = BlockPos.containing(searchBox.maxX, searchBox.maxY, searchBox.maxZ);
               MutableBlockPos mutablePos = new MutableBlockPos();

               for (int x = minPos.getX(); x <= maxPos.getX(); x++) {
                  for (int y = minPos.getY(); y <= maxPos.getY(); y++) {
                     for (int z = minPos.getZ(); z <= maxPos.getZ(); z++) {
                        mutablePos.set(x, y, z);
                        Vec3 blockCenter = new Vec3((double)x + 0.5, (double)y + 0.5, (double)z + 0.5);
                        Vec3 toBlock = blockCenter.subtract(clap.origin);
                        double distAlongAxis = toBlock.dot(clap.direction);
                        if (distAlongAxis >= minDist && distAlongAxis <= maxDist) {
                           double distFromAxisSq = toBlock.lengthSqr() - distAlongAxis * distAlongAxis;
                           if (distFromAxisSq <= currentRadius * currentRadius) {
                              BlockState state = world.getBlockState(mutablePos);
                              if (!state.isAir() && state.getFluidState().isEmpty() && state.getDestroySpeed(world, mutablePos) >= 0.0F) {
                                 BlockState upState = world.getBlockState(mutablePos.above());
                                 if (upState.isAir() || upState.canBeReplaced()) {
                                    if (world.random.nextFloat() < 0.25F) {
                                       FallingBlockEntity fallingBlock = FallingBlockEntityInvoker.invokeConstructor(
                                          world, (double)x + 0.5, (double)y + 0.5, (double)z + 0.5, state
                                       );
                                       fallingBlock.dropItem = false;
                                       fallingBlock.time = 1;
                                       Vec3 outward = new Vec3((double)x + 0.5 - clap.origin.x, 0.0, (double)z + 0.5 - clap.origin.z).normalize();
                                       double rawVelX = outward.x * 0.8;
                                       double rawVelY = 0.7 + world.random.nextDouble() * 0.5;
                                       double rawVelZ = outward.z * 0.8;
                                       double syncedVelX = (double)Math.round(rawVelX * 8000.0) / 8000.0;
                                       double syncedVelY = (double)Math.round(rawVelY * 8000.0) / 8000.0;
                                       double syncedVelZ = (double)Math.round(rawVelZ * 8000.0) / 8000.0;
                                       fallingBlock.setDeltaMovement(syncedVelX, syncedVelY, syncedVelZ);
                                       fallingBlock.hasImpulse = true;
                                       world.setBlock(mutablePos, Blocks.AIR.defaultBlockState(), 3);
                                       world.addFreshEntity(fallingBlock);
                                    } else {
                                       world.destroyBlock(mutablePos, false);
                                    }

                                    world.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, (double)x + 0.5, (double)y + 1.0, (double)z + 0.5, 3, 0.5, 0.5, 0.5, 0.05);
                                 }
                              }
                           }
                        }
                     }
                  }
               }

               if (clap.age % 3 == 0) {
                  world.playSound(null, BlockPos.containing(sliceCenter), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0F, 0.6F);
               }
            }
         }
      }
   }

   public static class ActiveClap {
      public ServerPlayer attacker;
      public Vec3 origin;
      public Vec3 direction;
      public int age;

      public ActiveClap(ServerPlayer attacker, Vec3 origin, Vec3 direction) {
         this.attacker = attacker;
         this.origin = origin;
         this.direction = direction;
         this.age = 0;
      }
   }
}
