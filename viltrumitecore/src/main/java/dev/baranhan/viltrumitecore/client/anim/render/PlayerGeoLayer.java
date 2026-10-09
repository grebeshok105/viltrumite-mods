package dev.baranhan.viltrumitecore.client.anim.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.baranhan.viltrumitecore.client.anim.AnimCache;
import dev.baranhan.viltrumitecore.client.anim.geo.BakedGeoModel;
import dev.baranhan.viltrumitecore.client.anim.geo.GeoBone;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.Nullable;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Anim-core extension (animation-system §7, approved "3D parts on bones"):
 * Blockbench geo parts attached to the vanilla player model parts. Each top
 * bone named like a player-armor bone ({@link PlayerBoneMap}) copies the pose
 * the model has after every setupAnim TAIL mixin, so parts follow all poses.
 * Base pass = entity cutout, glow pass = {@code RenderType.eyes} (vanilla
 * render types only — shader packs keep them).
 */
public class PlayerGeoLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
   private static final Logger LOGGER = LoggerFactory.getLogger("viltrumitecore/anim");
   private static final List<Provider> PROVIDERS = new CopyOnWriteArrayList<>();
   private static final Set<String> WARNED = new HashSet<>();

   /** One geo part: model, base texture (null = glow only), optional glow, tint/alpha. */
   public record Part(ResourceLocation model, @Nullable ResourceLocation texture, @Nullable ResourceLocation glow, float r, float g, float b, float a) {
      public Part(ResourceLocation model, @Nullable ResourceLocation texture, @Nullable ResourceLocation glow) {
         this(model, texture, glow, 1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   @FunctionalInterface
   public interface Provider {
      /** Parts for this player this frame (empty when none). Render thread only. */
      List<Part> parts(AbstractClientPlayer player, float partialTick);
   }

   public PlayerGeoLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
      super(parent);
   }

   public static void register(Provider provider) {
      PROVIDERS.add(provider);
   }

   @Override
   public void render(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
      float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
      if (player.isInvisible() || PROVIDERS.isEmpty()) {
         return;
      }

      List<Part> parts = new ArrayList<>();
      for (Provider provider : PROVIDERS) {
         parts.addAll(provider.parts(player, partialTick));
      }

      for (Part part : parts) {
         BakedGeoModel model = AnimCache.model(part.model());
         if (model == null) {
            continue;
         }

         pose(model, this.getParentModel());
         poseStack.pushPose();
         // Undo the player renderer's model flip: geo render space, y up from the feet.
         poseStack.translate(0.0F, 1.501F, 0.0F);
         poseStack.scale(-1.0F, -1.0F, 1.0F);
         if (part.texture() != null) {
            AnimRenderer.render(model, poseStack, buffers, buffers.getBuffer(RenderType.entityCutoutNoCull(part.texture())), light, OverlayTexture.NO_OVERLAY,
               part.r(), part.g(), part.b(), part.a(), null);
         }

         if (part.glow() != null) {
            AnimRenderer.render(model, poseStack, buffers, buffers.getBuffer(RenderType.eyes(part.glow())), 15728640, OverlayTexture.NO_OVERLAY,
               part.r(), part.g(), part.b(), part.a(), null);
         }

         poseStack.popPose();
      }
   }

   /** Copy the model part poses into the mapped top bones; unknown top bones are hidden. */
   static void pose(BakedGeoModel model, PlayerModel<?> player) {
      model.resetBones();
      for (GeoBone bone : model.topLevelBones()) {
         PlayerBoneMap.Part mapped = PlayerBoneMap.of(bone.name);
         if (mapped == null) {
            bone.hidden = true;
            if (WARNED.add(bone.name)) {
               LOGGER.warn("Geo part bone '{}' has no player part; skipped", bone.name);
            }
            continue;
         }

         ModelPart part = switch (mapped) {
            case HEAD -> player.head;
            case BODY -> player.body;
            case RIGHT_ARM -> player.rightArm;
            case LEFT_ARM -> player.leftArm;
            case RIGHT_LEG -> player.rightLeg;
            case LEFT_LEG -> player.leftLeg;
         };
         float[] pos = PlayerBoneMap.position(mapped, part.x, part.y, part.z);
         float[] rot = PlayerBoneMap.rotation(part.xRot, part.yRot, part.zRot);
         bone.posX = pos[0];
         bone.posY = pos[1];
         bone.posZ = pos[2];
         bone.rotX = bone.initRotX + rot[0];
         bone.rotY = bone.initRotY + rot[1];
         bone.rotZ = bone.initRotZ + rot[2];
         bone.hidden = !part.visible;
      }
   }
}
