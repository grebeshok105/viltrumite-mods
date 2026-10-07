package dev.baranhan.viltrumitecore.client.regulus;

import dev.baranhan.viltrumitecore.ViltrumiteCore;
import dev.baranhan.viltrumitecore.client.ViltrumiteCoreClient;
import dev.baranhan.viltrumitecore.client.hero.ClientHeroData;
import dev.baranhan.viltrumitecore.hero.HeroId;
import dev.baranhan.viltrumitecore.hero.HeroPlayer;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import dev.baranhan.viltrumitecore.hero.control.ControlKind;
import dev.baranhan.viltrumitecore.hero.regulus.RegulusRules;
import dev.baranhan.viltrumitecore.network.packet.HeroControlS2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

/**
 * Charged super-jump, client side (player movement is client-authoritative,
 * so a server-side velocity never reached the player — that is why holding
 * jump "did nothing"). While jump is held on the ground the vanilla hop is
 * suppressed and the charge builds; on release a short tap is a normal jump
 * and a real charge launches with RegulusRules.jumpVelocity. The held-key
 * input still goes to the server, which plays the launch for everyone.
 */
@EventBusSubscriber(
   modid = "viltrumitecore",
   bus = Bus.FORGE,
   value = {Dist.CLIENT}
)
public final class RegulusJumpClient {
   private static int charge;
   private static boolean charging;

   private RegulusJumpClient() {
   }

   /** 0..1 for the HUD charge bar. */
   public static float chargeFraction() {
      return charging ? Math.min(1.0F, charge / (float)RegulusRules.JUMP_CHARGE_TICKS) : 0.0F;
   }

   @SubscribeEvent
   public static void onMovementInput(MovementInputUpdateEvent event) {
      if (!(event.getEntity() instanceof LocalPlayer player) || !(player instanceof HeroPlayer heroPlayer)) {
         return;
      }

      HeroPublicSnapshot snapshot = heroPlayer.getHeroSnapshot();
      if (snapshot == null || snapshot.heroId() != HeroId.REGULUS || !canCharge(player, snapshot)) {
         charging = false;
         charge = 0;
         return;
      }

      boolean held = event.getInput().jumping;
      if (held && player.onGround()) {
         // Hold: no vanilla hop, build the charge.
         event.getInput().jumping = false;
         charging = true;
         charge = Math.min(charge + 1, RegulusRules.JUMP_CHARGE_TICKS);
         chargeFeedback(player);
         return;
      }

      if (charging && !held) {
         charging = false;
         int released = charge;
         charge = 0;
         if (released < RegulusRules.JUMP_MIN_CHARGE_TICKS) {
            // A tap stays a normal jump (vanilla runs it this tick).
            event.getInput().jumping = true;
            return;
         }

         Vec3 delta = player.getDeltaMovement();
         Vec3 look = player.getLookAngle();
         float velocity = RegulusRules.jumpVelocity(released);
         float fraction = released / (float)RegulusRules.JUMP_CHARGE_TICKS;
         // A little of the facing goes into the launch, so it can travel.
         double forward = 0.35 * fraction;
         player.setDeltaMovement(delta.x + look.x * forward, velocity, delta.z + look.z * forward);
         player.hasImpulse = true;
         return;
      }

      if (!player.onGround()) {
         charging = false;
         charge = 0;
      }
   }

   private static boolean canCharge(LocalPlayer player, HeroPublicSnapshot snapshot) {
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

   private static void chargeFeedback(LocalPlayer player) {
      if (charge == RegulusRules.JUMP_MIN_CHARGE_TICKS) {
         player.level().playLocalSound(player.getX(), player.getY(), player.getZ(), ViltrumiteCore.REGULUS_JUMP_CHARGE.get(), SoundSource.PLAYERS, 0.6F, 0.9F, false);
      }

      if (charge == RegulusRules.JUMP_CHARGE_TICKS - 1) {
         player.level().playLocalSound(player.getX(), player.getY(), player.getZ(), ViltrumiteCore.REGULUS_JUMP_CHARGE.get(), SoundSource.PLAYERS, 0.8F, 1.4F, false);
      }

      if (charge >= RegulusRules.JUMP_MIN_CHARGE_TICKS && charge % 2 == 0) {
         float f = charge / (float)RegulusRules.JUMP_CHARGE_TICKS;
         double az = player.getRandom().nextDouble() * Math.PI * 2.0;
         double r = 0.4 + 0.5 * f;
         player.level().addParticle(ParticleTypes.CLOUD, player.getX() + Math.cos(az) * r, player.getY() + 0.05, player.getZ() + Math.sin(az) * r,
            -Math.cos(az) * 0.04, 0.02 + 0.04 * f, -Math.sin(az) * 0.04);
      }
   }
}
