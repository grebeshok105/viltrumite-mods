package dev.baranhan.viltrumiteflight.network;

import dev.baranhan.viltrumiteflight.network.packet.FlightAccelerateC2SPacket;
import dev.baranhan.viltrumiteflight.network.packet.FlightSpeedLockC2SPacket;
import dev.baranhan.viltrumiteflight.network.packet.FlightToggleC2SPacket;
import dev.baranhan.viltrumiteflight.network.packet.HoverInputC2SPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry.ChannelBuilder;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModMessages {
   private static SimpleChannel INSTANCE;
   private static int packetId = 0;

   private static int id() {
      return packetId++;
   }

   public static void register() {
      SimpleChannel net = ChannelBuilder.named(new ResourceLocation("viltrumiteflight", "messages"))
         .networkProtocolVersion(() -> "1.0")
         .clientAcceptedVersions(s -> true)
         .serverAcceptedVersions(s -> true)
         .simpleChannel();
      INSTANCE = net;
      net.registerMessage(id(), FlightToggleC2SPacket.class, FlightToggleC2SPacket::toBytes, FlightToggleC2SPacket::new, FlightToggleC2SPacket::handle);
      net.registerMessage(
         id(), FlightAccelerateC2SPacket.class, FlightAccelerateC2SPacket::toBytes, FlightAccelerateC2SPacket::new, FlightAccelerateC2SPacket::handle
      );
      net.registerMessage(id(), HoverInputC2SPacket.class, HoverInputC2SPacket::toBytes, HoverInputC2SPacket::new, HoverInputC2SPacket::handle);
      net.registerMessage(
         id(), FlightSpeedLockC2SPacket.class, FlightSpeedLockC2SPacket::toBytes, FlightSpeedLockC2SPacket::new, FlightSpeedLockC2SPacket::handle
      );
   }

   public static <MSG> void sendToServer(MSG message) {
      INSTANCE.sendToServer(message);
   }
}
