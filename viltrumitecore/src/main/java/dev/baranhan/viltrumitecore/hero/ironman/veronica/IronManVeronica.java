package dev.baranhan.viltrumitecore.hero.ironman.veronica;

import dev.baranhan.viltrumitecore.entity.VeronicaPodEntity;
import dev.baranhan.viltrumitecore.hero.ironman.IronManState;
import dev.baranhan.viltrumitecore.hero.ironman.mark.IronManMarks;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkId;
import dev.baranhan.viltrumitecore.network.CoreMessages;
import dev.baranhan.viltrumitecore.network.packet.VeronicaMenuS2CPacket;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Server driver of Veronica (spec §12.1–§12.3): call the pod, open the suit
 * menu while it stands within {@link VeronicaPodEntity#MENU_RANGE}, validate
 * the choice and hand it to {@link IronManMarks}. Cooldown 3 min after the
 * pod leaves (extraCooldowns[2]).
 */
@EventBusSubscriber(modid = "viltrumitecore")
public final class IronManVeronica {
   public static final int COOLDOWN = 3600;

   private IronManVeronica() {
   }

   /** Slot press: call the pod, or open the menu while it stands near. */
   public static void press(ServerPlayer player, IronManState state) {
      VeronicaPodEntity pod = pod(player, state);
      if (pod != null) {
         if (!pod.landed()) {
            return;
         }

         if (player.distanceToSqr(pod) > VeronicaPodEntity.MENU_RANGE * VeronicaPodEntity.MENU_RANGE) {
            player.displayClientMessage(Component.translatable("hud.viltrumitecore.ironman.veronica_far"), true);
            return;
         }

         openMenu(player, state, pod);
         return;
      }

      if (state.veronicaCooldown > 0) {
         player.displayClientMessage(Component.translatable("hud.viltrumitecore.ironman.veronica_cooldown", (state.veronicaCooldown + 19) / 20), true);
         return;
      }

      ServerLevel level = player.serverLevel();
      Vec3 ground = DropPoint.pick(player.getRandom(), player.position(), surface(level));
      state.podId = VeronicaPodEntity.drop(player, ground).getId();
   }

   static void openMenu(ServerPlayer player, IronManState state, VeronicaPodEntity pod) {
      int left = Math.max(0, VeronicaPodEntity.LANDED_TICKS - pod.phaseTicks());
      CoreMessages.sendToPlayer(new VeronicaMenuS2CPacket(VeronicaView.of(state.roster, -1, left)), player);
   }

   /** The menu choice: every rule is checked here, the client only displays (spec §12.3). */
   public static void choose(ServerPlayer player, int choice) {
      IronManState state = IronManState.of(player);
      if (state == null || IronManMarks.controlled(player)) {
         return;
      }

      MarkId id = MarkId.byId(choice);
      if (id == null) {
         return;
      }

      VeronicaPodEntity pod = pod(player, state);
      if (pod == null || !pod.landed() || player.distanceToSqr(pod) > VeronicaPodEntity.MENU_RANGE * VeronicaPodEntity.MENU_RANGE) {
         player.displayClientMessage(Component.translatable("hud.viltrumitecore.ironman.veronica_far"), true);
         return;
      }

      switch (IronManMarks.deliver(player, state, id, pod.position().add(0.0, 2.2, 0.0))) {
         case BUSY -> player.displayClientMessage(Component.translatable("hud.viltrumitecore.ironman.veronica_busy"), true);
         case UNAVAILABLE -> player.displayClientMessage(Component.translatable("hud.viltrumitecore.ironman.mark_unavailable"), true);
         default -> {
         }
      }
   }

   @Nullable
   public static VeronicaPodEntity pod(ServerPlayer player, IronManState state) {
      if (state.podId < 0) {
         return null;
      }

      Entity entity = player.level().getEntity(state.podId);
      if (entity instanceof VeronicaPodEntity pod && pod.ownedBy(player) && pod.phase() != VeronicaPodEntity.Phase.LEAVING) {
         return pod;
      }

      return null;
   }

   public static void tick(ServerPlayer player, IronManState state) {
      if (state.veronicaCooldown > 0) {
         state.veronicaCooldown--;
      }

      // The pod vanished without leaving (chunk unload, removed): free the slot, start the cooldown.
      if (state.podId >= 0 && player.level().getEntity(state.podId) == null) {
         state.podId = -1;
         state.veronicaCooldown = Math.max(state.veronicaCooldown, COOLDOWN);
      }
   }

   public static void onPodLanded(ServerPlayer owner, VeronicaPodEntity pod) {
   }

   public static void onPodLeft(ServerPlayer owner, VeronicaPodEntity pod) {
      IronManState state = IronManState.of(owner);
      if (state != null && state.podId == pod.getId()) {
         state.podId = -1;
         state.veronicaCooldown = COOLDOWN;
      }
   }

   /** The landing blast never hits the owner (it is his delivery). */
   @SubscribeEvent
   public static void onDetonate(ExplosionEvent.Detonate event) {
      if (event.getExplosion().getDirectSourceEntity() instanceof VeronicaPodEntity pod) {
         event.getAffectedEntities().removeIf(entity -> entity == pod || pod.ownedBy(entity));
      }
   }

   static DropPoint.Surface surface(ServerLevel level) {
      return (x, z) -> {
         if (!level.hasChunk(x >> 4, z >> 4)) {
            return null;
         }

         BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
         BlockPos below = top.below();
         if (below.getY() < level.getMinBuildHeight()) {
            return null;
         }

         BlockState ground = level.getBlockState(below);
         if (!ground.getFluidState().isEmpty() || !level.getFluidState(top).isEmpty() || !ground.isFaceSturdy(level, below, Direction.UP)) {
            return null;
         }

         return (double)top.getY();
      };
   }
}
