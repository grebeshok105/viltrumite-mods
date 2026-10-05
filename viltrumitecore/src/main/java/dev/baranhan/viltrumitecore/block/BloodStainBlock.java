package dev.baranhan.viltrumitecore.block;

import dev.baranhan.viltrumitecore.item.ViltrumiteItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BloodStainBlock extends Block {
   public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
   public static final IntegerProperty SINK = IntegerProperty.create("sink", 0, 8);
   public static final BooleanProperty VILTRUMITE = BooleanProperty.create("viltrumite");
   private static final int DECAY_TICKS = 900;
   private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 0.25, 16.0);

   public BloodStainBlock(Properties properties) {
      super(properties);
      this.registerDefaultState(
         (BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(AGE, 0)).setValue(SINK, 0)).setValue(VILTRUMITE, true)
      );
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{AGE, SINK, VILTRUMITE});
   }

   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
      BlockPos below = pos.below();
      VoxelShape shape = level.getBlockState(below).getCollisionShape(level, below);
      return shape.isEmpty() ? false : shape.max(Axis.Y) >= 0.5;
   }

   public static int sinkFor(BlockGetter level, BlockPos stainPos) {
      BlockPos below = stainPos.below();
      VoxelShape shape = level.getBlockState(below).getCollisionShape(level, below);
      if (shape.isEmpty()) {
         return 0;
      } else {
         double top = shape.max(Axis.Y);
         return Mth.clamp((int)Math.round((1.0 - top) * 16.0), 0, 8);
      }
   }

   public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
      if (direction != Direction.DOWN) {
         return state;
      } else {
         return !this.canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : (BlockState)state.setValue(SINK, sinkFor(level, pos));
      }
   }

   public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
      level.scheduleTick(pos, this, 900);
   }

   public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      int age = (Integer)state.getValue(AGE);
      if (age >= 3) {
         level.removeBlock(pos, false);
      } else {
         level.setBlock(pos, (BlockState)state.setValue(AGE, age + 1), 2);
         level.scheduleTick(pos, this, 900);
      }
   }

   public static void refresh(ServerLevel level, BlockPos pos, BlockState state) {
      if ((Integer)state.getValue(AGE) != 0) {
         level.setBlock(pos, (BlockState)state.setValue(AGE, 0), 2);
      }

      level.scheduleTick(pos, state.getBlock(), 900);
   }

   public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      ItemStack held = player.getItemInHand(hand);
      if (!held.is(Items.GLASS_BOTTLE)) {
         return InteractionResult.PASS;
      } else {
         if (!level.isClientSide) {
            if (!player.getAbilities().instabuild) {
               held.shrink(1);
            }

            ItemStack sample = new ItemStack(
               state.getValue(VILTRUMITE) ? (ItemLike)ViltrumiteItems.VILTRUMITE_BLOOD_SAMPLE.get() : (ItemLike)ViltrumiteItems.HUMAN_BLOOD_SAMPLE.get()
            );
            if (!player.addItem(sample)) {
               player.drop(sample, false);
            }

            level.removeBlock(pos, false);
            level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.9F, 1.15F);
            ((ServerLevel)level)
               .sendParticles(
                  ParticleTypes.CRIMSON_SPORE,
                  (double)pos.getX() + 0.5,
                  (double)pos.getY() + 0.15,
                  (double)pos.getZ() + 0.5,
                  6,
                  0.25,
                  0.02,
                  0.25,
                  0.0
               );
         }

         return InteractionResult.sidedSuccess(level.isClientSide);
      }
   }

   public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
      return ItemStack.EMPTY;
   }
}
