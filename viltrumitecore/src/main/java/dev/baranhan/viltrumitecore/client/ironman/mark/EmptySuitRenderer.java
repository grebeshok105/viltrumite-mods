package dev.baranhan.viltrumitecore.client.ironman.mark;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.baranhan.viltrumitecore.client.anim.AnimCache;
import dev.baranhan.viltrumitecore.client.anim.animation.Animation;
import dev.baranhan.viltrumitecore.client.anim.animation.AnimationController;
import dev.baranhan.viltrumitecore.client.anim.geo.BakedGeoModel;
import dev.baranhan.viltrumitecore.client.anim.geo.GeoBone;
import dev.baranhan.viltrumitecore.client.ironman.IronManView;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import dev.baranhan.viltrumitecore.client.anim.render.AnimRenderer;
import dev.baranhan.viltrumitecore.entity.EmptySuitEntity;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkId;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The empty suit (spec §12.5–§12.6): the Satsu full_body shell with the helmet
 * box in the mark skin, cut into a back half and hinged front plates
 * (tools/assets/opening_shell.py), with the interior lining (and its lights)
 * inside every half. "open" (30 ticks) swings the chest doors, arm and leg
 * plates and the faceplate open while Tony walks out, then closes them.
 * Entering is code-driven ({@link EmptySuitEntity#enterDoor}): the plates
 * open while Tony walks up, stay open while he turns and steps back in, then
 * close group by group (legs, arms, chest, faceplate) with no overshoot, so
 * no plate passes through another. The lining is tinted to the mark
 * ({@link SuitPalette#lining}). The mark's own Satsu parts
 * ({@link MarkExtras}) show only while the suit stands closed. Geo in the body
 * frame, drawn at player scale.
 */
public final class EmptySuitRenderer extends EntityRenderer<EmptySuitEntity> {
   private static final ResourceLocation SHELL = new ResourceLocation("viltrumitecore", "geo/ironman/marks/empty_suit.geo.json");
   private static final ResourceLocation INNER = new ResourceLocation("viltrumitecore", "geo/ironman/marks/empty_suit_interior.geo.json");
   private static final ResourceLocation INNER_GLOW = new ResourceLocation("viltrumitecore", "textures/entity/hero/ironman_interior_glow.png");
   private static final ResourceLocation ANIMS = new ResourceLocation("viltrumitecore", "animations/ironman/empty_suit.animation.json");
   private static final float BODY_SCALE = 0.9375F;
   private static final WeakHashMap<EmptySuitEntity, Drive> DRIVES = new WeakHashMap<>();

   public EmptySuitRenderer(Context context) {
      super(context);
   }

   @Override
   public void render(EmptySuitEntity suit, float entityYaw, float partialTick, PoseStack stack, MultiBufferSource buffers, int light) {
      BakedGeoModel shell = AnimCache.model(SHELL);
      BakedGeoModel inner = AnimCache.model(INNER);
      if (shell == null || inner == null) {
         return;
      }

      MarkId mark = suit.mark() == null ? MarkId.MARK_7 : suit.mark();
      ResourceLocation extrasGeo = MarkExtras.geo(mark);
      BakedGeoModel extras = extrasGeo == null ? null : AnimCache.model(extrasGeo);
      Drive drive = DRIVES.computeIfAbsent(suit, s -> new Drive());
      drive.update(suit.phase());
      boolean entering = suit.phase() == EmptySuitEntity.Phase.ENTERING;
      if (entering && ownerSuited(suit)) {
         // The mark is on the owner already (the entity goes this tick): never draw it twice.
         return;
      }

      boolean closed = suit.phase() == EmptySuitEntity.Phase.STANDING || suit.phase() == EmptySuitEntity.Phase.LEAVING;
      drive.apply(shell, inner, closed ? extras : null, partialTick);
      if (entering) {
         float t = suit.clientPhaseAge() + partialTick;
         doors(shell, t);
         doors(inner, t);
         // First person: the helmet box would sit on the camera; the HUD frame takes over.
         Minecraft client = Minecraft.getInstance();
         boolean ownView = client.player != null && suit.ownerId().map(client.player.getUUID()::equals).orElse(false)
            && client.options.getCameraType().isFirstPerson() && t >= EmptySuitEntity.ENTER_TURN;
         hideHead(shell, ownView);
         hideHead(inner, ownView);
      } else {
         hideHead(shell, false);
         hideHead(inner, false);
      }

      float lr = SuitPalette.r(SuitPalette.lining(mark));
      float lg = SuitPalette.g(SuitPalette.lining(mark));
      float lb = SuitPalette.b(SuitPalette.lining(mark));
      float glowPulse = 0.85F + 0.15F * Mth.sin((suit.tickCount + partialTick) * 0.15F);
      stack.pushPose();
      stack.mulPose(Axis.YP.rotationDegrees(180.0F - suit.getYRot()));
      stack.scale(BODY_SCALE, BODY_SCALE, BODY_SCALE);
      AnimRenderer.render(shell, stack, null, buffers.getBuffer(RenderType.entityCutoutNoCull(MarkTextures.skin(mark))), light, OverlayTexture.NO_OVERLAY,
         1.0F, 1.0F, 1.0F, 1.0F, null);
      AnimRenderer.render(inner, stack, null, buffers.getBuffer(RenderType.entityCutoutNoCull(MarkTextures.INTERIOR)), light, OverlayTexture.NO_OVERLAY,
         lr, lg, lb, 1.0F, null);
      AnimRenderer.render(shell, stack, null, buffers.getBuffer(RenderType.eyes(MarkTextures.glow(mark))), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
         glowPulse, glowPulse, glowPulse, 1.0F, null);
      if (!closed) {
         AnimRenderer.render(inner, stack, null, buffers.getBuffer(RenderType.eyes(INNER_GLOW)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
            1.0F, 1.0F, 1.0F, 1.0F, null);
      } else if (extras != null) {
         MarkParts.draw(extras, stack, buffers, MarkExtras.passes(mark), light, 1.0F);
      }

      stack.popPose();
      super.render(suit, entityYaw, partialTick, stack, buffers, light);
   }

   /** Plate angles of the walk-in (degrees as in the "open" clip; clip X/Y map to -rad). */
   private static void doors(BakedGeoModel model, float t) {
      door(model, "left_leg_door", 0.0F, -95.0F, EmptySuitEntity.enterDoor(EmptySuitEntity.GROUP_LEGS, t));
      door(model, "right_leg_door", 0.0F, 95.0F, EmptySuitEntity.enterDoor(EmptySuitEntity.GROUP_LEGS, t));
      door(model, "left_arm_door", 0.0F, -100.0F, EmptySuitEntity.enterDoor(EmptySuitEntity.GROUP_ARMS, t));
      door(model, "right_arm_door", 0.0F, 100.0F, EmptySuitEntity.enterDoor(EmptySuitEntity.GROUP_ARMS, t));
      door(model, "body_door_left", 0.0F, -110.0F, EmptySuitEntity.enterDoor(EmptySuitEntity.GROUP_CHEST, t));
      door(model, "body_door_right", 0.0F, 110.0F, EmptySuitEntity.enterDoor(EmptySuitEntity.GROUP_CHEST, t));
      door(model, "faceplate", -105.0F, 0.0F, EmptySuitEntity.enterDoor(EmptySuitEntity.GROUP_FACE, t));
   }

   private static void door(BakedGeoModel model, String name, float x, float y, float open) {
      GeoBone bone = model.getBone(name);
      if (bone != null) {
         bone.rotX = bone.initRotX - (float)Math.toRadians(x * open);
         bone.rotY = bone.initRotY - (float)Math.toRadians(y * open);
         bone.rotZ = bone.initRotZ;
      }
   }

   private static void hideHead(BakedGeoModel model, boolean hidden) {
      GeoBone head = model.getBone("armorhead");
      if (head != null) {
         head.hidden = hidden;
      }
   }

   private static boolean ownerSuited(EmptySuitEntity suit) {
      Minecraft client = Minecraft.getInstance();
      if (client.level == null || suit.ownerId().isEmpty()) {
         return false;
      }

      Player owner = client.level.getPlayerByUUID(suit.ownerId().get());
      HeroPublicSnapshot snapshot = owner == null ? null : IronManView.of(owner);
      return snapshot != null && MarkState.of(snapshot).markOn();
   }

   @Override
   public ResourceLocation getTextureLocation(EmptySuitEntity suit) {
      return MarkTextures.skin(suit.mark() == null ? MarkId.MARK_7 : suit.mark());
   }

   /** Plays the open clip on both models from the entity's phase start. */
   private static final class Drive {
      private final AnimationController shell = new AnimationController("empty_suit_shell");
      private final AnimationController inner = new AnimationController("empty_suit_inner");
      private EmptySuitEntity.Phase last;

      void update(EmptySuitEntity.Phase phase) {
         if (phase == this.last) {
            return;
         }

         this.last = phase;
         if (phase == EmptySuitEntity.Phase.OPENING) {
            Animation open = AnimCache.animation(ANIMS, "open");
            this.shell.restart(open);
            this.inner.restart(open);
         } else {
            this.shell.stop();
            this.inner.stop();
         }
      }

      void apply(BakedGeoModel shell, BakedGeoModel inner, BakedGeoModel extras, float partialTick) {
         double time = AnimRenderer.time(partialTick);
         this.shell.apply(shell, time);
         this.inner.apply(inner, time);
         if (extras != null) {
            extras.resetBones();
         }
      }
   }
}
