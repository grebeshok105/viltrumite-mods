package dev.baranhan.viltrumitecore.mixin;

import dev.baranhan.viltrumitecore.ViltrumiteCore;
import dev.baranhan.viltrumitecore.network.CoreMessages;
import dev.baranhan.viltrumitecore.network.packet.BlockVFXS2CPacket;
import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin({Player.class})
public abstract class BlockDamageMixin {
   @ModifyVariable(
      method = {"hurt"},
      at = @At("HEAD"),
      argsOnly = true
   )
   private float applyViltrumiteBlock(float amount, DamageSource source) {
      Player player = (Player)(Object)this;
      if (player instanceof ViltrumiteCorePlayer corePlayer && corePlayer.isBlocking() && amount > 0.0F) {
         Vec3 sourcePos = source.getSourcePosition();
         if (sourcePos != null) {
            Vec3 lookVec = player.getLookAngle().normalize();
            Vec3 toSource = sourcePos.subtract(player.position()).normalize();
            if (lookVec.dot(toSource) > 0.0) {
               if (!player.level().isClientSide() && player.level() instanceof ServerLevel serverWorld) {
                  serverWorld.playSound(
                     null,
                     player.getX(),
                     player.getY(),
                     player.getZ(),
                     (SoundEvent)ViltrumiteCore.BLOCK_EVENT.get(),
                     SoundSource.PLAYERS,
                     1.5F,
                     1.4F
                  );
                  serverWorld.playSound(
                     null,
                     player.getX(),
                     player.getY(),
                     player.getZ(),
                     (SoundEvent)ViltrumiteCore.BLOCK_EVENT.get(),
                     SoundSource.PLAYERS,
                     1.5F,
                     1.3F
                  );
                  Entity attacker = source.getEntity();
                  float ringYaw = attacker != null ? attacker.getYRot() : player.getYRot();
                  float ringPitch = attacker != null ? attacker.getXRot() : player.getXRot();
                  Vec3 spawnPos = player.position().add(0.0, (double)player.getBbHeight() * 0.6, 0.0).add(lookVec.multiply(0.8, 0.8, 0.8));
                  CoreMessages.sendToTracking(new BlockVFXS2CPacket(spawnPos, ringYaw, ringPitch), player);
                  CoreMessages.sendToPlayer(new BlockVFXS2CPacket(spawnPos, ringYaw, ringPitch), (ServerPlayer)player);
               }

               return amount * 0.3F;
            }
         }
      }

      return amount;
   }
}
