package dev.baranhan.viltrumitecore.client.gui;

import com.mojang.authlib.GameProfile;
import dev.baranhan.viltrumitecore.entity.ViltrumiteFakePlayer;
import dev.baranhan.viltrumitecore.network.CoreMessages;
import dev.baranhan.viltrumitecore.network.packet.SpawnNpcC2SPacket;
import dev.baranhan.viltrumitecore.util.ViltrumiteCosmeticsPlayer;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class NpcSpawnerScreen extends Screen {
   private EditBox nameField;
   private EditBox skinField;
   private EditBox capeField;
   private EditBox modelField;
   private EditBox scaleField;
   private EditBox damageField;
   private EditBox damageIgnoreField;
   private EditBox healFactorField;
   private EditBox damageReductionField;
   private EditBox flySpeedField;
   private EditBox throttleField;
   private int intelligence = 1;
   private ViltrumiteFakePlayer.TargetMode targetMode = ViltrumiteFakePlayer.TargetMode.PLAYER_AGGRESSIVE;
   private NpcSpawnerScreen.PreviewPlayer previewPlayer;

   public NpcSpawnerScreen() {
      super(Component.translatable("gui.viltrumitecore.npc_spawner.title"));
   }

   protected void init() {
      super.init();
      int cX = this.width / 2;
      if (this.minecraft != null && this.minecraft.level != null) {
         this.previewPlayer = new NpcSpawnerScreen.PreviewPlayer(this.minecraft.level, new GameProfile(UUID.randomUUID(), "NPC"));
      }

      this.nameField = new EditBox(this.font, cX - 60, 30, 120, 20, Component.empty());
      this.nameField.setValue("John Viltrumite");
      this.addRenderableWidget(this.nameField);
      Button zekaBtn = Button.builder(Component.literal(String.valueOf(this.intelligence)), btn -> {
         this.intelligence++;
         if (this.intelligence > 3) {
            this.intelligence = 1;
         }

         btn.setMessage(Component.literal(String.valueOf(this.intelligence)));
      }).bounds(cX - 110, 90, 60, 20).build();
      this.addRenderableWidget(zekaBtn);
      Button hedefBtn = Button.builder(Component.translatable("gui.viltrumitecore.target_mode." + this.targetMode.name().toLowerCase()), btn -> {
         int next = (this.targetMode.ordinal() + 1) % ViltrumiteFakePlayer.TargetMode.values().length;
         this.targetMode = ViltrumiteFakePlayer.TargetMode.values()[next];
         btn.setMessage(Component.translatable("gui.viltrumitecore.target_mode." + this.targetMode.name().toLowerCase()));
      }).bounds(cX + 50, 90, 120, 20).build();
      this.addRenderableWidget(hedefBtn);
      int r1X = cX - 135;
      this.skinField = new EditBox(this.font, r1X, 160, 60, 20, Component.empty());
      this.skinField.setValue("skin_viltrumite_1");
      this.skinField.setResponder(this::updatePreviewCosmetics);
      this.addRenderableWidget(this.skinField);
      this.capeField = new EditBox(this.font, r1X + 70, 160, 60, 20, Component.empty());
      this.capeField.setValue("cape_1");
      this.capeField.setResponder(this::updatePreviewCosmetics);
      this.addRenderableWidget(this.capeField);
      this.modelField = new EditBox(this.font, r1X + 140, 160, 60, 20, Component.empty());
      this.modelField.setValue("default");
      this.modelField.setResponder(this::updatePreviewCosmetics);
      this.addRenderableWidget(this.modelField);
      this.scaleField = new EditBox(this.font, r1X + 210, 160, 60, 20, Component.empty());
      this.scaleField.setValue("1.0");
      this.scaleField.setFilter(s -> s.isEmpty() || s.matches("-?\\d*\\.?\\d*"));
      this.addRenderableWidget(this.scaleField);
      this.damageField = new EditBox(this.font, r1X, 210, 60, 20, Component.empty());
      this.damageField.setValue("20.0");
      this.damageField.setFilter(s -> s.isEmpty() || s.matches("-?\\d*\\.?\\d*"));
      this.addRenderableWidget(this.damageField);
      this.damageIgnoreField = new EditBox(this.font, r1X + 70, 210, 60, 20, Component.empty());
      this.damageIgnoreField.setValue("0.5");
      this.damageIgnoreField.setFilter(s -> s.isEmpty() || s.matches("-?\\d*\\.?\\d*"));
      this.addRenderableWidget(this.damageIgnoreField);
      this.healFactorField = new EditBox(this.font, r1X + 140, 210, 60, 20, Component.empty());
      this.healFactorField.setValue("1.0");
      this.healFactorField.setFilter(s -> s.isEmpty() || s.matches("-?\\d*\\.?\\d*"));
      this.addRenderableWidget(this.healFactorField);
      this.damageReductionField = new EditBox(this.font, r1X + 210, 210, 60, 20, Component.empty());
      this.damageReductionField.setValue("97.0");
      this.damageReductionField.setFilter(s -> s.isEmpty() || s.matches("-?\\d*\\.?\\d*"));
      this.addRenderableWidget(this.damageReductionField);
      int r3X = cX - 65;
      this.flySpeedField = new EditBox(this.font, r3X, 260, 60, 20, Component.empty());
      this.flySpeedField.setValue("10.0");
      this.flySpeedField.setFilter(s -> s.isEmpty() || s.matches("-?\\d*\\.?\\d*"));
      this.addRenderableWidget(this.flySpeedField);
      this.throttleField = new EditBox(this.font, r3X + 70, 260, 60, 20, Component.empty());
      this.throttleField.setValue("0.017");
      this.throttleField.setFilter(s -> s.isEmpty() || s.matches("-?\\d*\\.?\\d*"));
      this.addRenderableWidget(this.throttleField);
      Button confirmBtn = Button.builder(
            Component.translatable("gui.viltrumitecore.npc_spawner.spawn"),
            btn -> {
               float finalScale = this.getClampedFloat(this.scaleField, 0.8F, 1.2F, 1.0F);
               float finalDamage = this.getClampedFloat(this.damageField, 1.0F, 999.0F, 20.0F);
               float finalDamageIgnore = this.getClampedFloat(this.damageIgnoreField, 0.0F, 500.0F, 0.5F);
               float finalHeal = this.getClampedFloat(this.healFactorField, 0.0F, 50.0F, 1.0F);
               float finalReduction = this.getClampedFloat(this.damageReductionField, 0.0F, 100.0F, 97.0F);
               float finalFly = this.getClampedFloat(this.flySpeedField, 0.5F, 500.0F, 10.0F);
               float finalThrottle = this.getClampedFloat(this.throttleField, 0.001F, 1.0F, 0.017F);
               FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
               buf.writeUtf(this.nameField.getValue());
               buf.writeUtf(this.skinField.getValue());
               buf.writeUtf(this.capeField.getValue());
               buf.writeUtf(this.modelField.getValue());
               buf.writeInt(this.intelligence);
               buf.writeInt(this.targetMode.ordinal());
               buf.writeFloat(finalScale);
               buf.writeFloat(finalDamage);
               buf.writeFloat(finalDamageIgnore);
               buf.writeFloat(finalHeal);
               buf.writeFloat(finalReduction);
               buf.writeFloat(finalFly);
               buf.writeFloat(finalThrottle);
               CoreMessages.sendToServer(
                  new SpawnNpcC2SPacket(
                     this.nameField.getValue(),
                     this.skinField.getValue(),
                     this.capeField.getValue(),
                     this.modelField.getValue(),
                     this.intelligence,
                     this.targetMode.ordinal(),
                     finalScale,
                     finalDamage,
                     finalDamageIgnore,
                     finalHeal,
                     finalReduction,
                     finalFly,
                     finalThrottle
                  )
               );
               this.minecraft.setScreen(null);
            }
         )
         .bounds(cX - 50, 300, 100, 20)
         .build();
      this.addRenderableWidget(confirmBtn);
      this.updatePreviewCosmetics("");
   }

   private float getClampedFloat(EditBox field, float min, float max, float def) {
      try {
         if (field.getValue().isEmpty()) {
            return def;
         } else {
            float val = Float.parseFloat(field.getValue());
            return Math.max(min, Math.min(max, val));
         }
      } catch (NumberFormatException var6) {
         return def;
      }
   }

   private void updatePreviewCosmetics(String value) {
      if (this.previewPlayer != null) {
         ViltrumiteCosmeticsPlayer cosmetics = (ViltrumiteCosmeticsPlayer)this.previewPlayer;
         cosmetics.setViltrumiteSkin(this.skinField.getValue());
         cosmetics.setViltrumiteCape(this.capeField.getValue());
         cosmetics.setViltrumiteModel(this.modelField.getValue());
      }
   }

   public void tick() {
      super.tick();
      if (this.previewPlayer != null) {
         this.previewPlayer.tick();
      }
   }

   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
      guiGraphics.fill(0, 0, this.width, this.height, -1073741824);
      int cX = this.width / 2;
      guiGraphics.fill(cX - 40, 60, cX + 40, 140, -16777216);
      guiGraphics.renderOutline(cX - 41, 59, 82, 82, -5592406);
      if (this.previewPlayer != null) {
         InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, cX, 135, 35, (float)(cX - mouseX), (float)(100 - mouseY), this.previewPlayer);
      }

      int yellowishWhite = 16777130;
      this.drawCenteredLabel(guiGraphics, Component.translatable("gui.viltrumitecore.npc_spawner.name").getString(), this.nameField, yellowishWhite);
      guiGraphics.drawCenteredString(this.font, Component.translatable("gui.viltrumitecore.npc_spawner.intelligence"), cX - 80, 78, yellowishWhite);
      guiGraphics.drawCenteredString(this.font, Component.translatable("gui.viltrumitecore.npc_spawner.target_mode"), cX + 110, 78, yellowishWhite);
      this.drawCenteredLabel(guiGraphics, Component.translatable("gui.viltrumitecore.npc_spawner.skin").getString(), this.skinField, yellowishWhite);
      this.drawCenteredLabel(guiGraphics, Component.translatable("gui.viltrumitecore.npc_spawner.cape").getString(), this.capeField, yellowishWhite);
      this.drawCenteredLabel(guiGraphics, Component.translatable("gui.viltrumitecore.npc_spawner.model").getString(), this.modelField, yellowishWhite);
      this.drawCenteredLabel(guiGraphics, Component.translatable("gui.viltrumitecore.npc_spawner.scale").getString(), this.scaleField, yellowishWhite);
      this.drawCenteredLabel(guiGraphics, Component.translatable("gui.viltrumitecore.npc_spawner.base_damage").getString(), this.damageField, yellowishWhite);
      this.drawCenteredLabel(
         guiGraphics, Component.translatable("gui.viltrumitecore.npc_spawner.damage_ignore").getString(), this.damageIgnoreField, yellowishWhite
      );
      this.drawCenteredLabel(guiGraphics, Component.translatable("gui.viltrumitecore.npc_spawner.heal_factor").getString(), this.healFactorField, yellowishWhite);
      this.drawCenteredLabel(
         guiGraphics, Component.translatable("gui.viltrumitecore.npc_spawner.damage_reduction").getString(), this.damageReductionField, yellowishWhite
      );
      this.drawCenteredLabel(guiGraphics, Component.translatable("gui.viltrumitecore.npc_spawner.max_flight").getString(), this.flySpeedField, yellowishWhite);
      this.drawCenteredLabel(guiGraphics, Component.translatable("gui.viltrumitecore.npc_spawner.throttle").getString(), this.throttleField, yellowishWhite);
      super.render(guiGraphics, mouseX, mouseY, delta);
   }

   private void drawCenteredLabel(GuiGraphics guiGraphics, String text, EditBox field, int color) {
      String[] lines = text.split("\n");
      int yBase = field.getY() - lines.length * 10;

      for (int i = 0; i < lines.length; i++) {
         guiGraphics.drawCenteredString(this.font, lines[i], field.getX() + field.getWidth() / 2, yBase + i * 10, color);
      }
   }

   private class PreviewPlayer extends RemotePlayer {
      public PreviewPlayer(ClientLevel world, GameProfile profile) {
         super(world, profile);
         this.getEntityData().set(Player.DATA_PLAYER_MODE_CUSTOMISATION, (byte)127);
      }

      public boolean isSkinLoaded() {
         return true;
      }

      public boolean isCapeLoaded() {
         return true;
      }
   }
}
