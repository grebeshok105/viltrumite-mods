package dev.baranhan.viltrumitecore.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Axis;
import dev.baranhan.viltrumitecore.entity.MeteorEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard.TextureStateShard;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.data.ModelData;
import org.joml.Vector3f;

public class MeteorRenderer extends EntityRenderer<MeteorEntity> {
   private final BlockRenderDispatcher blockRenderer;

   public MeteorRenderer(Context context) {
      super(context);
      this.blockRenderer = context.getBlockRenderDispatcher();
   }

   public void render(MeteorEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      int glowingLight = 15728880;
      BlockState blockState = Blocks.MAGMA_BLOCK.defaultBlockState();
      float time = (float)entity.tickCount + partialTick;
      Vec3 velocity = entity.getDeltaMovement();
      double speed = velocity.length();
      Vector3f tumbleAxis = new Vector3f(1.0F, 0.6F, 0.3F).normalize();
      int ghostCount = 8;

      for (int i = ghostCount; i > 0; i--) {
         poseStack.pushPose();
         double offsetMultiplier = 1.5 * (double)i;
         poseStack.translate(-velocity.x * offsetMultiplier, -velocity.y * offsetMultiplier + 0.5, -velocity.z * offsetMultiplier);
         float ghostScale = 4.0F - (float)i * 0.2F;
         poseStack.scale(ghostScale, ghostScale, ghostScale);
         float yaw = (float)(Math.atan2(velocity.x, velocity.z) * (180.0 / Math.PI));
         poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
         float ghostTime = time - (float)i * 2.0F;
         float ghostAngle = (float)((double)ghostTime * speed * 10.0);
         poseStack.mulPose(Axis.of(tumbleAxis).rotationDegrees(ghostAngle));
         poseStack.translate(-0.5, -0.5, -0.5);
         float alpha = 0.5F - (float)i * 0.1F;
         if (alpha < 0.05F) {
            alpha = 0.05F;
         }

         this.renderTransparentBlock(blockState, poseStack, buffer, glowingLight, alpha);
         poseStack.popPose();
      }

      poseStack.pushPose();
      poseStack.translate(0.0, 0.5, 0.0);
      float scale = 4.0F;
      poseStack.scale(scale, scale, scale);
      float yaw = (float)(Math.atan2(velocity.x, velocity.z) * (180.0 / Math.PI));
      poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
      float mainAngle = (float)((double)time * speed * 10.0);
      poseStack.mulPose(Axis.of(tumbleAxis).rotationDegrees(mainAngle));
      poseStack.translate(-0.5, -0.5, -0.5);
      this.blockRenderer.renderSingleBlock(blockState, poseStack, buffer, glowingLight, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
      poseStack.popPose();
      super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
   }

   private void renderTransparentBlock(BlockState state, PoseStack poseStack, MultiBufferSource buffer, int packedLight, float alpha) {
      BakedModel bakedModel = this.blockRenderer.getBlockModel(state);
      VertexConsumer vertexConsumer = buffer.getBuffer(MeteorRenderer.CustomRenderTypes.METEOR_GHOST_TRAIL);
      Pose pose = poseStack.last();
      RandomSource random = RandomSource.create();

      for (Direction direction : Direction.values()) {
         random.setSeed(42L);

         for (BakedQuad quad : bakedModel.getQuads(state, direction, random, ModelData.EMPTY, null)) {
            vertexConsumer.putBulkData(pose, quad, 1.0F, 1.0F, 1.0F, alpha, packedLight, OverlayTexture.NO_OVERLAY, true);
         }
      }

      random.setSeed(42L);

      for (BakedQuad quad : bakedModel.getQuads(state, null, random, ModelData.EMPTY, null)) {
         vertexConsumer.putBulkData(pose, quad, 1.0F, 1.0F, 1.0F, alpha, packedLight, OverlayTexture.NO_OVERLAY, true);
      }
   }

   public ResourceLocation getTextureLocation(MeteorEntity entity) {
      return InventoryMenu.BLOCK_ATLAS;
   }

   private static class CustomRenderTypes extends RenderType {
      public static final RenderType METEOR_GHOST_TRAIL = create(
         "meteor_ghost_trail",
         DefaultVertexFormat.NEW_ENTITY,
         Mode.QUADS,
         256,
         false,
         true,
         CompositeState.builder()
            .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_CULL_SHADER)
            .setTextureState(new TextureStateShard(InventoryMenu.BLOCK_ATLAS, false, false))
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setCullState(NO_CULL)
            .setLightmapState(LIGHTMAP)
            .setOverlayState(OVERLAY)
            .setWriteMaskState(COLOR_DEPTH_WRITE)
            .createCompositeState(false)
      );

      private CustomRenderTypes(
         String name, VertexFormat format, Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState
      ) {
         super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
      }
   }
}
