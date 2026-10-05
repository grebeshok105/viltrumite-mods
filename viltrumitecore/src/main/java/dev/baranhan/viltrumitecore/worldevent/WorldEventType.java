package dev.baranhan.viltrumitecore.worldevent;

import java.util.function.Supplier;
import net.minecraft.ChatFormatting;

public record WorldEventType(
   String id, ChatFormatting color, String warnKey, String startKey, String endKey, int defaultWarmupTicks, Supplier<WorldEvent> factory
) {
   public WorldEvent create() {
      WorldEvent event = this.factory.get();
      event.setType(this);
      return event;
   }
}
