package dev.baranhan.viltrumitecore.client.hero;

/**
 * Owner-private hero data mirrored on the client (heart carrier ids for the
 * heart counter). Written by HeroOwnerSnapshotS2CPacket.
 */
public final class ClientHeroData {
   private static int[] carrierEntityIds = new int[0];

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
}
