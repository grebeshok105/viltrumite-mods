package dev.baranhan.viltrumitecore.client.render.vfx;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import dev.baranhan.viltrumitecore.client.hero.ClientHeroData;
import dev.baranhan.viltrumitecore.client.regulus.RegulusClientFx;
import dev.baranhan.viltrumitecore.client.regulus.RegulusVfxMath;
import dev.baranhan.viltrumitecore.hero.HeroAction;
import dev.baranhan.viltrumitecore.hero.HeroId;
import dev.baranhan.viltrumitecore.hero.HeroPlayer;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import dev.baranhan.viltrumitecore.hero.control.ControlKind;
import dev.baranhan.viltrumitecore.hero.regulus.RegulusRules;
import dev.baranhan.viltrumitecore.hero.regulus.GreedsEmbrace;
import dev.baranhan.viltrumitecore.network.packet.HeroControlS2CPacket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

/** State-driven Regulus casts, control telegraphs and ritual/Madness feedback. */
@EventBusSubscriber(
   modid = "viltrumitecore",
   bus = Bus.FORGE,
   value = {Dist.CLIENT}
)
public class RegulusActionVFXManager {
   private static final List<RegulusActionVFXManager.FlashVFX> FLASHES = new ArrayList<>();
   private static final List<RegulusActionVFXManager.SparkVFX> SPARKS = new ArrayList<>();
   private static final Map<UUID, RegulusActionVFXManager.PrevState> PREV = new HashMap<>();
   private static ClientLevel lastLevel;
   private static int lastLocalHearts = -1;
   private static final int GOLD_R = 255;
   private static final int GOLD_G = 210;
   private static final int GOLD_B = 92;

   /** 4x4 rune glyphs (bitmask rows, 1 = lit pixel). */
   private static final int[] RUNES = {0x6996, 0xF926, 0x6F96, 0x9E79, 0x2697, 0x96F6, 0x79E9, 0xF6F9};

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent event) {
      if (event.phase != Phase.END) {
         return;
      }

      Minecraft client = Minecraft.getInstance();
      ClientLevel level = client.level;
      if (level != lastLevel) {
         lastLevel = level;
         FLASHES.clear();
         SPARKS.clear();
         PREV.clear();
         lastLocalHearts = -1;
         RegulusClientFx.reset();
      }
      if (level == null) {
         return;
      }

      RegulusClientFx.tickClient();

      for (Player player : level.players()) {
         if (!(player instanceof HeroPlayer heroPlayer)) {
            continue;
         }

         HeroPublicSnapshot snapshot = heroPlayer.getHeroSnapshot();
         if (snapshot == null || snapshot.heroId() != HeroId.REGULUS) {
            PREV.remove(player.getUUID());
            continue;
         }

         detectEdges(client, level, player, snapshot);
      }

      // Heart loss red flash is owner-local (spec 14: "у Regulus").
      LocalPlayer local = client.player;
      if (local instanceof HeroPlayer heroPlayer) {
         HeroPublicSnapshot snapshot = heroPlayer.getHeroSnapshot();
         if (snapshot != null && snapshot.heroId() == HeroId.REGULUS) {
            if (lastLocalHearts >= 0 && snapshot.hearts() < lastLocalHearts) {
               RegulusClientFx.heartFlashTicks = (int)RegulusVfxMath.HEART_FLASH_TICKS;
               for (int i = 0; i < 5; i++) {
                  Vec3 chest = local.position().add(0.0, 1.3, 0.0);
                  SPARKS.add(new SparkVFX(chest, ((float)RegulusVfxMath.hashOffset(i, 0) - 0.5F) * 0.8F, 0.9F + (float)RegulusVfxMath.hashOffset(i, 1) * 0.7F, ((float)RegulusVfxMath.hashOffset(i, 2) - 0.5F) * 0.8F));
               }
            }

            // Madness heartbeat: a lub-dub every HEARTBEAT_PERIOD ticks.
            if (snapshot.madness()) {
               long beat = level.getGameTime() / RegulusVfxMath.HEARTBEAT_PERIOD_TICKS;
               if (beat != RegulusClientFx.lastHeartbeatTick) {
                  RegulusClientFx.lastHeartbeatTick = beat;
                  local.playSound(SoundEvents.WARDEN_HEARTBEAT, 0.85F, 1.0F);
               }
            }

            lastLocalHearts = snapshot.hearts();
         } else {
            lastLocalHearts = -1;
            RegulusClientFx.reset();
         }
      }

      FLASHES.removeIf(vfx -> vfx.age++ > 10);
      SPARKS.removeIf(vfx -> {
         vfx.age++;
         vfx.pos = vfx.pos.add(vfx.velX * 0.06, vfx.velY * 0.06, vfx.velZ * 0.06);
         vfx.velY *= 0.92F;
         return vfx.age > 14;
      });
   }

   private static void detectEdges(Minecraft client, ClientLevel level, Player player, HeroPublicSnapshot snapshot) {
      PrevState prev = PREV.computeIfAbsent(player.getUUID(), k -> new PrevState());
      HeroAction action = HeroAction.byId(snapshot.actionId());
      HeroAction prevAction = HeroAction.byId(prev.actionId);
      int elapsed = snapshot.actionElapsed();

      if (action == HeroAction.DEBRIS_KICK && player == client.player
         && (prevAction != action || prev.elapsed < RegulusRules.DEBRIS_EVENT_TICK)
         && elapsed >= RegulusRules.DEBRIS_EVENT_TICK) {
         float impact = RegulusVfxMath.debrisImpactEnvelope(elapsed);
         RegulusClientFx.debrisShakeTicks = 10;
         RegulusClientFx.debrisShakePower = impact * 0.25F;
      }

      if (action == HeroAction.COUNTER) {
         if ((prevAction != action || prev.elapsed < RegulusRules.COUNTER_LIFT_TICKS) && elapsed >= RegulusRules.COUNTER_LIFT_TICKS) {
            FLASHES.add(new FlashVFX(player.position().add(0.0, 1.2, 0.0)));
         }

         int slamTick = RegulusRules.COUNTER_LIFT_TICKS + RegulusRules.COUNTER_SLAM_TICKS;
         if ((prevAction != action || prev.elapsed < slamTick) && elapsed >= slamTick) {
            Vec3 impact = snapshot.actionTarget() != null ? snapshot.actionTarget() : player.position();
            for (int i = 0; i < 10; i++) {
               double az = Math.PI * 2.0 * (double)i / 10.0;
               SPARKS.add(new SparkVFX(impact.add(Math.cos(az) * 1.2, 0.1, Math.sin(az) * 1.2), (float)(Math.cos(az) * 0.5), 1.4F, (float)(Math.sin(az) * 0.5)));
            }
         }
      }

      // Landing fragments follow the existing radial knockback.
      if (prev.fallDistance >= RegulusRules.SHOCKWAVE_MIN_FALL && player.onGround() && !prev.onGround) {
         for (int i = 0; i < 12; i++) {
            double az = Math.PI * 2.0 * (double)i / 12.0;
            SPARKS.add(new SparkVFX(player.position().add(Math.cos(az) * 0.8, 0.1, Math.sin(az) * 0.8), (float)(Math.cos(az) * 0.9), 0.8F + (float)RegulusVfxMath.hashOffset(i, 0), (float)(Math.sin(az) * 0.9)));
         }
      }

      prev.actionId = snapshot.actionId();
      prev.elapsed = elapsed;
      prev.fallDistance = player.fallDistance;
      prev.onGround = player.onGround();
   }

   @SubscribeEvent
   public static void onRenderLevel(RenderLevelStageEvent event) {
      if (event.getStage() != Stage.AFTER_LEVEL) {
         return;
      }

      Minecraft client = Minecraft.getInstance();
      ClientLevel level = client.level;
      if (level == null) {
         return;
      }

      float partialTick = event.getPartialTick();
      Camera camera = event.getCamera();
      Vec3 cameraPos = camera.getPosition();
      long gameTime = level.getGameTime();
      float timeSeconds = ((gameTime % 24000L) + partialTick) / 20.0F;
      PoseStack modelViewStack = RenderSystem.getModelViewStack();
      modelViewStack.pushPose();
      modelViewStack.setIdentity();
      RegulusPixelVfx.rotateCamera(modelViewStack, camera.getXRot(), camera.getYRot());
      RenderSystem.applyModelViewMatrix();
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
      RenderSystem.disableCull();
      RenderSystem.enableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      Tesselator tessellator = Tesselator.getInstance();
      BufferBuilder buffer = tessellator.getBuilder();
      try {
         buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

         for (Player player : level.players()) {
            if (!(player instanceof HeroPlayer heroPlayer)) {
               continue;
            }

            HeroPublicSnapshot snapshot = heroPlayer.getHeroSnapshot();
            if (snapshot == null || snapshot.heroId() != HeroId.REGULUS) {
               continue;
            }

            drawEmbracePreview(buffer, cameraPos, camera, player, snapshot, timeSeconds);
            drawManiaTether(level, buffer, cameraPos, camera, player, snapshot, partialTick);
            drawRitualRunes(buffer, cameraPos, camera, player, snapshot, partialTick, timeSeconds);
            drawMadnessSymbols(buffer, cameraPos, camera, player, snapshot, partialTick, timeSeconds);
         }

         drawDomes(buffer, cameraPos, camera, gameTime, timeSeconds);
         drawControlOutlines(level, buffer, cameraPos, camera, partialTick);
         drawActiveEffects(buffer, cameraPos, camera, partialTick);

         tessellator.end();
      } finally {
         RenderSystem.depthMask(true);
         RenderSystem.enableCull();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableBlend();
         modelViewStack.popPose();
         RenderSystem.applyModelViewMatrix();
      }
   }

   private static void drawEmbracePreview(BufferBuilder buffer, Vec3 cameraPos, Camera camera, Player player,
      HeroPublicSnapshot snapshot, float timeSeconds) {
      boolean casting = HeroAction.byId(snapshot.actionId()) == HeroAction.GREEDS_EMBRACE;
      if (casting && snapshot.actionElapsed() >= RegulusRules.EMBRACE_APPEAR_TICK) {
         return;
      }
      // The dome telegraph exists only while V is actually being cast — never
      // as an idle aim preview whenever the ability is off cooldown.
      if (!casting) {
         return;
      }
      Vec3 point = casting ? snapshot.actionTarget() : null;
      if (point == null) {
         point = GreedsEmbrace.aimPoint(player);
      }
      if (point != null) {
         RegulusPixelVfx.domeShell(buffer, cameraPos, camera, point, (float)RegulusRules.EMBRACE_RADIUS,
            timeSeconds, GOLD_R, GOLD_G, GOLD_B, 75);
      }
   }

   /** Gold tether from the channeling hand to the grabbed target. */
   private static void drawManiaTether(ClientLevel level, BufferBuilder buffer, Vec3 cameraPos, Camera camera, Player player, HeroPublicSnapshot snapshot, float partialTick) {
      if (snapshot.controlTargetId() < 0) {
         return;
      }

      Entity target = level.getEntity(snapshot.controlTargetId());
      if (target == null) {
         return;
      }

      Vec3 eyePos = player.getEyePosition(partialTick);
      Vec3 look = player.getViewVector(partialTick);
      double handedness = player.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT ? -1.0 : 1.0;
      Vec3 side = new Vec3(-look.z, 0.0, look.x).normalize().scale(handedness);
      Vec3 hand = eyePos.add(look.scale(0.45)).add(side.scale(0.28)).add(0.0, -0.28, 0.0);
      Vec3 targetPos = target.getPosition(partialTick).add(0.0, target.getBbHeight() * 0.55, 0.0);
      float shimmer = 0.75F + 0.25F * (float)Math.sin((double)((level.getGameTime() % 24000L) + partialTick) * 6.0);
      RegulusPixelVfx.beamDots(buffer, cameraPos, camera, hand, targetPos, 0.42F, 0.055F, GOLD_R, GOLD_G, GOLD_B, (int)(200.0F * shimmer));
   }

   /** Ritual: runes orbit the caster, the held book glows faintly gold. */
   private static void drawRitualRunes(BufferBuilder buffer, Vec3 cameraPos, Camera camera, Player player, HeroPublicSnapshot snapshot, float partialTick, float timeSeconds) {
      if (snapshot.ritualTicks() < 0) {
         return;
      }

      float progress = (float)snapshot.ritualTicks() / (float)RegulusRules.RITUAL_TICKS;
      int alpha = 140 + (int)(80.0F * progress);
      Vec3 center = player.getPosition(partialTick).add(0.0, 1.25, 0.0);
      for (int i = 0; i < (localFirstPerson(player) ? 0 : 8); i++) {
         float angle = RegulusVfxMath.runeAngle(i, timeSeconds);
         Vec3 pos = center.add(Math.cos((double)angle) * 1.3, Math.sin((double)(angle * 2.0F)) * 0.18, Math.sin((double)angle) * 1.3);
         drawGlyph(buffer, cameraPos, camera, pos, RUNES[i], 0.055F, GOLD_R, GOLD_G, GOLD_B, alpha);
      }

      Vec3 look = player.getViewVector(partialTick);
      Vec3 book = player.getEyePosition(partialTick).add(look.scale(0.45)).add(0.0, -0.32, 0.0);
      for (int i = 0; i < 4; i++) {
         float angle = timeSeconds * 2.2F + i * (float)(Math.PI / 2.0);
         Vec3 mote = book.add(Math.cos((double)angle) * 0.22, Math.sin((double)angle * 1.4) * 0.1, Math.sin((double)angle) * 0.22);
         RegulusPixelVfx.billboardPixel(buffer, cameraPos, camera, mote, 0.04F, 255, 235, 160, 170);
      }
   }

   /** Madness: dark-red runes drift slowly around the mad Regulus. */
   private static void drawMadnessSymbols(BufferBuilder buffer, Vec3 cameraPos, Camera camera, Player player, HeroPublicSnapshot snapshot, float partialTick, float timeSeconds) {
      if (!snapshot.madness() || localFirstPerson(player)) {
         return;
      }

      Vec3 center = player.getPosition(partialTick).add(0.0, 1.1, 0.0);
      for (int i = 0; i < 6; i++) {
         float angle = RegulusVfxMath.runeAngle(i, -timeSeconds * 0.55F);
         float bob = (float)Math.sin((double)(timeSeconds * 0.9F + i * 1.1F)) * 0.3F;
         Vec3 pos = center.add(Math.cos((double)angle) * 1.8, 0.3 + bob, Math.sin((double)angle) * 1.8);
         drawGlyph(buffer, cameraPos, camera, pos, RUNES[i + 2], 0.07F, 150, 18, 18, 130);
      }
   }

   private static void drawGlyph(BufferBuilder buffer, Vec3 cameraPos, Camera camera, Vec3 pos, int glyph, float cell, int r, int g, int b, int alpha) {
      org.joml.Vector3f left = camera.getLeftVector();
      org.joml.Vector3f up = camera.getUpVector();
      for (int row = 0; row < 4; row++) {
         for (int col = 0; col < 4; col++) {
            if ((glyph >> (row * 4 + col) & 1) == 0) {
               continue;
            }

            float dx = (col - 1.5F) * cell;
            float dy = (1.5F - row) * cell;
            Vec3 pixelPos = pos.add(left.x * dx + up.x * dy, left.y * dx + up.y * dy, left.z * dx + up.z * dy);
            RegulusPixelVfx.billboardPixel(buffer, cameraPos, camera, pixelPos, cell * 0.6F, r, g, b, alpha);
         }
      }
   }

   /** Embrace domes: translucent shell + suspended motes. */
   private static void drawDomes(BufferBuilder buffer, Vec3 cameraPos, Camera camera, long gameTime, float timeSeconds) {
      for (HeroControlS2CPacket.DomeInfo dome : ClientHeroData.domes()) {
         Vec3 center = new Vec3(dome.x(), dome.y(), dome.z());
         int alpha = (int)(120.0F * RegulusVfxMath.domeAlpha(gameTime, dome.createdAt(), dome.expiresAt()));
         if (alpha <= 0) {
            continue;
         }

         RegulusPixelVfx.domeShell(buffer, cameraPos, camera, center, (float)dome.radius(), timeSeconds, GOLD_R, GOLD_G, GOLD_B, alpha);
         int seedBase = dome.id().hashCode() & 0x7FFF;
         for (int i = 0; i < 30; i++) {
            double theta = RegulusVfxMath.hashOffset(seedBase + i, 0) * Math.PI * 2.0;
            double y = RegulusVfxMath.hashOffset(seedBase + i, 1) * dome.radius() * 0.85;
            double radius = RegulusVfxMath.hashOffset(seedBase + i, 2) * dome.radius() * 0.8;
            Vec3 pos = center.add(Math.cos(theta) * radius, y, Math.sin(theta) * radius);
            RegulusPixelVfx.billboardPixel(buffer, cameraPos, camera, pos, 0.035F, 255, 225, 140, (int)(alpha * 0.7F));
         }
      }
   }

   /** Frozen/stasis targets get a gold pixel outline (spec 14). */
   private static void drawControlOutlines(ClientLevel level, BufferBuilder buffer, Vec3 cameraPos, Camera camera, float partialTick) {
      for (HeroControlS2CPacket.ControlInfo control : ClientHeroData.controls()) {
         int ordinal = control.kindOrdinal();
         ControlKind kind = ordinal >= 0 && ordinal < ControlKind.values().length ? ControlKind.values()[ordinal] : null;
         if (kind != ControlKind.FREEZE && kind != ControlKind.STASIS) {
            continue;
         }

         Entity target = level.getEntity(control.entityId());
         if (target != null) {
            float pulse = 0.7F + 0.3F * (float)Math.sin((double)((level.getGameTime() % 24000L) + partialTick) * 4.0);
            RegulusPixelVfx.boxOutline(buffer, cameraPos, camera, target.getBoundingBox().move(target.getPosition(partialTick).subtract(target.position())), 0.025F, GOLD_R, GOLD_G, GOLD_B, (int)(170.0F * pulse));
         }
      }
   }

   private static void drawActiveEffects(BufferBuilder buffer, Vec3 cameraPos, Camera camera, float partialTick) {
      for (RegulusActionVFXManager.FlashVFX flash : FLASHES) {
         float progress = (flash.age + partialTick) / 10.0F;
         int alpha = (int)(255.0F * (1.0F - progress));
         if (alpha > 0) {
            float size = 0.3F + progress * 1.1F;
            RegulusPixelVfx.crossGlow(buffer, cameraPos, camera, flash.pos, size, 255, 255, 255, alpha);
            // Speed lines: vertical streaks racing down past the lifted player.
            for (int i = 0; i < 6; i++) {
               double az = Math.PI * 2.0 * (double)i / 6.0;
               Vec3 linePos = flash.pos.add(Math.cos(az) * 0.8, 1.8 - progress * 2.6, Math.sin(az) * 0.8);
               RegulusPixelVfx.billboardPixel(buffer, cameraPos, camera, linePos, 0.05F, 255, 255, 255, alpha / 2);
            }
         }
      }

      for (RegulusActionVFXManager.SparkVFX spark : SPARKS) {
         float progress = (spark.age + partialTick) / 14.0F;
         int alpha = (int)(240.0F * (1.0F - progress));
         if (alpha > 0) {
            RegulusPixelVfx.billboardPixel(buffer, cameraPos, camera, spark.pos, 0.06F, 200, 30, 24, alpha);
         }
      }
   }

   private static boolean localFirstPerson(Player player) {
      Minecraft client = Minecraft.getInstance();
      return player == client.player && client.options.getCameraType().isFirstPerson();
   }

   private static class PrevState {
      int actionId = -1;
      int elapsed;
      float fallDistance;
      boolean onGround = true;
   }

   private static class FlashVFX {
      final Vec3 pos;
      int age;

      FlashVFX(Vec3 pos) {
         this.pos = pos;
      }
   }

   private static class SparkVFX {
      Vec3 pos;
      float velX;
      float velY;
      float velZ;
      int age;

      SparkVFX(Vec3 pos, float velX, float velY, float velZ) {
         this.pos = pos;
         this.velX = velX;
         this.velY = velY;
         this.velZ = velZ;
      }
   }
}
