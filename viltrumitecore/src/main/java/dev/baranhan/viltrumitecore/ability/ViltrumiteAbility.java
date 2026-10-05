package dev.baranhan.viltrumitecore.ability;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class ViltrumiteAbility {
   private final String id;
   private final ResourceLocation icon;
   private final String name;
   private final String description;
   private final int displayCooldown;
   private final Predicate<Player> shouldRenderGrey;

   public ViltrumiteAbility(String id, ResourceLocation icon, String name, String description, int displayCooldown, Predicate<Player> shouldRenderGrey) {
      this.id = id;
      this.icon = icon;
      this.name = name;
      this.description = description;
      this.displayCooldown = displayCooldown;
      this.shouldRenderGrey = shouldRenderGrey;
   }

   public ViltrumiteAbility(String id, ResourceLocation icon, String name, String description, int displayCooldown) {
      this(id, icon, name, description, displayCooldown, player -> false);
   }

   public String getId() {
      return this.id;
   }

   public ResourceLocation getIcon() {
      return this.icon;
   }

   public boolean isGrey(Player player) {
      return this.shouldRenderGrey.test(player);
   }

   private String parseTags(String text) {
      return text == null
         ? ""
         : text.replace("<yellow>", ChatFormatting.YELLOW.toString())
            .replace("<grey>", ChatFormatting.GRAY.toString())
            .replace("<lime>", ChatFormatting.GREEN.toString())
            .replace("<aqua>", ChatFormatting.AQUA.toString())
            .replace("<blue>", ChatFormatting.BLUE.toString())
            .replace("<red>", ChatFormatting.RED.toString())
            .replace("<white>", ChatFormatting.WHITE.toString())
            .replace("<b>", ChatFormatting.BOLD.toString())
            .replace("<r>", ChatFormatting.RESET.toString());
   }

   public List<Component> getTooltip() {
      List<Component> tooltip = new ArrayList<>();
      if (this.name != null && !this.name.isEmpty()) {
         String localizedName = Component.translatable(this.name).getString();
         tooltip.add(Component.literal(this.parseTags(localizedName)));
      }

      if (this.description != null && !this.description.isEmpty()) {
         String localizedDesc = Component.translatable(this.description).getString();
         String[] lines = this.parseTags(localizedDesc).split("\n");

         for (String line : lines) {
            tooltip.add(Component.literal(line));
         }
      }

      return tooltip;
   }
}
