package dev.baranhan.viltrumitecore.hero.ironman.scan;

import dev.baranhan.viltrumitecore.hero.ScanLine;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

/** Result card of a finished scan (server → owner). */
public record ScanCard(
   int entityId,
   Component name,
   float hp,
   float maxHp,
   float armor,
   float toughness,
   List<EffectLine> effects,
   List<ScanLine> resists,
   float attackDamage,
   float moveSpeed,
   List<ScanLine> weakSpots,
   List<ScanLine> conditions
) {
   public static final int MAX_LINES = 8;

   public ScanCard {
      effects = cap(effects);
      resists = cap(resists);
      weakSpots = cap(weakSpots);
      conditions = cap(conditions);
   }

   private static <T> List<T> cap(List<T> list) {
      if (list == null) {
         return List.of();
      }

      return List.copyOf(list.size() > MAX_LINES ? list.subList(0, MAX_LINES) : list);
   }

   public void write(FriendlyByteBuf buffer) {
      buffer.writeInt(this.entityId);
      buffer.writeComponent(this.name);
      buffer.writeFloat(this.hp);
      buffer.writeFloat(this.maxHp);
      buffer.writeFloat(this.armor);
      buffer.writeFloat(this.toughness);
      buffer.writeVarInt(this.effects.size());
      this.effects.forEach(line -> line.write(buffer));
      writeLines(buffer, this.resists);
      buffer.writeFloat(this.attackDamage);
      buffer.writeFloat(this.moveSpeed);
      writeLines(buffer, this.weakSpots);
      writeLines(buffer, this.conditions);
   }

   public static ScanCard read(FriendlyByteBuf buffer) {
      int id = buffer.readInt();
      Component name = buffer.readComponent();
      float hp = buffer.readFloat();
      float maxHp = buffer.readFloat();
      float armor = buffer.readFloat();
      float toughness = buffer.readFloat();
      int count = Math.min(MAX_LINES, buffer.readVarInt());
      List<EffectLine> effects = new ArrayList<>(count);
      for (int i = 0; i < count; i++) {
         effects.add(EffectLine.read(buffer));
      }

      List<ScanLine> resists = readLines(buffer);
      float attack = buffer.readFloat();
      float speed = buffer.readFloat();
      return new ScanCard(id, name, hp, maxHp, armor, toughness, effects, resists, attack, speed, readLines(buffer), readLines(buffer));
   }

   private static void writeLines(FriendlyByteBuf buffer, List<ScanLine> lines) {
      buffer.writeVarInt(lines.size());
      lines.forEach(line -> line.write(buffer));
   }

   private static List<ScanLine> readLines(FriendlyByteBuf buffer) {
      int count = Math.min(MAX_LINES, buffer.readVarInt());
      List<ScanLine> lines = new ArrayList<>(count);
      for (int i = 0; i < count; i++) {
         lines.add(ScanLine.read(buffer));
      }

      return lines;
   }
}
