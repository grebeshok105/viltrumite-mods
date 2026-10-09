package dev.baranhan.viltrumitecore.hero;

import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

/**
 * One line of a scan card: a translation key and plain string arguments.
 * Kept as data (not a Component) so the rules stay testable and the
 * owner's client translates it into its own language.
 */
public record ScanLine(String key, List<String> args) {
   private static final int MAX_ARGS = 4;

   public ScanLine {
      args = args == null ? List.of() : List.copyOf(args.size() > MAX_ARGS ? args.subList(0, MAX_ARGS) : args);
   }

   public static ScanLine of(String key, Object... args) {
      String[] strings = new String[args.length];
      for (int i = 0; i < args.length; i++) {
         strings[i] = String.valueOf(args[i]);
      }

      return new ScanLine(key, List.of(strings));
   }

   public Component text() {
      return Component.translatable(this.key, this.args.toArray());
   }

   public void write(FriendlyByteBuf buffer) {
      buffer.writeUtf(this.key, 128);
      buffer.writeVarInt(this.args.size());
      for (String arg : this.args) {
         buffer.writeUtf(arg, 64);
      }
   }

   public static ScanLine read(FriendlyByteBuf buffer) {
      String key = buffer.readUtf(128);
      int count = Math.min(MAX_ARGS, buffer.readVarInt());
      String[] args = new String[count];
      for (int i = 0; i < count; i++) {
         args[i] = buffer.readUtf(64);
      }

      return new ScanLine(key, List.of(args));
   }
}
