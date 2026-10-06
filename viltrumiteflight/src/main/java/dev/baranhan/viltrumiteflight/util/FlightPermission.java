package dev.baranhan.viltrumiteflight.util;

import net.minecraft.world.entity.player.Player;

@FunctionalInterface
public interface FlightPermission {
   boolean allowsModFlight(Player player);
}
