package dev.baranhan.viltrumitecore.client.ironman.scan;

import dev.baranhan.viltrumitecore.client.ironman.IronManView;
import dev.baranhan.viltrumitecore.client.ironman.jarvis.JarvisVoice;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import dev.baranhan.viltrumitecore.hero.ScanLine;
import dev.baranhan.viltrumitecore.hero.ironman.IronManRules;
import dev.baranhan.viltrumitecore.hero.ironman.scan.EffectLine;
import dev.baranhan.viltrumitecore.hero.ironman.scan.ScanCard;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

/**
 * Scan card (spec §11.2) at the right edge, above the ability panel: compact
 * rows — HP, armor / toughness, attack and speed, effects, resists, weak
 * spot, conditions. Everything comes from the server card; it fades after
 * 10 s ({@link IronManRules#SCAN_HIGHLIGHT_TICKS}). Shown with the helmet closed.
 */
@EventBusSubscriber(
   modid = "viltrumitecore",
   bus = Bus.FORGE,
   value = {Dist.CLIENT}
)
public final class ScanCardRenderer {
   private static final int WIDTH = 150;
   @Nullable
   private static ScanCard card;
   private static long shownAt;

   private ScanCardRenderer() {
   }

   public static void accept(ScanCard received) {
      Minecraft client = Minecraft.getInstance();
      card = received;
      shownAt = client.level == null ? 0L : client.level.getGameTime();
      JarvisVoice.onScanCard();
   }

   static String number(float value) {
      return value == Math.rint(value) ? Integer.toString(Math.round(value)) : String.format(Locale.ROOT, "%.1f", value);
   }

   static List<Component> rows(ScanCard card) {
      List<Component> rows = new ArrayList<>();
      rows.add(Component.translatable("scan.viltrumitecore.card.hp", number(card.hp()), number(card.maxHp())));
      rows.add(Component.translatable("scan.viltrumitecore.card.armor", number(card.armor()), number(card.toughness())));
      rows.add(Component.translatable("scan.viltrumitecore.card.stats", number(card.attackDamage()), String.format(Locale.ROOT, "%.2f", card.moveSpeed())));
      if (!card.effects().isEmpty()) {
         rows.add(Component.translatable("scan.viltrumitecore.card.effects"));
         for (EffectLine effect : card.effects()) {
            String time = effect.duration() < 0 ? "∞" : (effect.duration() / 20) + "s";
            Component name = Component.translatable(effect.descriptionId());
            Component level = effect.amplifier() > 0 ? Component.translatable("enchantment.level." + (effect.amplifier() + 1)) : Component.empty();
            rows.add(Component.literal(effect.beneficial() ? " + " : " - ").append(name).append(" ").append(level).append(" " + time)
               .withStyle(style -> style.withColor(effect.beneficial() ? 0x80FF90 : 0xFF8070)));
         }
      }

      rows.add(Component.translatable("scan.viltrumitecore.card.resists"));
      if (card.resists().isEmpty()) {
         rows.add(Component.literal(" ").append(Component.translatable("scan.viltrumitecore.resist.none")).withStyle(style -> style.withColor(0x8090A0)));
      }

      for (ScanLine line : card.resists()) {
         rows.add(Component.literal(" ").append(line.text()));
      }

      rows.add(Component.translatable("scan.viltrumitecore.card.weak"));
      for (ScanLine line : card.weakSpots()) {
         rows.add(Component.literal(" ").append(line.text()).withStyle(style -> style.withColor(0xFFD060)));
      }

      if (!card.conditions().isEmpty()) {
         rows.add(Component.translatable("scan.viltrumitecore.card.conditions"));
         for (ScanLine line : card.conditions()) {
            rows.add(Component.literal(" ").append(line.text()));
         }
      }

      return rows;
   }

   @SubscribeEvent
   public static void onRenderGui(RenderGuiEvent.Post event) {
      Minecraft client = Minecraft.getInstance();
      LocalPlayer player = client.player;
      ScanCard current = card;
      if (current == null || client.options.hideGui || player == null || client.level == null) {
         return;
      }

      long age = client.level.getGameTime() - shownAt;
      if (age < 0 || age > IronManRules.SCAN_HIGHLIGHT_TICKS) {
         card = null;
         return;
      }

      HeroPublicSnapshot snapshot = IronManView.of(player);
      if (!JarvisVoice.online(snapshot)) {
         return;
      }

      GuiGraphics graphics = event.getGuiGraphics();
      Font font = client.font;
      float fadeIn = Math.min(1.0F, (age + event.getPartialTick()) / 6.0F);
      float fadeOut = Math.min(1.0F, (IronManRules.SCAN_HIGHLIGHT_TICKS - age) / 20.0F);
      float alpha = Math.max(0.05F, Math.min(fadeIn, fadeOut));
      int a = Math.round(alpha * 255.0F);
      int width = graphics.guiWidth();
      int x = width - WIDTH - 6;
      int y = 28;
      // Above the ability panel (bottom right, ~150 px high).
      int maxBottom = graphics.guiHeight() - 160;
      List<Component> rows = rows(current);
      int lineHeight = 9;
      int titleHeight = 12;
      int maxRows = Math.max(1, (maxBottom - y - titleHeight - 6) / lineHeight);
      int shown = Math.min(rows.size(), maxRows);
      int bottom = y + titleHeight + shown * lineHeight + 4;
      graphics.fill(x - 3, y - 3, x + WIDTH + 3, bottom, (Math.round(alpha * 0xA0) << 24) | 0x041018);
      graphics.fill(x - 3, y - 3, x + WIDTH + 3, y - 2, (a << 24) | 0x60E0FF);
      graphics.fill(x - 3, y - 3, x - 2, bottom, (a << 24) | 0x60E0FF);
      Component title = Component.translatable("scan.viltrumitecore.card.title").append(" ").append(current.name());
      graphics.drawString(font, font.split(title, WIDTH).get(0), x, y, (a << 24) | 0xBFF4FF, false);
      int ry = y + titleHeight;
      for (int i = 0; i < shown; i++) {
         List<FormattedCharSequence> split = font.split(rows.get(i), WIDTH);
         if (!split.isEmpty()) {
            graphics.drawString(font, split.get(0), x, ry, (a << 24) | 0xD8F0FF, false);
         }

         ry += lineHeight;
      }

      // HP bar under the title.
      float hp = current.maxHp() <= 0.0F ? 0.0F : Math.max(0.0F, Math.min(1.0F, current.hp() / current.maxHp()));
      graphics.fill(x, y + 9, x + WIDTH, y + 10, (Math.round(alpha * 0x60) << 24) | 0x601010);
      graphics.fill(x, y + 9, x + Math.round(WIDTH * hp), y + 10, (a << 24) | 0xFF5040);
   }
}
