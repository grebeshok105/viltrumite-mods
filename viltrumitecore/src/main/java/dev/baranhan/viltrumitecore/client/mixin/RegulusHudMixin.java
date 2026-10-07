package dev.baranhan.viltrumitecore.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.baranhan.viltrumitecore.client.regulus.RegulusVfxMath;
import dev.baranhan.viltrumitecore.hero.HeroId;
import dev.baranhan.viltrumitecore.hero.HeroPlayer;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Regulus HUD elements (spec 15), drawn in the same ForgeGui.render TAIL slot
 * as ViltrumiteInGameHudMixin: heart counter, Lion window bar with a pulsing
 * red overheat fill, madness timer, and the sprite-based blood overlay that
 * pulses with the heartbeat.
 */
@Mixin({ForgeGui.class})
public class RegulusHudMixin {
   @Unique
   private static final ResourceLocation HEART_TEXTURE = new ResourceLocation("viltrumitecore", "textures/gui/hero/regulus_heart.png");
   @Unique
   private static final ResourceLocation LION_BAR_TEXTURE = new ResourceLocation("viltrumitecore", "textures/gui/hero/regulus_lion_bar.png");
   @Unique
   private static final ResourceLocation DROP_TEXTURE = new ResourceLocation("viltrumitecore", "textures/gui/hero/regulus_drop.png");
   @Unique
   private static final ResourceLocation BLOOD_0 = new ResourceLocation("viltrumitecore", "textures/gui/hero/regulus_blood_0.png");
   @Unique
   private static final ResourceLocation BLOOD_1 = new ResourceLocation("viltrumitecore", "textures/gui/hero/regulus_blood_1.png");

   @Inject(
      method = {"render"},
      at = {@At("TAIL")}
   )
   private void renderRegulusHud(GuiGraphics guiGraphics, float partialTick, CallbackInfo ci) {
      Minecraft client = Minecraft.getInstance();
      LocalPlayer player = client.player;
      if (player == null || client.options.hideGui) {
         return;
      }

      if (!(player instanceof HeroPlayer heroPlayer)) {
         return;
      }

      HeroPublicSnapshot snapshot = heroPlayer.getHeroSnapshot();
      if (snapshot == null || snapshot.heroId() != HeroId.REGULUS) {
         return;
      }

      int width = client.getWindow().getGuiScaledWidth();
      int height = client.getWindow().getGuiScaledHeight();
      int baseX = 8;
      int baseY = height - 22;

      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();

      this.renderHearts(guiGraphics, client, snapshot, baseX, baseY);
      this.renderLionBar(guiGraphics, snapshot, baseX, baseY - 14, partialTick, client);
      this.renderMadnessTimer(guiGraphics, client, snapshot, baseX, baseY - 26);
      this.renderBloodOverlay(guiGraphics, snapshot, width, height, partialTick, client);
      this.renderJumpCharge(guiGraphics, width, height);

      RenderSystem.disableBlend();
   }

   @Unique
   private void renderHearts(GuiGraphics guiGraphics, Minecraft client, HeroPublicSnapshot snapshot, int x, int y) {
      guiGraphics.blit(HEART_TEXTURE, x, y, 0.0F, 0.0F, 9, 9, 9, 9);
      String count = "x" + snapshot.hearts();
      guiGraphics.drawString(client.font, count, x + 12, y + 1, 16769280, true);
   }

   @Unique
   private void renderLionBar(GuiGraphics guiGraphics, HeroPublicSnapshot snapshot, int x, int y, float partialTick, Minecraft client) {
      if (!snapshot.lionActive() || snapshot.lionWindowMax() <= 0) {
         return;
      }

      guiGraphics.blit(LION_BAR_TEXTURE, x, y, 0.0F, 0.0F, 64, 9, 64, 9);
      float fraction = Mth.clamp((float)snapshot.lionWindowLeft() / (float)snapshot.lionWindowMax(), 0.0F, 1.0F);
      int fill = (int)(fraction * 60.0F);
      if (fill <= 0) {
         return;
      }

      int color;
      if (snapshot.lionOverheat()) {
         // Overheating: the fill pulses red (spec 15).
         float timeSeconds = ((float)(client.level == null ? 0L : client.level.getGameTime() % 24000L) + partialTick) / 20.0F;
         float pulse = 0.5F + 0.5F * (float)Math.sin((double)(timeSeconds * Math.PI * 4.0));
         int r = 200 + (int)(55.0F * pulse);
         int g = (int)(60.0F * pulse);
         color = 0xFF000000 | (r << 16) | (g << 8) | 30;
      } else {
         color = 0xFFFFD25C;
      }

      guiGraphics.fill(x + 2, y + 2, x + 2 + fill, y + 7, color);
   }

   @Unique
   private void renderMadnessTimer(GuiGraphics guiGraphics, Minecraft client, HeroPublicSnapshot snapshot, int x, int y) {
      if (!snapshot.madness() || snapshot.madnessTicksLeft() <= 0) {
         return;
      }

      guiGraphics.blit(DROP_TEXTURE, x, y, 0.0F, 0.0F, 9, 9, 9, 9);
      String seconds = String.valueOf((snapshot.madnessTicksLeft() + 19) / 20) + "s";
      guiGraphics.drawString(client.font, seconds, x + 12, y + 1, 14687580, true);
   }

   @Unique
   private void renderBloodOverlay(GuiGraphics guiGraphics, HeroPublicSnapshot snapshot, int width, int height, float partialTick, Minecraft client) {
      if (client.level == null) {
         return;
      }

      float beatPhase = ((float)(client.level.getGameTime() % RegulusVfxMath.HEARTBEAT_PERIOD_TICKS) + partialTick) / (float)RegulusVfxMath.HEARTBEAT_PERIOD_TICKS;
      dev.baranhan.viltrumitecore.client.regulus.RegulusBloodRain.render(guiGraphics, width, height, snapshot.madness(), RegulusVfxMath.madnessPulse(beatPhase));
   }

   @Unique
   private void renderJumpCharge(GuiGraphics guiGraphics, int width, int height) {
      float charge = dev.baranhan.viltrumitecore.client.regulus.RegulusJumpClient.chargeFraction();
      if (charge <= 0.0F) {
         return;
      }

      int barWidth = 40;
      int x = (width - barWidth) / 2;
      int y = height / 2 + 12;
      guiGraphics.fill(x - 1, y - 1, x + barWidth + 1, y + 3, 0x90000000);
      int fill = (int)(barWidth * charge);
      int color = charge >= 1.0F ? 0xFFFFF2B0 : 0xFFFFD25C;
      guiGraphics.fill(x, y, x + fill, y + 2, color);
   }
}
