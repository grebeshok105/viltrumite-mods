package dev.baranhan.viltrumitecore.entity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * An entity a hero uses with RMB instead of firing the current tool (Stage 4:
 * Iron Man's empty suit). The client asks {@link #canHeroInteract} for the
 * crosshair entity; the server re-checks distance, line of sight and its own
 * rules before {@link #heroInteract}.
 */
public interface HeroInteractable {
   /** Both sides; client: synced data only. */
   boolean canHeroInteract(Player who);

   /** Server: perform the interaction. True when it happened. */
   boolean heroInteract(ServerPlayer who);
}
