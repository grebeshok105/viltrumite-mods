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
import dev.baranhan.viltrumitecore.client.regulus.RegulusVfxMath;
import dev.baranhan.viltrumitecore.hero.HeroId;
import dev.baranhan.viltrumitecore.hero.HeroPlayer;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

/**
 * Lion's Heart world VFX (spec 14): the distortion dome around an active
 * Regulus, suspended dust motes inside it, a white highlight on frozen
 * projectiles, the owner-only gold pulse on heart carriers, and a small repulse
 * burst when the heart releases.
 */
@EventBusSubscriber(
   modid = "viltrumitecore",
   bus = Bus.FORGE,
   value = {Dist.CLIENT}
)
public class RegulusLionVFXManager {
   private static final List<RegulusLionVFXManager.BurstVFX> BURSTS = new ArrayList<>();
   private static final Map<UUID, Boolean> PREV_LION = new HashMap<>();
   private static final int GOLD_R = 255;
   private static final int GOLD_G = 210;
   private static final int GOLD_B = 92;

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent event) {
      if (event.phase != Phase.END) {
         return;
      }

      Minecraft client = Minecraft.getInstance();
      if (client.level == null) {
         PREV_LION.clear();
         BURSTS.clear();
         return;
      }

      for (Player player : client.level.players()) {
         if (!(player instanceof HeroPlayer heroPlayer)) {
            continue;
         }

         HeroPublicSnapshot snapshot = heroPlayer.getHeroSnapshot();
         if (snapshot == null || snapshot.heroId() != HeroId.REGULUS) {
            continue;
         }

         boolean wasActive = PREV_LION.getOrDefault(player.getUUID(), false);
         if (wasActive && !snapshot.lionActive()) {
            BURSTS.add(new RegulusLionVFXManager.BurstVFX(player.position().add(0.0, 0.9, 0.0), 4.0F));
         }

         PREV_LION.put(player.getUUID(), snapshot.lionActive());
      }

      BURSTS.removeIf(burst -> {
         burst.age++;
         return burst.age > 14;
      });
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

      List<Player> activeLions = new ArrayList<>();
      for (Player player : level.players()) {
         if (player instanceof HeroPlayer heroPlayer) {
            HeroPublicSnapshot snapshot = heroPlayer.getHeroSnapshot();
            if (snapshot != null && snapshot.heroId() == HeroId.REGULUS && snapshot.lionActive()) {
               activeLions.add(player);
            }
         }
      }

      if (activeLions.isEmpty() && BURSTS.isEmpty() && ClientHeroData.carriers().length == 0) {
         return;
      }

      float partialTick = event.getPartialTick();
      Camera camera = event.getCamera();
      Vec3 cameraPos = camera.getPosition();
      float timeSeconds = (level.getGameTime() % 24000L) + partialTick;
      PoseStack modelViewStack = RenderSystem.getModelViewStack();
      modelViewStack.pushPose();
      modelViewStack.setIdentity();
      RenderSystem.applyModelViewMatrix();
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
      RenderSystem.disableCull();
      RenderSystem.enableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      Tesselator tessellator = Tesselator.getInstance();
      BufferBuilder buffer = tessellator.getBuilder();
      buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

      for (Player lion : activeLions) {
         Vec3 center = lion.getPosition(partialTick).add(0.0, 0.9, 0.0);
         // Distortion dome + frozen motes (spec 14).
         RegulusPixelVfx.domeShell(buffer, cameraPos, center, 4.0F, timeSeconds, 235, 240, 255, 95);
         drawSuspendedMotes(buffer, cameraPos, camera, center, lion.getUUID());

         // Frozen projectiles inside the aura get a white highlight.
         for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof Projectile
               && entity.isNoGravity()
               && entity.getDeltaMovement().lengthSqr() < 0.01
               && entity.distanceToSqr(lion) <= 16.0 + 4.0) {
               RegulusPixelVfx.crossGlow(buffer, cameraPos, camera, entity.getPosition(partialTick).add(0.0, entity.getBbHeight() * 0.5, 0.0), 0.16F, 250, 252, 255, 210);
            }
         }
      }

      // Heart carriers: owner-only gold pulse (carrier ids are owner-private).
      for (int carrierId : ClientHeroData.carriers()) {
         Entity carrier = level.getEntity(carrierId);
         if (carrier != null) {
            float pulse = 0.5F + 0.5F * (float)Math.sin((double)(timeSeconds * 2.4F));
            float ringRadius = 0.7F + 0.25F * pulse;
            Vec3 carrierPos = carrier.getPosition(partialTick);
            RegulusPixelVfx.groundRing(buffer, cameraPos, carrierPos, ringRadius, 0.06F, GOLD_R, GOLD_G, GOLD_B, 80 + (int)(90.0F * pulse));
            RegulusPixelVfx.billboardPixel(buffer, cameraPos, camera, carrierPos.add(0.0, carrier.getBbHeight() + 0.25 + pulse * 0.15, 0.0), 0.07F, GOLD_R, GOLD_G, GOLD_B, 110);
         }
      }

      for (RegulusLionVFXManager.BurstVFX burst : BURSTS) {
         float progress = (burst.age + partialTick) / 14.0F;
         int alpha = (int)(220.0F * (1.0F - progress));
         if (alpha > 0) {
            RegulusPixelVfx.expandingRing(buffer, cameraPos, burst.origin, progress, burst.maxRadius, 240, 245, 255, alpha);
         }
      }

      tessellator.end();
      RenderSystem.depthMask(true);
      RenderSystem.enableCull();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
      modelViewStack.popPose();
      RenderSystem.applyModelViewMatrix();
   }

   /** Dust motes hang frozen inside the dome — static hash positions, no drift. */
   private static void drawSuspendedMotes(BufferBuilder buffer, Vec3 cameraPos, Camera camera, Vec3 center, UUID ownerId) {
      int seedBase = ownerId.hashCode() & 0x7FFF;
      for (int i = 0; i < 48; i++) {
         double theta = RegulusVfxMath.hashOffset(seedBase + i, 0) * Math.PI * 2.0;
         double phi = (RegulusVfxMath.hashOffset(seedBase + i, 1) - 0.5) * Math.PI;
         double radius = RegulusVfxMath.hashOffset(seedBase + i, 2) * 3.8;
         Vec3 pos = center.add(Math.cos(theta) * Math.cos(phi) * radius, Math.sin(phi) * radius, Math.sin(theta) * Math.cos(phi) * radius);
         float twinkle = 0.4F + 0.6F * (float)Math.abs(Math.sin((double)(i * 1.3F) + center.x));
         RegulusPixelVfx.billboardPixel(buffer, cameraPos, camera, pos, 0.045F, 235, 240, 255, (int)(70.0F * twinkle));
      }
   }

   private static class BurstVFX {
      final Vec3 origin;
      final float maxRadius;
      int age;

      BurstVFX(Vec3 origin, float maxRadius) {
         this.origin = origin;
         this.maxRadius = maxRadius;
      }
   }
}
