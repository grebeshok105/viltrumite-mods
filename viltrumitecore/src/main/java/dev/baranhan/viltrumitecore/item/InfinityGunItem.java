package dev.baranhan.viltrumitecore.item;

import dev.baranhan.viltrumitecore.ViltrumiteCore;
import dev.baranhan.viltrumitecore.client.render.InfinityGunRenderer;
import dev.baranhan.viltrumitecore.network.CoreMessages;
import dev.baranhan.viltrumitecore.network.packet.MeltedBlocksS2CPacket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public class InfinityGunItem extends Item {
   private static final int MAX_CHARGES = 25;
   private static final int CHARGE_PER_STAR = 5;

   public InfinityGunItem(Properties properties) {
      super(properties);
   }

   public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
      if (!(entity instanceof Player player) || !player.isShiftKeyDown()) {
         return super.onEntitySwing(stack, entity);
      }

      if (player.getCooldowns().isOnCooldown(this)) {
         return true;
      } else {
         Level level = player.level();
         CompoundTag nbt = stack.getOrCreateTag();
         int charges = nbt.getInt("Charges");
         if (charges <= 20) {
            int starSlot = -1;

            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
               if (player.getInventory().getItem(i).getItem() == Items.NETHER_STAR) {
                  starSlot = i;
                  break;
               }
            }

            if (starSlot == -1 && !player.isCreative()) {
               player.getCooldowns().addCooldown(this, 10);
               if (!level.isClientSide()) {
                  level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0F, 1.0F);
               }
            } else {
               nbt.putInt("ReloadTimer", 20);
               player.getCooldowns().addCooldown(this, 20);
               if (!level.isClientSide()) {
                  if (!player.isCreative()) {
                     player.getInventory().getItem(starSlot).shrink(1);
                  }

                  nbt.putInt("Charges", charges + 5);
               }
            }
         }

         return true;
      }
   }

   public Component getName(ItemStack stack) {
      return super.getName(stack).copy().withStyle(ChatFormatting.AQUA);
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
      super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
      int charges = stack.hasTag() ? stack.getTag().getInt("Charges") : 0;
      tooltipComponents.add(Component.translatable("tooltip.viltrumitecore.infinity_gun.charges", new Object[]{charges, 25}).withStyle(ChatFormatting.GRAY));
      tooltipComponents.add(Component.translatable("tooltip.viltrumitecore.infinity_gun").withStyle(ChatFormatting.DARK_GRAY));
   }

   public boolean onBlockStartBreak(ItemStack itemstack, BlockPos pos, Player player) {
      return player.isShiftKeyDown() ? true : super.onBlockStartBreak(itemstack, pos, player);
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (hand != InteractionHand.MAIN_HAND) {
         return InteractionResultHolder.pass(stack);
      } else if (player.getCooldowns().isOnCooldown(this)) {
         return InteractionResultHolder.fail(stack);
      } else {
         CompoundTag nbt = stack.getOrCreateTag();
         int charges = nbt.getInt("Charges");
         if (charges > 0) {
            if (!level.isClientSide()) {
               if (!player.isCreative()) {
                  nbt.putInt("Charges", charges - 1);
               }

               nbt.putInt("GunTimer", 14);
            }

            player.getCooldowns().addCooldown(this, 54);
            level.playSound(
               null,
               player.getX(),
               player.getY(),
               player.getZ(),
               (SoundEvent)ViltrumiteCore.INFINITY_GUN_SHOOT_EVENT.get(),
               SoundSource.PLAYERS,
               1.0F,
               0.9F + level.random.nextFloat() * 0.05F
            );
            return InteractionResultHolder.consume(stack);
         } else {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0F, 1.2F);
            player.getCooldowns().addCooldown(this, 10);
            return InteractionResultHolder.fail(stack);
         }
      }
   }

   public boolean isBarVisible(ItemStack stack) {
      return true;
   }

   public int getBarWidth(ItemStack stack) {
      int charges = stack.getOrCreateTag().getInt("Charges");
      return Math.round(13.0F * (float)charges / 25.0F);
   }

   public int getBarColor(ItemStack stack) {
      int charges = stack.getOrCreateTag().getInt("Charges");
      float f = Math.max(0.0F, (float)charges / 25.0F);
      return Mth.hsvToRgb(f * 0.5F, 1.0F, 1.0F);
   }

   public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
      if (entity instanceof Player player) {
         CompoundTag nbt = stack.getOrCreateTag();
         if (!level.isClientSide() && level instanceof ServerLevel serverWorld) {
            int reloadTimer = nbt.getInt("ReloadTimer");
            if (reloadTimer > 0) {
               nbt.putInt("ReloadTimer", reloadTimer - 1);
            }

            if (reloadTimer == 13) {
               level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 2.0F);
            }

            int timer = nbt.getInt("GunTimer");
            if (timer <= 0) {
               return;
            }

            if (!isSelected || player.getMainHandItem().getItem() != this) {
               nbt.putInt("GunTimer", 0);
               return;
            }

            nbt.putInt("GunTimer", --timer);
            Vec3 startPos = player.getEyePosition();
            Vec3 lookDir = player.getViewVector(1.0F);
            Vec3 endPos = startPos.add(lookDir.scale(128.0));
            Vec3 lineVec = endPos.subtract(startPos);
            double lineLenSq = lineVec.lengthSqr();
            float elapsed = 14.0F - (float)timer;
            float hitboxFade = Mth.clamp(1.0F - elapsed / 5.0F, 0.0F, 1.0F);
            if (hitboxFade <= 0.01F) {
               return;
            }

            Vec3 look = player.getLookAngle();
            player.setDeltaMovement(player.getDeltaMovement().add(look.x * -0.09, look.y * -0.09, look.z * -0.09));
            player.hurtMarked = true;
            double radius = 1.5 * (double)hitboxFade;
            double radiusSq = radius * radius;
            AABB scanBox = new AABB(startPos, endPos).inflate(radius);
            BlockPos minPos = BlockPos.containing(scanBox.minX, scanBox.minY, scanBox.minZ);
            BlockPos maxPos = BlockPos.containing(scanBox.maxX, scanBox.maxY, scanBox.maxZ);
            MutableBlockPos mutablePos = new MutableBlockPos();
            List<BlockPos> meltedThisTick = new ArrayList<>();
            int blocksBroken = 0;
            int maxBlocksPerTick = 450;

            for (int x = minPos.getX(); x <= maxPos.getX(); x++) {
               for (int y = minPos.getY(); y <= maxPos.getY(); y++) {
                  for (int z = minPos.getZ(); z <= maxPos.getZ(); z++) {
                     mutablePos.set(x, y, z);
                     Vec3 blockCenter = new Vec3((double)x + 0.5, (double)y + 0.5, (double)z + 0.5);
                     Vec3 pToStart = blockCenter.subtract(startPos);
                     double t = 0.0;
                     if (lineLenSq > 0.0) {
                        t = pToStart.dot(lineVec) / lineLenSq;
                        t = Mth.clamp(t, 0.0, 1.0);
                     }

                     Vec3 closestPoint = startPos.add(lineVec.scale(t));
                     double distSq = blockCenter.distanceToSqr(closestPoint);
                     if (distSq <= radiusSq) {
                        BlockState stateWorld = serverWorld.getBlockState(mutablePos);
                        if (!stateWorld.isAir() && blocksBroken < maxBlocksPerTick) {
                           if (!stateWorld.getFluidState().isEmpty()) {
                              serverWorld.setBlock(mutablePos, Blocks.AIR.defaultBlockState(), 3);
                              serverWorld.sendParticles(ParticleTypes.CLOUD, (double)x + 0.5, (double)y + 0.8, (double)z + 0.5, 2, 0.1, 0.1, 0.1, 0.05);
                           } else {
                              serverWorld.sendParticles(
                                 new BlockParticleOption(ParticleTypes.BLOCK, stateWorld),
                                 (double)x + 0.5,
                                 (double)y + 0.5,
                                 (double)z + 0.5,
                                 2,
                                 0.2,
                                 0.2,
                                 0.2,
                                 0.1
                              );
                              serverWorld.removeBlock(mutablePos, false);
                           }

                           meltedThisTick.add(mutablePos.immutable());
                           blocksBroken++;
                        }
                     }
                  }
               }
            }

            if (!meltedThisTick.isEmpty()) {
               CoreMessages.sendToTracking(new MeltedBlocksS2CPacket(meltedThisTick), player);
               CoreMessages.sendToPlayer(new MeltedBlocksS2CPacket(meltedThisTick), (ServerPlayer)player);
            }

            for (Entity e : serverWorld.getEntities(player, scanBox)) {
               if (e instanceof LivingEntity) {
                  LivingEntity living = (LivingEntity)e;
                  Vec3 entityCenter = living.position().add(0.0, (double)living.getBbHeight() / 2.0, 0.0);
                  Vec3 pToStartx = entityCenter.subtract(startPos);
                  double tx = 0.0;
                  if (lineLenSq > 0.0) {
                     tx = pToStartx.dot(lineVec) / lineLenSq;
                     tx = Mth.clamp(tx, 0.0, 1.0);
                  }

                  Vec3 closestPoint = startPos.add(lineVec.scale(tx));
                  double entityDistSq = entityCenter.distanceToSqr(closestPoint);
                  double hitRadius = radius + (double)living.getBbWidth() / 2.0;
                  if (entityDistSq <= hitRadius * hitRadius) {
                     living.invulnerableTime = 0;
                     living.hurt(serverWorld.damageSources().playerAttack(player), 100.0F);
                     living.invulnerableTime = 4;
                     living.setSecondsOnFire(2);
                  }
               }
            }
         }
      }
   }

   public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
      return oldStack.getItem() != newStack.getItem() || slotChanged;
   }

   public static boolean isFiring(ItemStack stack) {
      return stack != null && stack.hasTag() ? stack.getTag().getInt("GunTimer") > 0 : false;
   }

   public static boolean isCharging(ItemStack stack) {
      return false;
   }

   public void initializeClient(Consumer<IClientItemExtensions> consumer) {
      consumer.accept(new IClientItemExtensions() {
         private InfinityGunRenderer renderer;

         public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            if (this.renderer == null) {
               this.renderer = new InfinityGunRenderer();
            }

            return this.renderer;
         }
      });
   }
}
