package dev.baranhan.viltrumitecore.client.regulus;

import dev.baranhan.viltrumitecore.client.ViltrumiteCoreClient;
import dev.baranhan.viltrumitecore.client.hero.ClientHeroData;
import dev.baranhan.viltrumitecore.hero.HeroId;
import dev.baranhan.viltrumitecore.hero.HeroPlayer;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import dev.baranhan.viltrumitecore.hero.control.ControlKind;
import dev.baranhan.viltrumitecore.hero.regulus.RegulusRules;
import dev.baranhan.viltrumitecore.network.packet.HeroControlS2CPacket;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Super jump, client side: one press of the super-jump key (G) launches
 * Regulus ~20 blocks up instantly. Player movement is client-authoritative,
 * so the launch velocity is applied here; the server only plays the launch
 * sound and FX for everyone. The space bar stays a normal vanilla jump.
 */
public final class RegulusJumpClient {
   private RegulusJumpClient() {
   }

   /** Launch if allowed; true when the server should be told (for FX). */
   public static boolean tryLaunch(LocalPlayer player) {
      if (!(player instanceof HeroPlayer heroPlayer)) {
         return false;
      }

      HeroPublicSnapshot snapshot = heroPlayer.getHeroSnapshot();
      if (snapshot == null || snapshot.heroId() != HeroId.REGULUS || !player.onGround() || !canJump(player, snapshot)) {
         return false;
      }

      Vec3 delta = player.getDeltaMovement();
      Vec3 look = player.getLookAngle();
      // A little of the facing goes into the launch, so it can travel.
      player.setDeltaMovement(
         delta.x + look.x * RegulusRules.SUPER_JUMP_FORWARD,
         RegulusRules.SUPER_JUMP_VELOCITY,
         delta.z + look.z * RegulusRules.SUPER_JUMP_FORWARD
      );
      player.hasImpulse = true;
      return true;
   }

   private static boolean canJump(LocalPlayer player, HeroPublicSnapshot snapshot) {
      if (player.isSpectator() || player.getAbilities().flying || player.isInWater() || player.isInLava() || player.isPassenger()) {
         return false;
      }

      if (snapshot.controlTargetId() >= 0 || ViltrumiteCoreClient.iAmBeingGrabbedBy != null) {
         return false;
      }

      for (HeroControlS2CPacket.ControlInfo control : ClientHeroData.controls()) {
         if (control.entityId() == player.getId()) {
            int kind = control.kindOrdinal();
            if (kind == ControlKind.FREEZE.ordinal() || kind == ControlKind.STASIS.ordinal()) {
               return false;
            }
         }
      }

      return true;
   }
}
