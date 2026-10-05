package dev.baranhan.viltrumitecore.client.event;

import dev.baranhan.viltrumitecore.item.ViltrumiteItems;
import dev.baranhan.viltrumitecore.potion.ViltrumitePotions;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(
   modid = "viltrumitecore",
   bus = Bus.FORGE,
   value = {Dist.CLIENT}
)
public final class ScourgeTooltips {
   @SubscribeEvent
   public static void onItemTooltip(ItemTooltipEvent event) {
      ItemStack stack = event.getItemStack();
      if (stack.is((Item)ViltrumiteItems.SCOURGE_CULTURE.get())) {
         event.getToolTip().add(Component.translatable("tooltip.viltrumitecore.scourge_culture").withStyle(ChatFormatting.DARK_GRAY));
      } else {
         Potion potion = PotionUtils.getPotion(stack);
         if (potion == ViltrumitePotions.SCOURGE_VIRUS.get()
            || potion == ViltrumitePotions.LONG_SCOURGE_VIRUS.get()
            || potion == ViltrumitePotions.STRONG_SCOURGE_VIRUS.get()) {
            event.getToolTip().add(Component.translatable("tooltip.viltrumitecore.scourge_viltrumites_only").withStyle(ChatFormatting.RED));
         }
      }
   }

   private ScourgeTooltips() {
   }
}
