package dev.baranhan.viltrumitecore.entity;

import dev.baranhan.viltrumitecore.hero.ironman.IronManState;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Owner lookup shared by Iron Man world entities (pod, empty suit). Server only. */
public final class IronManOwned {
   private IronManOwned() {
   }

   /** The online, living Iron Man owner in the same level, or null (then the entity leaves). */
   @Nullable
   public static ServerPlayer owner(Entity entity, Optional<UUID> ownerId) {
      if (ownerId.isEmpty() || !(entity.level() instanceof ServerLevel level)) {
         return null;
      }

      ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId.get());
      if (owner == null || owner.level() != level || !owner.isAlive() || IronManState.of(owner) == null) {
         return null;
      }

      return owner;
   }
}
