package dev.baranhan.viltrumitecore.client;

import dev.baranhan.viltrumitecore.client.gui.ViltrumiteDashboardScreen;
import dev.baranhan.viltrumitecore.network.CoreMessages;
import dev.baranhan.viltrumitecore.network.packet.SwapAbilityBarC2SPacket;
import dev.baranhan.viltrumitecore.util.ViltrumiteAbilityUser;
import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;

public class AbilityInputManager {
   public static KeyMapping abilityKey1;
   public static KeyMapping abilityKey2;
   public static KeyMapping abilityKey3;
   public static KeyMapping abilityKey4;
   public static KeyMapping abilityKey5;
   public static KeyMapping abilityKey6;
   public static KeyMapping abilityMenuKey;
   public static KeyMapping dashboardKey;
   public static KeyMapping abilitySwapKey;
   /** Regulus: mark/unmark a heart carrier. Default MMB — wins over vanilla pick-block. */
   public static KeyMapping assignHeartKey;
   public static KeyMapping superJumpKey;

   public static void registerKeys(RegisterKeyMappingsEvent event) {
      abilityKey1 = new KeyMapping("key.viltrumitecore.ability_1", 82, "category.viltrumitecore.keys");
      abilityKey2 = new KeyMapping("key.viltrumitecore.ability_2", 89, "category.viltrumitecore.keys");
      abilityKey3 = new KeyMapping("key.viltrumitecore.ability_3", 90, "category.viltrumitecore.keys");
      abilityKey4 = new KeyMapping("key.viltrumitecore.ability_4", 86, "category.viltrumitecore.keys");
      abilityKey5 = new KeyMapping("key.viltrumitecore.ability_5", 66, "category.viltrumitecore.keys");
      abilityKey6 = new KeyMapping("key.viltrumitecore.ability_6", 78, "category.viltrumitecore.keys");
      abilityMenuKey = new KeyMapping("key.viltrumitecore.ability_menu", 75, "category.viltrumitecore.keys");
      dashboardKey = new KeyMapping("key.viltrumitecore.dashboard", 80, "category.viltrumitecore.keys");
      abilitySwapKey = new KeyMapping("key.viltrumitecore.ability_swap", 342, "category.viltrumitecore.keys");
      event.register(abilityKey1);
      event.register(abilityKey2);
      event.register(abilityKey3);
      event.register(abilityKey4);
      event.register(abilityKey5);
      event.register(abilityKey6);
      event.register(abilityMenuKey);
      event.register(dashboardKey);
      event.register(abilitySwapKey);
      assignHeartKey = new KeyMapping("key.viltrumitecore.assign_heart", net.minecraftforge.client.settings.KeyConflictContext.IN_GAME,
         com.mojang.blaze3d.platform.InputConstants.Type.MOUSE, org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_MIDDLE, "category.viltrumitecore.keys");
      event.register(assignHeartKey);
      superJumpKey = new KeyMapping("key.viltrumitecore.regulus_super_jump", net.minecraftforge.client.settings.KeyConflictContext.IN_GAME,
         com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_G, "category.viltrumitecore.keys");
      event.register(superJumpKey);
   }

   /** True while a Regulus is playing and the heart key shares the given key with another mapping. */
   public static boolean heartKeyOwns(Minecraft client, KeyMapping other) {
      return assignHeartKey != null
         && client.player instanceof dev.baranhan.viltrumitecore.hero.HeroPlayer heroPlayer
         && heroPlayer.getHeroId() == dev.baranhan.viltrumitecore.hero.HeroId.REGULUS
         && !assignHeartKey.isUnbound()
         && assignHeartKey.getKey().equals(other.getKey());
   }

   public static void tick(Minecraft client) {
      // Regulus assigns hearts on the dedicated heart key (default MMB). The
      // vanilla pick-block sharing that button is cancelled in
      // RegulusInputPriority, so the heart key always wins for Regulus.
      if (client.player instanceof dev.baranhan.viltrumitecore.hero.HeroPlayer heroPlayer
         && heroPlayer.getHeroId() == dev.baranhan.viltrumitecore.hero.HeroId.REGULUS) {
         if (heartKeyOwns(client, client.options.keyPickItem)) {
            // Drain stray pick-item clicks so a later tick never replays them.
            while (client.options.keyPickItem.consumeClick()) {
            }
         }
         while (assignHeartKey.consumeClick()) {
            if (client.screen == null) {
               CoreMessages.sendToServer(new dev.baranhan.viltrumitecore.network.packet.HeroInputC2SPacket(
                  dev.baranhan.viltrumitecore.hero.HeroAction.ASSIGN_HEART, true));
            }
         }
      }
      while (abilitySwapKey.consumeClick()) {
         Player player = client.player;
         if (player == null || !dev.baranhan.viltrumitecore.hero.HeroRegistry.get(player).allowsAbilityPages(player)) {
            return;
         }

         CoreMessages.sendToServer(new SwapAbilityBarC2SPacket());
         client.getSoundManager().play(SimpleSoundInstance.forUI((SoundEvent)SoundEvents.UI_BUTTON_CLICK.get(), 1.1F, 0.3F));
      }

      while (dashboardKey.consumeClick()) {
         if (client.screen == null) {
            client.setScreen(new ViltrumiteDashboardScreen());
            client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
         }
      }
   }

   public static boolean consumeAbilityKeyPress(Player player, String targetAbilityId) {
      if (player instanceof ViltrumiteAbilityUser abilityUser) {
         int offset = abilityUser.getActivePage() * 6;
         if (targetAbilityId.equals(abilityUser.getAbilityInSlot(offset)) && abilityKey1.consumeClick()) {
            return true;
         } else if (targetAbilityId.equals(abilityUser.getAbilityInSlot(offset + 1)) && abilityKey2.consumeClick()) {
            return true;
         } else if (targetAbilityId.equals(abilityUser.getAbilityInSlot(offset + 2)) && abilityKey3.consumeClick()) {
            return true;
         } else if (targetAbilityId.equals(abilityUser.getAbilityInSlot(offset + 3)) && abilityKey4.consumeClick()) {
            return true;
         } else {
            return targetAbilityId.equals(abilityUser.getAbilityInSlot(offset + 4)) && abilityKey5.consumeClick()
               ? true
               : targetAbilityId.equals(abilityUser.getAbilityInSlot(offset + 5)) && abilityKey6.consumeClick();
         }
      } else {
         return false;
      }
   }

   public static boolean isAbilityKeyDown(Player player, String targetAbilityId) {
      if (player instanceof ViltrumiteAbilityUser abilityUser) {
         int offset = abilityUser.getActivePage() * 6;
         if (targetAbilityId.equals(abilityUser.getAbilityInSlot(offset)) && abilityKey1.isDown()) {
            return true;
         } else if (targetAbilityId.equals(abilityUser.getAbilityInSlot(offset + 1)) && abilityKey2.isDown()) {
            return true;
         } else if (targetAbilityId.equals(abilityUser.getAbilityInSlot(offset + 2)) && abilityKey3.isDown()) {
            return true;
         } else if (targetAbilityId.equals(abilityUser.getAbilityInSlot(offset + 3)) && abilityKey4.isDown()) {
            return true;
         } else {
            return targetAbilityId.equals(abilityUser.getAbilityInSlot(offset + 4)) && abilityKey5.isDown()
               ? true
               : targetAbilityId.equals(abilityUser.getAbilityInSlot(offset + 5)) && abilityKey6.isDown();
         }
      } else {
         return false;
      }
   }
}
