package dev.baranhan.viltrumiteflight.network.packet;

import dev.baranhan.viltrumiteflight.config.ViltrumiteConfig;
import dev.baranhan.viltrumiteflight.mixin.FallingBlockEntityInvoker;
import dev.baranhan.viltrumiteflight.registry.ModSounds;
import dev.baranhan.viltrumiteflight.util.FlightState;
import dev.baranhan.viltrumiteflight.util.ViltrumiteFlightPlayer;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkEvent.Context;

public class FlightToggleC2SPacket {
   public FlightToggleC2SPacket() {
   }

   public FlightToggleC2SPacket(FriendlyByteBuf buf) {
   }

   public void toBytes(FriendlyByteBuf buf) {
   }

   public boolean handle(Supplier<Context> supplier) {
      Context context = supplier.get();
      context.enqueueWork(
         () -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
               ViltrumiteFlightPlayer viltrumiteFlightPlayer = (ViltrumiteFlightPlayer)player;
               FlightState currentState = viltrumiteFlightPlayer.getFlightState();
               if (currentState != FlightState.NONE || player.getAbilities().mayfly) {
                  FlightState newState = currentState == FlightState.NONE ? FlightState.HOVER : FlightState.NONE;
                  viltrumiteFlightPlayer.setFlightState(newState);
                  if (newState == FlightState.HOVER && player.isCrouching()) {
                     viltrumiteFlightPlayer.setTakeoffTicks(5);
                     ServerLevel world = (ServerLevel)player.level();
                     world.sendParticles(ParticleTypes.POOF, player.getX(), player.getY(), player.getZ(), 40, 0.5, 0.2, 0.5, 0.15);
                     world.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(), 20, 0.4, 0.1, 0.4, 0.1);
                     world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.5F, 1.2F);
                     world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.PLAYERS, 1.8F, 1.2F);
                     world.playSound(
                        null, player.getX(), player.getY(), player.getZ(), (SoundEvent)ModSounds.TAKEOFF.get(), SoundSource.PLAYERS, 1.8F, 1.2F
                     );
                     if (ViltrumiteConfig.INSTANCE.breakBlocksOnTakeoff) {
                        int radius = 3;
                        BlockPos playerPos = player.blockPosition();

                        for (int x = -radius; x <= radius; x++) {
                           for (int z = -radius; z <= radius; z++) {
                              if (x * x + z * z <= radius * radius) {
                                 BlockPos targetPos = null;
                                 BlockState groundState = null;

                                 for (int yOffset = 2; yOffset >= -4; yOffset--) {
                                    BlockPos checkPos = playerPos.offset(x, yOffset, z);
                                    BlockState state = world.getBlockState(checkPos);
                                    if (!state.isAir() && state.getFluidState().isEmpty()) {
                                       float hardness = state.getDestroySpeed(world, checkPos);
                                       if (hardness > 0.0F && hardness < 50.0F) {
                                          targetPos = checkPos;
                                          groundState = state;
                                          break;
                                       }

                                       if (hardness == 0.0F) {
                                          world.removeBlock(checkPos, false);
                                       }
                                    }
                                 }

                                 if (targetPos != null && groundState != null) {
                                    FallingBlockEntity debris = FallingBlockEntityInvoker.invokeConstructor(
                                       world,
                                       (double)targetPos.getX() + 0.5,
                                       (double)targetPos.getY(),
                                       (double)targetPos.getZ() + 0.5,
                                       groundState
                                    );
                                    debris.dropItem = false;
                                    debris.time = 1;
                                    double dirX = (double)x;
                                    double dirZ = (double)z;
                                    double dist = Math.sqrt(dirX * dirX + dirZ * dirZ);
                                    if (dist > 0.0) {
                                       dirX /= dist;
                                       dirZ /= dist;
                                    }

                                    double upwardSpeed = 0.4 + world.random.nextDouble() * 0.6;
                                    double outwardSpeed = 0.1 + world.random.nextDouble() * 0.15;
                                    debris.setDeltaMovement(dirX * outwardSpeed, upwardSpeed, dirZ * outwardSpeed);
                                    debris.hasImpulse = true;
                                    world.addFreshEntity(debris);
                                    world.removeBlock(targetPos, false);
                                 }
                              }
                           }
                        }
                     }
                  }

                  player.getAbilities().flying = newState == FlightState.HOVER;
                  player.onUpdateAbilities();
               }
            }
         }
      );
      return true;
   }
}
