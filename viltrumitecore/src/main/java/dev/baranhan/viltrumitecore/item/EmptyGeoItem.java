package dev.baranhan.viltrumitecore.item;

import dev.baranhan.viltrumitecore.client.anim.AnimCache;
import dev.baranhan.viltrumitecore.client.render.EmptyGeoItemRenderer;
import java.util.function.Consumer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.registries.ForgeRegistries;

public class EmptyGeoItem extends Item {
   private final ResourceLocation texture;
   private ResourceLocation geoPath;

   public EmptyGeoItem(Properties properties) {
      this(properties, AnimCache.itemTexture("infinity_gun"));
   }

   public EmptyGeoItem(Properties properties, ResourceLocation texture) {
      super(properties);
      this.texture = texture;
   }

   public ResourceLocation texture() {
      return this.texture;
   }

   public ResourceLocation geoPath() {
      if (this.geoPath == null) {
         ResourceLocation key = ForgeRegistries.ITEMS.getKey(this);
         this.geoPath = key == null ? AnimCache.itemGeo("missing") : new ResourceLocation(key.getNamespace(), "geo/item/" + key.getPath() + ".geo.json");
      }

      return this.geoPath;
   }

   public void initializeClient(Consumer<IClientItemExtensions> consumer) {
      consumer.accept(new IClientItemExtensions() {
         public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            return EmptyGeoItemRenderer.get();
         }
      });
   }
}
