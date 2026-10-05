package dev.baranhan.viltrumitecore.worldevent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;

public final class WorldEvents {
   private static final Map<String, WorldEventType> TYPES = new LinkedHashMap<>();
   public static final WorldEventType CONQUEST = register(
      new WorldEventType(
         "conquest",
         ChatFormatting.LIGHT_PURPLE,
         "event.viltrumitecore.conquest.warn",
         "event.viltrumitecore.conquest.start",
         "event.viltrumitecore.conquest.end",
         2400,
         ConquestEvent::new
      )
   );
   public static final WorldEventType VILTRUMITE_TRIO = register(
      new WorldEventType(
         "trio",
         ChatFormatting.LIGHT_PURPLE,
         "event.viltrumitecore.trio.warn",
         "event.viltrumitecore.trio.start",
         "event.viltrumitecore.trio.end",
         1800,
         ViltrumiteTrioEvent::new
      )
   );

   private static WorldEventType register(WorldEventType type) {
      TYPES.put(type.id(), type);
      return type;
   }

   public static WorldEventType byId(String id) {
      return TYPES.get(id);
   }

   public static Collection<WorldEventType> all() {
      return TYPES.values();
   }

   public static Collection<String> ids() {
      return TYPES.keySet();
   }

   public static List<WorldEventType> asList() {
      return new ArrayList<>(TYPES.values());
   }

   private WorldEvents() {
   }
}
