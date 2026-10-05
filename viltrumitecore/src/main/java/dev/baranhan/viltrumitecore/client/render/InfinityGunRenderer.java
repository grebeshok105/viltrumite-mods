package dev.baranhan.viltrumitecore.client.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.baranhan.viltrumitecore.client.anim.AnimCache;
import dev.baranhan.viltrumitecore.client.anim.animation.Animation;
import dev.baranhan.viltrumitecore.client.anim.animation.AnimationController;
import dev.baranhan.viltrumitecore.client.anim.geo.BakedGeoModel;
import dev.baranhan.viltrumitecore.client.anim.geo.GeoBone;
import dev.baranhan.viltrumitecore.client.anim.render.AnimRenderer;
import dev.baranhan.viltrumitecore.item.InfinityGunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class InfinityGunRenderer extends BlockEntityWithoutLevelRenderer {
   private static final String ASSET = "infinity_gun";
   private static final ResourceLocation GEO = AnimCache.itemGeo("infinity_gun");
   private static final ResourceLocation ANIMS = AnimCache.itemAnimations("infinity_gun");
   private static final ResourceLocation TEXTURE = AnimCache.itemTexture("infinity_gun");
   private final AnimationController controller = new AnimationController("main").transitionLength(0.0F);
   private final InfinityGunRenderer.PendingArm rightArm = new InfinityGunRenderer.PendingArm();
   private final InfinityGunRenderer.PendingArm leftArm = new InfinityGunRenderer.PendingArm();
   private boolean firstPerson;

   public InfinityGunRenderer() {
      super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
   }

   public void renderByItem(ItemStack stack, ItemDisplayContext transform, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
      BakedGeoModel model = AnimCache.model(GEO);
      if (model != null) {
         this.firstPerson = transform == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || transform == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
         boolean thirdPerson = transform == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND || transform == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
         this.rightArm.queued = false;
         this.leftArm.queued = false;
         if (this.firstPerson || thirdPerson) {
            this.updateAnimation();
         }

         float partialTick = Minecraft.getInstance().getFrameTime();
         this.controller.apply(model, AnimRenderer.time(partialTick));
         boolean gui = transform == ItemDisplayContext.GUI;
         BufferSource guiBufferSource = null;
         if (gui) {
            Lighting.setupForFlatItems();
            guiBufferSource = bufferSource instanceof BufferSource bs ? bs : Minecraft.getInstance().renderBuffers().bufferSource();
         }

         RenderType renderType = RenderType.entityCutoutNoCull(TEXTURE);
         VertexConsumer buffer = ItemRenderer.getFoilBufferDirect(bufferSource, renderType, gui, stack.hasFoil());
         MultiBufferSource target = (MultiBufferSource)(gui ? guiBufferSource : bufferSource);
         poseStack.pushPose();
         poseStack.translate(0.5F, 0.51F, 0.5F);
         AnimRenderer.render(model, poseStack, target, buffer, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F, this::onBone);
         poseStack.popPose();
         this.renderPendingArms(poseStack, target, packedLight, packedOverlay);
         if (gui) {
            guiBufferSource.endBatch();
            RenderSystem.enableDepthTest();
            Lighting.setupFor3DItems();
         }
      }
   }

   private void updateAnimation() {
      Player player = Minecraft.getInstance().player;
      if (player != null) {
         ItemStack live = player.getMainHandItem();
         if (live.getItem() instanceof InfinityGunItem) {
            CompoundTag nbt = live.getTag();
            int gunTimer = nbt == null ? 0 : nbt.getInt("GunTimer");
            int reloadTimer = nbt == null ? 0 : nbt.getInt("ReloadTimer");
            if (gunTimer > 0) {
               this.controller.play(anim("shoot"));
            } else if (reloadTimer > 0 && this.firstPerson) {
               this.controller.play(anim("reload"));
            } else {
               this.controller.play(anim("idle"));
            }
         }
      }
   }

   private static Animation anim(String name) {
      return AnimCache.animation(ANIMS, name);
   }

   private void onBone(GeoBone bone, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
      boolean right = bone.name.equals("right_arm");
      if (right || bone.name.equals("left_arm")) {
         bone.hidden = true;
         if (this.firstPerson) {
            InfinityGunRenderer.PendingArm slot = right ? this.rightArm : this.leftArm;
            slot.queued = true;
            slot.pose.set(poseStack.last().pose());
            slot.normal.set(poseStack.last().normal());
            slot.pivotX = bone.pivotX;
            slot.pivotY = bone.pivotY;
            slot.pivotZ = bone.pivotZ;
         }
      }
   }

   private void renderPendingArms(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
      if (this.rightArm.queued || this.leftArm.queued) {
         Minecraft mc = Minecraft.getInstance();
         AbstractClientPlayer player = mc.player;
         if (player != null) {
            ResourceLocation skin = player.getSkinTextureLocation();
            PlayerRenderer playerRenderer = (PlayerRenderer)mc.getEntityRenderDispatcher().getRenderer(player);
            PlayerModel<AbstractClientPlayer> playerModel = (PlayerModel<AbstractClientPlayer>)playerRenderer.getModel();
            VertexConsumer armBuffer = bufferSource.getBuffer(RenderType.entitySolid(skin));
            if (this.rightArm.queued) {
               this.drawArmPart(playerModel.rightArm, this.rightArm, true, poseStack, armBuffer, packedLight, packedOverlay);
            }

            if (this.leftArm.queued) {
               this.drawArmPart(playerModel.leftArm, this.leftArm, false, poseStack, armBuffer, packedLight, packedOverlay);
            }

            VertexConsumer sleeveBuffer = bufferSource.getBuffer(RenderType.entityTranslucent(skin));
            if (this.rightArm.queued) {
               this.drawArmPart(playerModel.rightSleeve, this.rightArm, true, poseStack, sleeveBuffer, packedLight, packedOverlay);
            }

            if (this.leftArm.queued) {
               this.drawArmPart(playerModel.leftSleeve, this.leftArm, false, poseStack, sleeveBuffer, packedLight, packedOverlay);
            }
         }
      }
   }

   private void drawArmPart(
      ModelPart part, InfinityGunRenderer.PendingArm slot, boolean right, PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay
   ) {
      poseStack.pushPose();
      poseStack.last().pose().set(slot.pose);
      poseStack.last().normal().set(slot.normal);
      poseStack.scale(0.67F, 0.8F, 0.67F);
      poseStack.translate(right ? 0.25 : -0.25, -0.1, 0.1625);
      part.setPos(slot.pivotX, slot.pivotY, slot.pivotZ);
      part.setRotation(0.0F, 0.0F, 0.0F);
      part.render(poseStack, buffer, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
      poseStack.popPose();
   }

   private static final class PendingArm {
      boolean queued;
      final Matrix4f pose = new Matrix4f();
      final Matrix3f normal = new Matrix3f();
      float pivotX;
      float pivotY;
      float pivotZ;
   }
}
