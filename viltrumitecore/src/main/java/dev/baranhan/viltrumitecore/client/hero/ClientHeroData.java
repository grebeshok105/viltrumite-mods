package dev.baranhan.viltrumitecore.client.hero;

import dev.baranhan.viltrumitecore.network.packet.HeroControlS2CPacket;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Owner-private hero data mirrored on the client (heart carrier ids for the
 * heart counter), plus the shared world-control view (domes + anchored
 * targets) written by HeroControlS2CPacket for render and HUD consumers.
 */
public final class ClientHeroData {
   private static int[] carrierEntityIds = new int[0];
   private static ResourceKey<Level> controlDimension;
   private static Map<UUID, HeroControlS2CPacket.DomeInfo> domes = Map.of();
   private static Map<UUID, HeroControlS2CPacket.ControlInfo> controls = Map.of();

   private ClientHeroData() {
   }

   public static void setCarriers(int[] ids) {
      carrierEntityIds = ids == null ? new int[0] : ids;
   }

   public static int[] carriers() {
      return carrierEntityIds;
   }

   public static int heartCount() {
      return carrierEntityIds.length;
   }

   /** Replaces the whole control view for the given dimension (full snapshot). */
   public static void setControlSnapshot(ResourceKey<Level> dimension, List<HeroControlS2CPacket.DomeInfo> domeList, List<HeroControlS2CPacket.ControlInfo> controlList) {
      controlDimension = dimension;
      Map<UUID, HeroControlS2CPacket.DomeInfo> domeMap = new java.util.HashMap<>();
      for (HeroControlS2CPacket.DomeInfo dome : domeList) {
         domeMap.put(dome.id(), dome);
      }

      Map<UUID, HeroControlS2CPacket.ControlInfo> controlMap = new java.util.HashMap<>();
      for (HeroControlS2CPacket.ControlInfo control : controlList) {
         controlMap.put(control.target(), control);
      }

      domes = Map.copyOf(domeMap);
      controls = Map.copyOf(controlMap);
   }

   /** Domes in the client's current dimension. */
   public static Collection<HeroControlS2CPacket.DomeInfo> domes() {
      return domes.values();
   }

   /** Live control records keyed by target UUID. */
   public static Collection<HeroControlS2CPacket.ControlInfo> controls() {
      return controls.values();
   }

   /** The control record anchored on this entity, if any. */
   public static HeroControlS2CPacket.ControlInfo controlOf(UUID targetId) {
      return controls.get(targetId);
   }
}
