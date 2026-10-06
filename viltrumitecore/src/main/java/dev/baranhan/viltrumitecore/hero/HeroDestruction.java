package dev.baranhan.viltrumitecore.hero;

import dev.baranhan.viltrumitecore.config.ViltrumiteCoreConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Approved world-destruction boundary for hero abilities (spec 7.2, plan
 * Task 3). Same rules as the Viltrumite impact destruction: unbreakable
 * blocks (bedrock) survive, the mobGriefing game rule is honored, drops use
 * the shared punch drop chance and removals emit block-shard particles.
 */
public final class HeroDestruction {
   private HeroDestruction() {
   }

   /** True when hero abilities may remove the block at pos. */
   public static boolean canDestroy(ServerLevel level, BlockPos pos) {
      if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
         return false;
      }

      BlockState state = level.getBlockState(pos);
      return !state.isAir() && state.getDestroySpeed(level, pos) >= 0.0F;
   }

   /**
    * Removes one block the Viltrumite way: optional resource drop, air fill
    * and a block-particle burst. Returns false when canDestroy rejects it.
    */
   public static boolean destroyBlock(ServerLevel level, BlockPos pos) {
      if (!canDestroy(level, pos)) {
         return false;
      }

      BlockState state = level.getBlockState(pos);
      if (level.random.nextFloat() < ViltrumiteCoreConfig.INSTANCE.punchBlockDropChance / 100.0F) {
         Block.dropResources(state, level, pos);
      }

      level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
      level.sendParticles(
         new BlockParticleOption(ParticleTypes.BLOCK, state),
         (double)pos.getX() + 0.5,
         (double)pos.getY() + 0.5,
         (double)pos.getZ() + 0.5,
         15,
         0.25,
         0.25,
         0.25,
         0.05
      );
      return true;
   }
}
