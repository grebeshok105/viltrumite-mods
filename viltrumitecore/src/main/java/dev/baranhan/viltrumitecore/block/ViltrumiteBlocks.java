package dev.baranhan.viltrumitecore.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ViltrumiteBlocks {
   public static final String MOD_ID = "viltrumitecore";
   public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "viltrumitecore");
   public static final RegistryObject<Block> BLOOD_STAIN = BLOCKS.register(
      "viltrumite_blood_stain",
      () -> new BloodStainBlock(
            Properties.of()
               .mapColor(MapColor.COLOR_RED)
               .replaceable()
               .noCollission()
               .instabreak()
               .noOcclusion()
               .pushReaction(PushReaction.DESTROY)
               .sound(SoundType.WET_GRASS)
         )
   );

   public static void register(IEventBus modEventBus) {
      BLOCKS.register(modEventBus);
   }

   private ViltrumiteBlocks() {
   }
}
