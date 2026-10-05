package dev.baranhan.viltrumitecore.potion;

import dev.baranhan.viltrumitecore.item.ViltrumiteItems;
import javax.annotation.Nonnull;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.brewing.IBrewingRecipe;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@EventBusSubscriber(
   modid = "viltrumitecore",
   bus = Bus.MOD
)
public final class ScourgeBrewing {
   @SubscribeEvent
   public static void onCommonSetup(FMLCommonSetupEvent event) {
      event.enqueueWork(() -> {
         register(Potions.AWKWARD, (Item)ViltrumiteItems.SCOURGE_CULTURE.get(), (Potion)ViltrumitePotions.SCOURGE_VIRUS.get());
         register((Potion)ViltrumitePotions.SCOURGE_VIRUS.get(), Items.REDSTONE, (Potion)ViltrumitePotions.LONG_SCOURGE_VIRUS.get());
         register((Potion)ViltrumitePotions.SCOURGE_VIRUS.get(), Items.GLOWSTONE_DUST, (Potion)ViltrumitePotions.STRONG_SCOURGE_VIRUS.get());
      });
   }

   private static void register(Potion from, Item ingredient, Potion to) {
      BrewingRecipeRegistry.addRecipe(new ScourgeBrewing.ScourgeRecipe(from, ingredient, to));
   }

   private ScourgeBrewing() {
   }

   private static record ScourgeRecipe(Potion from, Item ingredient, Potion to) implements IBrewingRecipe {
      public boolean isInput(@Nonnull ItemStack input) {
         return isPotionContainer(input.getItem()) && PotionUtils.getPotion(input) == this.from;
      }

      public boolean isIngredient(@Nonnull ItemStack stack) {
         return stack.is(this.ingredient);
      }

      @Nonnull
      public ItemStack getOutput(@Nonnull ItemStack input, @Nonnull ItemStack ingredientStack) {
         return this.isInput(input) && this.isIngredient(ingredientStack) ? PotionUtils.setPotion(new ItemStack(input.getItem()), this.to) : ItemStack.EMPTY;
      }

      private static boolean isPotionContainer(Item item) {
         return item == Items.POTION || item == Items.SPLASH_POTION || item == Items.LINGERING_POTION;
      }
   }
}
