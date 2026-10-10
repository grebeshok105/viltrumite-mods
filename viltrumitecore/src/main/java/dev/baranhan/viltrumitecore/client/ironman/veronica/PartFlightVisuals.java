package dev.baranhan.viltrumitecore.client.ironman.veronica;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.baranhan.viltrumitecore.client.anim.AnimCache;
import dev.baranhan.viltrumitecore.client.anim.geo.BakedGeoModel;
import dev.baranhan.viltrumitecore.client.anim.render.PlayerGeoLayer;
import dev.baranhan.viltrumitecore.client.ironman.IronManView;
import dev.baranhan.viltrumitecore.client.ironman.mark.MarkState;
import dev.baranhan.viltrumitecore.client.ironman.mark.Mark42Parts;
import dev.baranhan.viltrumitecore.client.ironman.mark.MarkParts;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import dev.baranhan.viltrumitecore.hero.ironman.IronManMarkSounds;
import dev.baranhan.viltrumitecore.hero.ironman.mark.EquipTimeline;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkId;
import dev.baranhan.viltrumitecore.hero.ironman.mark.SuitPart;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.joml.Vector3f;

/**
 * Suit parts flying from Veronica to the player (spec §12.4). Each flying part
 * is the plate geo of its slice, drawn in the world on the moving player's
 * body frame; when it arrives, {@link dev.baranhan.viltrumitecore.client.ironman.mark.MarkVisuals}
 * takes it over on the body. The local player also gets the equip clicks.
 */
@EventBusSubscriber(
   modid = "viltrumitecore",
   bus = Bus.FORGE,
   value = {Dist.CLIENT}
)
public final class PartFlightVisuals {
   /** Player render scale: the geo body frame is drawn at this size. */
   public static final float BODY_SCALE = 0.9375F;
   private static int lastElapsed = -1;

   private PartFlightVisuals() {
   }

   /** Live body-frame anchor of a part on the player, in world space. */
   public static Vec3 anchor(Player player, Vec3 centre, float partialTick) {
      float yaw = bodyYaw(player, partialTick);
      Vector3f offset = new Vector3f((float)centre.x * BODY_SCALE, (float)centre.y * BODY_SCALE, (float)centre.z * BODY_SCALE);
      offset.rotateY((float)Math.toRadians(180.0F - yaw));
      return player.getPosition(partialTick).add(offset.x, offset.y, offset.z);
   }

   /** World position of a part that is flying now, or null when it is not in flight. */
   public static Vec3 flyingPosition(Player player, MarkState state, int index, float partialTick) {
      if (!state.equipping() || !state.delivery() || state.launch() == null || state.mark() == null) {
         return null;
      }

      List<SuitPart> parts = SuitPart.of(state.mark());
      int count = parts.size();
      if (index < 0 || index >= count) {
         return null;
      }

      float elapsed = state.elapsed() + partialTick;
      if (EquipTimeline.phase(index, count, true, elapsed) != EquipTimeline.Phase.FLYING) {
         return null;
      }

      BakedGeoModel geo = AnimCache.model(MarkParts.geo(state.mark(), parts.get(index)));
      if (geo == null) {
         return null;
      }

      Vec3 target = anchor(player, MarkParts.centre(geo), partialTick);
      return PartFlight.position(state.launch(), target, EquipTimeline.phaseProgress(index, count, true, elapsed));
   }

   static float bodyYaw(Player player, float partialTick) {
      return Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot);
   }

   @SubscribeEvent
   public static void onRenderLevel(RenderLevelStageEvent event) {
      if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
         return;
      }

      Minecraft client = Minecraft.getInstance();
      ClientLevel level = client.level;
      if (level == null) {
         return;
      }

      float partialTick = event.getPartialTick();
      Vec3 camera = event.getCamera().getPosition();
      PoseStack stack = event.getPoseStack();
      MultiBufferSource.BufferSource buffers = client.renderBuffers().bufferSource();
      for (Player player : level.players()) {
         HeroPublicSnapshot snapshot = IronManView.of(player);
         if (snapshot == null) {
            continue;
         }

         MarkState state = MarkState.of(snapshot);
         if (state.mark() == null || !state.equipping() || !state.delivery()) {
            continue;
         }

         List<SuitPart> parts = SuitPart.of(state.mark());
         for (int i = 0; i < parts.size(); i++) {
            Vec3 at = flyingPosition(player, state, i, partialTick);
            if (at == null) {
               continue;
            }

            BakedGeoModel geo = AnimCache.model(MarkParts.geo(state.mark(), parts.get(i)));
            if (geo == null) {
               continue;
            }

            float progress = EquipTimeline.phaseProgress(i, parts.size(), true, state.elapsed() + partialTick);
            float tumble = PartFlight.tumble(progress);
            Vec3 centre = MarkParts.centre(geo);
            stack.pushPose();
            stack.translate(at.x - camera.x, at.y - camera.y, at.z - camera.z);
            stack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw(player, partialTick)));
            stack.mulPose(Axis.XP.rotation(tumble * 1.4F));
            stack.mulPose(Axis.ZP.rotation(tumble * 1.1F));
            stack.scale(BODY_SCALE, BODY_SCALE, BODY_SCALE);
            stack.translate(-centre.x, -centre.y, -centre.z);
            geo.resetBones();
            MarkParts.draw(geo, stack, buffers, MarkParts.passes(state.mark(), parts.get(i)), LightTexture.FULL_BRIGHT, 1.0F);
            ResourceLocation fire = state.mark() == MarkId.MARK_42 ? Mark42Parts.fire(parts.get(i)) : null;
            BakedGeoModel flame = fire == null ? null : AnimCache.model(fire);
            if (flame != null) {
               flame.resetBones();
               // Sind repulsor_layer: an animated additive flame texture.
               List<PlayerGeoLayer.Pass> fireTex = List.of(PlayerGeoLayer.Pass.glow(Mark42Parts.fireTexture(player.tickCount / 4)));
               MarkParts.draw(flame, stack, buffers, fireTex, LightTexture.FULL_BRIGHT, 1.0F);
            }
            stack.popPose();
         }
      }

      buffers.endBatch();
   }

   @SubscribeEvent
   public static void onClientTick(TickEvent.ClientTickEvent event) {
      if (event.phase != TickEvent.Phase.END) {
         return;
      }

      Minecraft client = Minecraft.getInstance();
      LocalPlayer me = client.player;
      if (client.level == null || client.isPaused() || me == null) {
         return;
      }

      HeroPublicSnapshot snapshot = IronManView.of(me);
      MarkState state = snapshot == null ? null : MarkState.of(snapshot);
      if (state == null || !state.equipping() || state.mark() == null) {
         lastElapsed = -1;
         return;
      }

      int before = lastElapsed;
      int now = state.elapsed();
      lastElapsed = now;
      if (before < 0 || now < before) {
         return;
      }

      List<SuitPart> parts = SuitPart.of(state.mark());
      int count = parts.size();
      boolean delivery = state.delivery();
      for (int i = 0; i < count; i++) {
         int launch = EquipTimeline.launchTick(i, count, delivery);
         if (delivery && before < launch && launch <= now) {
            play(client, IronManMarkSounds.PART_FLY.get());
         }

         int lock = EquipTimeline.lockTick(i, count, delivery);
         if (before < lock && lock <= now) {
            play(client, parts.get(i).bone() == SuitPart.Bone.HEAD ? IronManMarkSounds.HELMET_LOCK.get() : IronManMarkSounds.PART_CLAMP.get());
         }
      }
   }

   @SubscribeEvent
   public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
      lastElapsed = -1;
   }

   private static void play(Minecraft client, net.minecraft.sounds.SoundEvent sound) {
      client.getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0F, 1.0F));
   }
}
