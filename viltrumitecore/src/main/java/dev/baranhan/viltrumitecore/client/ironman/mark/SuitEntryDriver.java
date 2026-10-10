package dev.baranhan.viltrumitecore.client.ironman.mark;

import dev.baranhan.viltrumitecore.entity.EmptySuitEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

/**
 * Walk-in of the empty suit (spec §12.6), client side. The server only
 * starts {@link EmptySuitEntity.Phase#ENTERING} and snaps the owner onto the
 * suit at {@link EmptySuitEntity#ENTER_SEAL}; the local owner's walk is
 * scripted here so it is smooth: walk to the front point facing the suit
 * (0–14), turn round to the suit's facing (14–24), step back into the shell
 * (24–32), then stand still in the rest pose while the plates close. Movement
 * keys are ignored meanwhile, yaw and pitch are driven per frame. For every
 * entering player (remote ones too) the body is kept on the suit's facing
 * and {@link #restWeight} drives the rest pose, so the closing plates meet the
 * limbs exactly.
 */
@EventBusSubscriber(
   modid = "viltrumitecore",
   bus = Bus.FORGE,
   value = {Dist.CLIENT}
)
public final class SuitEntryDriver {
   /** Owner → suit being entered, rebuilt each client tick. */
   private static final Map<UUID, EmptySuitEntity> ENTERING = new HashMap<>();
   @Nullable
   private static Walk walk;

   private SuitEntryDriver() {
   }

   /** The suit this player is walking into, or null. */
   @Nullable
   public static EmptySuitEntity suitOf(Entity player) {
      EmptySuitEntity suit = ENTERING.get(player.getUUID());
      return suit != null && !suit.isRemoved() && suit.phase() == EmptySuitEntity.Phase.ENTERING ? suit : null;
   }

   /** Entering time in ticks (fractional), or -1 when not entering. */
   public static float enterTime(Entity player, float partialTick) {
      EmptySuitEntity suit = suitOf(player);
      return suit == null ? -1.0F : suit.clientPhaseAge() + partialTick;
   }

   /** Weight 0..1 of the straight rest pose (arms down, head forward) before the plates close. */
   public static float restWeight(Entity player, float partialTick) {
      float t = enterTime(player, partialTick);
      if (t < EmptySuitEntity.ENTER_TURN) {
         return 0.0F;
      }

      float x = Mth.clamp((t - EmptySuitEntity.ENTER_TURN) / (EmptySuitEntity.ENTER_SEAL - EmptySuitEntity.ENTER_TURN), 0.0F, 1.0F);
      return x * x * (3.0F - 2.0F * x);
   }

   @SubscribeEvent
   public static void onClientTick(TickEvent.ClientTickEvent event) {
      Minecraft client = Minecraft.getInstance();
      if (client.level == null || client.player == null) {
         ENTERING.clear();
         walk = null;
         return;
      }

      if (event.phase == TickEvent.Phase.START) {
         ENTERING.clear();
         for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof EmptySuitEntity suit && suit.phase() == EmptySuitEntity.Phase.ENTERING) {
               suit.ownerId().ifPresent(id -> ENTERING.put(id, suit));
            }
         }

         drive(client.player);
      }
   }

   private static void drive(LocalPlayer me) {
      EmptySuitEntity suit = suitOf(me);
      if (suit == null) {
         walk = null;
         return;
      }

      if (walk == null || walk.suit != suit) {
         walk = new Walk(suit, me.position(), me.getYRot(), me.getXRot());
      }

      int t = suit.clientPhaseAge();
      Vec3 target = walk.position(t + 1.0F);
      Vec3 step = target.subtract(me.position());
      Vec3 motion = me.getDeltaMovement();
      // Applied before the player's own tick: the scripted step is exactly this tick's horizontal travel.
      me.setDeltaMovement(step.x, motion.y, step.z);
      me.setSprinting(false);
      apply(me, walk, t);
   }

   @SubscribeEvent
   public static void onRenderTick(TickEvent.RenderTickEvent event) {
      Minecraft client = Minecraft.getInstance();
      if (event.phase != TickEvent.Phase.START || client.player == null || walk == null || suitOf(client.player) != walk.suit) {
         return;
      }

      apply(client.player, walk, walk.suit.clientPhaseAge() + event.renderTickTime);
   }

   /** Scripted look: overwrites mouse turns every frame (camera follows the walk). */
   private static void apply(LocalPlayer me, Walk walk, float t) {
      float yaw = walk.yaw(t);
      float pitch = walk.pitch(t);
      me.setYRot(yaw);
      me.yRotO = yaw;
      me.setXRot(pitch);
      me.xRotO = pitch;
      me.setYHeadRot(yaw);
      me.yHeadRotO = yaw;
      if (t >= EmptySuitEntity.ENTER_APPROACH) {
         me.setYBodyRot(yaw);
         me.yBodyRotO = yaw;
      }
   }

   @SubscribeEvent
   public static void onMovementInput(MovementInputUpdateEvent event) {
      if (suitOf(event.getEntity()) == null) {
         return;
      }

      Input input = event.getInput();
      input.forwardImpulse = 0.0F;
      input.leftImpulse = 0.0F;
      input.up = false;
      input.down = false;
      input.left = false;
      input.right = false;
      input.jumping = false;
      input.shiftKeyDown = false;
   }

   /** Remote players: the body stands on the suit's facing once the turn is done. */
   @SubscribeEvent(priority = EventPriority.HIGH)
   public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
      LivingEntity entity = event.getEntity();
      if (!(entity instanceof Player)) {
         return;
      }

      EmptySuitEntity suit = suitOf(entity);
      if (suit == null || suit.clientPhaseAge() < EmptySuitEntity.ENTER_TURN) {
         return;
      }

      float yaw = suit.getYRot();
      entity.yBodyRot = yaw;
      entity.yBodyRotO = yaw;
      entity.yHeadRot = yaw;
      entity.yHeadRotO = yaw;
   }

   @SubscribeEvent
   public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
      ENTERING.clear();
      walk = null;
   }

   /** The scripted path of one entry, from where the owner stood when it started. */
   private static final class Walk {
      final EmptySuitEntity suit;
      final Vec3 start;
      final Vec3 front;
      final Vec3 inside;
      final float startYaw;
      final float startPitch;
      final float faceSuitYaw;
      final float suitYaw;

      Walk(EmptySuitEntity suit, Vec3 start, float startYaw, float startPitch) {
         this.suit = suit;
         this.start = start;
         this.inside = suit.position();
         Vec3 front = suit.frontPoint();
         this.front = new Vec3(front.x, start.y, front.z);
         this.suitYaw = suit.getYRot();
         this.faceSuitYaw = this.suitYaw + 180.0F;
         this.startYaw = startYaw;
         this.startPitch = startPitch;
      }

      Vec3 position(float t) {
         if (t <= EmptySuitEntity.ENTER_APPROACH) {
            float k = ease(t / EmptySuitEntity.ENTER_APPROACH);
            return new Vec3(Mth.lerp(k, this.start.x, this.front.x), this.start.y, Mth.lerp(k, this.start.z, this.front.z));
         }

         if (t <= EmptySuitEntity.ENTER_TURN) {
            return this.front;
         }

         float k = ease(Math.min(1.0F, (t - EmptySuitEntity.ENTER_TURN) / (EmptySuitEntity.ENTER_SEAL - EmptySuitEntity.ENTER_TURN)));
         return new Vec3(Mth.lerp(k, this.front.x, this.inside.x), this.start.y, Mth.lerp(k, this.front.z, this.inside.z));
      }

      float yaw(float t) {
         if (t <= EmptySuitEntity.ENTER_APPROACH) {
            // Face the suit while walking up (turn within the first few ticks).
            float k = ease(Math.min(1.0F, t / 6.0F));
            return this.startYaw + Mth.wrapDegrees(this.faceSuitYaw - this.startYaw) * k;
         }

         if (t <= EmptySuitEntity.ENTER_TURN) {
            float k = ease((t - EmptySuitEntity.ENTER_APPROACH) / (EmptySuitEntity.ENTER_TURN - EmptySuitEntity.ENTER_APPROACH));
            float base = this.startYaw + Mth.wrapDegrees(this.faceSuitYaw - this.startYaw);
            return base + 180.0F * k;
         }

         return this.startYaw + Mth.wrapDegrees(this.faceSuitYaw - this.startYaw) + 180.0F;
      }

      float pitch(float t) {
         return this.startPitch * (1.0F - ease(Math.min(1.0F, t / EmptySuitEntity.ENTER_APPROACH)));
      }

      private static float ease(float x) {
         x = Mth.clamp(x, 0.0F, 1.0F);
         return x * x * (3.0F - 2.0F * x);
      }
   }
}
