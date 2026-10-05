package dev.baranhan.viltrumitecore.client.render.blood;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.baranhan.viltrumitecore.block.ViltrumiteBlocks;
import dev.baranhan.viltrumitecore.entity.BloodDropEntity;
import dev.baranhan.viltrumitecore.entity.ViltrumiteEntities;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class BloodDropRenderer extends EntityRenderer<BloodDropEntity> {
   private static final ResourceLocation VILTRUMITE_TEXTURE = new ResourceLocation("viltrumitecore", "textures/entity/viltrumite_blood_drop.png");
   private static final ResourceLocation HUMAN_TEXTURE = new ResourceLocation("viltrumitecore", "textures/entity/human_blood_drop.png");
   private static final float WIDTH = 0.055F;
   private static final float MIN_LENGTH = 0.055F;
   private static final float STRETCH = 0.55F;
   private static final float MAX_LENGTH = 0.42F;
   private static final int MIN_BLOCK_LIGHT = 5;

   public BloodDropRenderer(Context context) {
      super(context);
   }

   public void render(BloodDropEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
      Vector3f motion = new Vector3f((float)entity.getDeltaMovement().x, (float)entity.getDeltaMovement().y, (float)entity.getDeltaMovement().z);
      float speed = motion.length();
      float length = Math.min(0.055F + speed * 0.55F, 0.42F);
      poseStack.pushPose();
      Matrix3f worldNormal = new Matrix3f(poseStack.last().normal());
      Quaternionf camera = this.entityRenderDispatcher.cameraOrientation();
      float angle = 0.0F;
      if (speed > 1.0E-4F) {
         Vector3f screenMotion = new Vector3f(motion);
         new Quaternionf(camera).conjugate().transform(screenMotion);
         angle = (float)Math.atan2((double)screenMotion.x, (double)screenMotion.y);
      }

      poseStack.mulPose(camera);
      poseStack.mulPose(Axis.ZP.rotation(-angle));
      Matrix4f position = poseStack.last().pose();
      int light = LightTexture.pack(Math.max(LightTexture.block(packedLight), 5), LightTexture.sky(packedLight));
      ResourceLocation texture = entity.isViltrumiteBlood() ? VILTRUMITE_TEXTURE : HUMAN_TEXTURE;
      VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));
      vertex(buffer, position, worldNormal, light, -0.055F, -length, 0.0F, 1.0F);
      vertex(buffer, position, worldNormal, light, 0.055F, -length, 1.0F, 1.0F);
      vertex(buffer, position, worldNormal, light, 0.055F, length, 1.0F, 0.0F);
      vertex(buffer, position, worldNormal, light, -0.055F, length, 0.0F, 0.0F);
      poseStack.popPose();
      super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
   }

   private static void vertex(VertexConsumer buffer, Matrix4f position, Matrix3f worldNormal, int light, float x, float y, float u, float v) {
      buffer.vertex(position, x, y, 0.0F)
         .color(255, 255, 255, 255)
         .uv(u, v)
         .overlayCoords(OverlayTexture.NO_OVERLAY)
         .uv2(light)
         .normal(worldNormal, 0.0F, 1.0F, 0.0F)
         .endVertex();
   }

   public ResourceLocation getTextureLocation(BloodDropEntity entity) {
      return entity.isViltrumiteBlood() ? VILTRUMITE_TEXTURE : HUMAN_TEXTURE;
   }

   @EventBusSubscriber(
      modid = "viltrumitecore",
      bus = Bus.MOD,
      value = {Dist.CLIENT}
   )
   public static final class ClientSetup {
      @SubscribeEvent
      public static void onRegisterRenderers(RegisterRenderers event) {
         event.registerEntityRenderer((EntityType)ViltrumiteEntities.BLOOD_DROP.get(), BloodDropRenderer::new);
      }

      @SubscribeEvent
      public static void onClientSetup(FMLClientSetupEvent event) {
         event.enqueueWork(() -> ItemBlockRenderTypes.setRenderLayer((Block)ViltrumiteBlocks.BLOOD_STAIN.get(), RenderType.cutout()));
      }

      private ClientSetup() {
      }
   }
}
