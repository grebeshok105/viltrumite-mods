package dev.baranhan.viltrumitecore.hero.ironman.scan;

import net.minecraft.network.FriendlyByteBuf;

/** Active effect on a scanned target: effect translation key, amplifier, ticks left (-1 = infinite). */
public record EffectLine(String descriptionId, int amplifier, int duration, boolean beneficial) {
   public void write(FriendlyByteBuf buffer) {
      buffer.writeUtf(this.descriptionId, 128);
      buffer.writeVarInt(Math.max(0, this.amplifier));
      buffer.writeInt(this.duration);
      buffer.writeBoolean(this.beneficial);
   }

   public static EffectLine read(FriendlyByteBuf buffer) {
      return new EffectLine(buffer.readUtf(128), buffer.readVarInt(), buffer.readInt(), buffer.readBoolean());
   }
}
