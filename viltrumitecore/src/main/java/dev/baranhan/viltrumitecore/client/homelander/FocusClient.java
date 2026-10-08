package dev.baranhan.viltrumitecore.client.homelander;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.baranhan.viltrumitecore.client.hero.ClientHeroData;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.joml.Matrix4f;

/**
 * Focus on the owner's client (spec §6.4): target ids come owner-only through
 * ClientHeroData; colours stick per target. Outlines use the vanilla glowing
 * path (HomelanderGlowMixin / HomelanderTeamColorMixin), plus HP labels,
 * a warm vignette and muffling of every sound that is not a target's.
 */
@EventBusSubscriber(
   modid = "viltrumitecore",
   bus = Bus.FORGE,
   value = {Dist.CLIENT}
)
public final class FocusClient {
   static final ResourceLocation VIGNETTE = new ResourceLocation("textures/misc/vignette.png");
   private static final double MUFFLE_KEEP_RADIUS = 2.5;
   private static final float MUFFLE_VOLUME = 0.6F;
   private static final FocusPalette PALETTE = new FocusPalette();
   private static final List<Integer> TARGETS = new ArrayList<>();
   private static float vignette;

   private FocusClient() {
   }

   /** True while the local player is Homelander with focus on. */
   public static boolean active() {
      Minecraft client = Minecraft.getInstance();
      HeroPublicSnapshot snapshot = HomelanderPoser.homelander(client.player);
      return snapshot != null && HomelanderPoser.focusOn(snapshot);
   }

   /** RGB colour of a focus target, -1 when the entity is not one. */
   public static int colorOf(Entity entity) {
      return PALETTE.isEmpty() ? -1 : PALETTE.colorOf(entity.getId());
   }

   public static List<Integer> targets() {
      return TARGETS;
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent event) {
      if (event.phase != Phase.END) {
         return;
      }

      TARGETS.clear();
      if (Minecraft.getInstance().level != null && active()) {
         for (int id : ClientHeroData.carriers()) {
            TARGETS.add(id);
         }
      }

      PALETTE.update(TARGETS);
      FocusFootsteps.tick(TARGETS);
   }

   /** HP label above each target, visible through walls. */
   @SubscribeEvent
   public static void onRenderLiving(RenderLivingEvent.Post<?, ?> event) {
      LivingEntity entity = event.getEntity();
      int color = colorOf(entity);
      if (color < 0) {
         return;
      }

      Minecraft client = Minecraft.getInstance();
      Font font = client.font;
      Component text = Component.literal("\u2764 " + Mth.ceil(entity.getHealth()) + "/" + Mth.ceil(entity.getMaxHealth()));
      PoseStack poseStack = event.getPoseStack();
      poseStack.pushPose();
      poseStack.translate(0.0F, entity.getBbHeight() + (entity.shouldShowName() ? 0.8F : 0.5F), 0.0F);
      poseStack.mulPose(client.getEntityRenderDispatcher().cameraOrientation());
      poseStack.scale(-0.025F, -0.025F, 0.025F);
      Matrix4f matrix = poseStack.last().pose();
      MultiBufferSource buffer = event.getMultiBufferSource();
      float x = -font.width(text) / 2.0F;
      int background = (int)(client.options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
      font.drawInBatch(text, x, 0.0F, 0x20000000 | color, false, matrix, buffer, Font.DisplayMode.SEE_THROUGH, background, LightTexture.FULL_BRIGHT);
      font.drawInBatch(text, x, 0.0F, 0xFF000000 | color, false, matrix, buffer, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
      poseStack.popPose();
   }

   /** Everything that does not come from a target is quieter while focusing. */
   @SubscribeEvent
   public static void onPlaySound(PlaySoundEvent event) {
      SoundInstance sound = event.getSound();
      if (sound == null || TARGETS.isEmpty() || sound.isRelative() || sound instanceof TickableSoundInstance
         || sound.getSource() == SoundSource.MASTER || sound.getSource() == SoundSource.MUSIC || sound.getSource() == SoundSource.RECORDS) {
         return;
      }

      Minecraft client = Minecraft.getInstance();
      if (client.level == null) {
         return;
      }

      Vec3 at = new Vec3(sound.getX(), sound.getY(), sound.getZ());
      for (int id : TARGETS) {
         Entity target = client.level.getEntity(id);
         if (target != null && target.position().distanceTo(at) <= MUFFLE_KEEP_RADIUS + target.getBbHeight()) {
            return;
         }
      }

      event.setSound(new MuffledSound(sound, MUFFLE_VOLUME));
   }

   @SubscribeEvent
   public static void onRenderGui(RenderGuiEvent.Post event) {
      float target = active() ? 1.0F : 0.0F;
      vignette = Mth.lerp(0.1F, vignette, target);
      if (vignette > 0.01F) {
         // Darkens green/blue more than red: a warm tint at the edges.
         drawVignette(event.getGuiGraphics(), 0.12F * vignette, 0.45F * vignette, 0.45F * vignette);
      }
   }

   /** Vanilla vignette texture with the vanilla darkening blend; rgb = how much each channel darkens. */
   static void drawVignette(GuiGraphics graphics, float r, float g, float b) {
      int width = graphics.guiWidth();
      int height = graphics.guiHeight();
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(SourceFactor.ZERO, DestFactor.ONE_MINUS_SRC_COLOR);
      graphics.setColor(r, g, b, 1.0F);
      graphics.blit(VIGNETTE, 0, 0, -90, 0.0F, 0.0F, width, height, width, height);
      graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
   }
}
