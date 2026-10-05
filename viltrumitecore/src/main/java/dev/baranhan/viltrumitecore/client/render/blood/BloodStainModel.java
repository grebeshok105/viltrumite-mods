package dev.baranhan.viltrumitecore.client.render.blood;

import com.google.common.collect.UnmodifiableIterator;
import dev.baranhan.viltrumitecore.block.BloodStainBlock;
import dev.baranhan.viltrumitecore.block.ViltrumiteBlocks;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent.ModifyBakingResult;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

public class BloodStainModel implements BakedModel {
   private static final int VARIANT_GRID = 2;
   private static final float BASE_HEIGHT = 0.009F;
   private static final float LAYER_STEP = 0.0013F;
   private static final int NORMAL_UP = packNormal(0.0F, 1.0F, 0.0F);
   private static final int COLOR_WHITE = -1;
   private static final int[] SPLATS_BY_AGE = new int[]{5, 4, 3, 2};
   private static final float[] SCALE_BY_AGE = new float[]{1.0F, 0.86F, 0.72F, 0.56F};
   private final BakedModel original;
   private final int age;
   private final float sink;

   public BloodStainModel(BakedModel original, int age, int sinkSixteenths) {
      this.original = original;
      this.age = Mth.clamp(age, 0, 3);
      this.sink = (float)Mth.clamp(sinkSixteenths, 0, 8) / 16.0F;
   }

   public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
      if (side != null) {
         return Collections.emptyList();
      } else {
         TextureAtlasSprite sprite = this.original.getParticleIcon();
         int count = SPLATS_BY_AGE[this.age];
         float sizeMul = SCALE_BY_AGE[this.age];
         List<BakedQuad> quads = new ArrayList<>(count);

         for (int i = 0; i < count; i++) {
            float angle = random.nextFloat() * (float) (Math.PI * 2);
            float half = (0.17F + random.nextFloat() * 0.24F) * sizeMul;
            float margin = half * 0.75F;
            float cx = margin + random.nextFloat() * (1.0F - 2.0F * margin);
            float cz = margin + random.nextFloat() * (1.0F - 2.0F * margin);
            float y = 0.009F + (float)i * 0.0013F - this.sink;
            int variant = random.nextInt(4);
            boolean mirror = random.nextBoolean();
            quads.add(splat(sprite, cx, cz, y, half, angle, variant, mirror));
         }

         return quads;
      }
   }

   private static BakedQuad splat(TextureAtlasSprite sprite, float cx, float cz, float y, float half, float angle, int variant, boolean mirror) {
      float sin = Mth.sin(angle);
      float cos = Mth.cos(angle);
      float[][] local = new float[][]{{-half, half}, {half, half}, {half, -half}, {-half, -half}};
      int cell = 2;
      float step = 1.0F / (float)cell;
      float u0 = (float)(variant % cell) * step;
      float v0 = (float)(variant / cell) * step;
      float u1 = u0 + step;
      float v1 = v0 + step;
      if (mirror) {
         float tmp = u0;
         u0 = u1;
         u1 = tmp;
      }

      float[][] uv = new float[][]{{u0, v1}, {u1, v1}, {u1, v0}, {u0, v0}};
      int[] data = new int[32];

      for (int i = 0; i < 4; i++) {
         float lx = local[i][0];
         float lz = local[i][1];
         float x = cx + lx * cos - lz * sin;
         float z = cz + lx * sin + lz * cos;
         putVertex(data, i, x, y, z, sprite.getU((double)uv[i][0] * 16.0), sprite.getV((double)uv[i][1] * 16.0));
      }

      return new BakedQuad(data, -1, Direction.UP, sprite, false);
   }

   private static void putVertex(int[] data, int index, float x, float y, float z, float u, float v) {
      int i = index * 8;
      data[i] = Float.floatToRawIntBits(x);
      data[i + 1] = Float.floatToRawIntBits(y);
      data[i + 2] = Float.floatToRawIntBits(z);
      data[i + 3] = -1;
      data[i + 4] = Float.floatToRawIntBits(u);
      data[i + 5] = Float.floatToRawIntBits(v);
      data[i + 6] = 0;
      data[i + 7] = NORMAL_UP;
   }

   private static int packNormal(float x, float y, float z) {
      int nx = (int)(x * 127.0F) & 0xFF;
      int ny = (int)(y * 127.0F) & 0xFF;
      int nz = (int)(z * 127.0F) & 0xFF;
      return nx | ny << 8 | nz << 16;
   }

   public boolean useAmbientOcclusion() {
      return false;
   }

   public boolean isGui3d() {
      return false;
   }

   public boolean usesBlockLight() {
      return false;
   }

   public boolean isCustomRenderer() {
      return false;
   }

   public TextureAtlasSprite getParticleIcon() {
      return this.original.getParticleIcon();
   }

   public ItemTransforms getTransforms() {
      return ItemTransforms.NO_TRANSFORMS;
   }

   public ItemOverrides getOverrides() {
      return ItemOverrides.EMPTY;
   }

   @EventBusSubscriber(
      modid = "viltrumitecore",
      bus = Bus.MOD,
      value = {Dist.CLIENT}
   )
   public static final class Swapper {
      @SubscribeEvent
      public static void onModifyBakingResult(ModifyBakingResult event) {
         Map<ResourceLocation, BakedModel> models = event.getModels();
         UnmodifiableIterator var2 = ((Block)ViltrumiteBlocks.BLOOD_STAIN.get()).getStateDefinition().getPossibleStates().iterator();

         while (var2.hasNext()) {
            BlockState state = (BlockState)var2.next();
            ModelResourceLocation key = BlockModelShaper.stateToModelLocation(state);
            BakedModel original = models.get(key);
            if (original != null) {
               models.put(key, new BloodStainModel(original, (Integer)state.getValue(BloodStainBlock.AGE), (Integer)state.getValue(BloodStainBlock.SINK)));
            }
         }
      }

      private Swapper() {
      }
   }
}
