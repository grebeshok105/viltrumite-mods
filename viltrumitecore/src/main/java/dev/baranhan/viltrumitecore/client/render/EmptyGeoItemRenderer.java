package dev.baranhan.viltrumitecore.client.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.baranhan.viltrumitecore.client.anim.AnimCache;
import dev.baranhan.viltrumitecore.client.anim.geo.BakedGeoModel;
import dev.baranhan.viltrumitecore.client.anim.render.AnimRenderer;
import dev.baranhan.viltrumitecore.item.EmptyGeoItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class EmptyGeoItemRenderer extends BlockEntityWithoutLevelRenderer {
   private static EmptyGeoItemRenderer instance;

   public static EmptyGeoItemRenderer get() {
      if (instance == null) {
         instance = new EmptyGeoItemRenderer();
      }

      return instance;
   }

   private EmptyGeoItemRenderer() {
      super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
   }

   public void renderByItem(ItemStack stack, ItemDisplayContext transform, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
      if (stack.getItem() instanceof EmptyGeoItem item) {
         BakedGeoModel model = AnimCache.model(item.geoPath());
         if (model != null) {
            model.resetBones();
            boolean gui = transform == ItemDisplayContext.GUI;
            BufferSource guiBufferSource = null;
            if (gui) {
               Lighting.setupForFlatItems();
               guiBufferSource = bufferSource instanceof BufferSource bs ? bs : Minecraft.getInstance().renderBuffers().bufferSource();
            }

            RenderType renderType = RenderType.entityCutoutNoCull(item.texture());
            VertexConsumer buffer = ItemRenderer.getFoilBufferDirect(bufferSource, renderType, gui, stack.hasFoil());
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.51F, 0.5F);
            AnimRenderer.render(model, poseStack, buffer, packedLight, packedOverlay);
            poseStack.popPose();
            if (gui) {
               guiBufferSource.endBatch();
               RenderSystem.enableDepthTest();
               Lighting.setupFor3DItems();
            }
         }
      }
   }
}
