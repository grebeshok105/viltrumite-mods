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
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.ClipContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

/** Lion's frozen mote boundary and owner-private body-center carrier glow. */
@EventBusSubscriber(
   modid = "viltrumitecore",
   bus = Bus.FORGE,
   value = {Dist.CLIENT}
)
public class RegulusLionVFXManager {
   private static final List<RegulusLionVFXManager.ChestPulse> PULSES = new ArrayList<>();
   private static final Map<UUID, Boolean> PREV_LION = new HashMap<>();
   private static ClientLevel lastLevel;

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent event) {
      if (event.phase != Phase.END) {
         return;
      }

      Minecraft client = Minecraft.getInstance();
      if (client.level != lastLevel) {
         lastLevel = client.level;
         PREV_LION.clear();
         PULSES.clear();
      }
      if (client.level == null) {
         return;
      }

      for (Player player : client.level.players()) {
         if (!(player instanceof HeroPlayer heroPlayer)) {
            continue;
         }

         HeroPublicSnapshot snapshot = heroPlayer.getHeroSnapshot();
         if (snapshot == null || snapshot.heroId() != HeroId.REGULUS) {
            PREV_LION.remove(player.getUUID());
            continue;
         }

         boolean wasActive = PREV_LION.getOrDefault(player.getUUID(), false);
         if (wasActive != snapshot.lionActive()) {
            PULSES.add(new ChestPulse(player.getUUID(), snapshot.lionActive()));
         }

         PREV_LION.put(player.getUUID(), snapshot.lionActive());
      }

      PULSES.removeIf(burst -> {
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

      if (activeLions.isEmpty() && PULSES.isEmpty() && ClientHeroData.carriers().length == 0) {
         return;
      }

      float partialTick = event.getPartialTick();
      Camera camera = event.getCamera();
      Vec3 cameraPos = camera.getPosition();
      float timeSeconds = ((level.getGameTime() % 24000L) + partialTick) / 20.0F;
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

         for (Player lion : activeLions) {
            Vec3 center = lion.getPosition(partialTick).add(0.0, lion.getBbHeight() * 0.55, 0.0);
            if (!localFirstPerson(lion)) {
               HeroPublicSnapshot snapshot = ((HeroPlayer)lion).getHeroSnapshot();
               int green = snapshot.lionOverheat() ? 110 : 240;
               int blue = snapshot.lionOverheat() ? 90 : 230;
               RegulusPixelVfx.sphereShell(buffer, cameraPos, camera, center, 4.0F, timeSeconds, 250, green, blue, 65);
               drawSuspendedMotes(buffer, cameraPos, camera, center, lion.getUUID());
               RegulusPixelVfx.billboardPixel(buffer, cameraPos, camera, center, 0.08F, 255, green, blue, 180);
            }

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

         for (ChestPulse pulse : PULSES) {
            Player owner = level.getPlayerByUUID(pulse.ownerId);
            if (owner == null || localFirstPerson(owner)) {
               continue;
            }
            float progress = (pulse.age + partialTick) / 14.0F;
            int alpha = (int)(180.0F * (1.0F - progress));
            if (alpha > 0) {
               Vec3 chest = owner.getPosition(partialTick).add(0.0, owner.getBbHeight() * 0.55, 0.0);
               RegulusPixelVfx.billboardPixel(buffer, cameraPos, camera, chest, 0.12F - progress * 0.07F,
                  pulse.activated ? 255 : 180, pulse.activated ? 235 : 185, pulse.activated ? 150 : 190, alpha);
            }
         }

         tessellator.end();
         // Body-center markers bypass the entity skin, but never the owner's block LOS.
         RenderSystem.disableDepthTest();
         buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
         drawCarriers(client, level, buffer, cameraPos, camera, partialTick, timeSeconds);
         tessellator.end();
         RenderSystem.enableDepthTest();
      } finally {
         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(true);
         RenderSystem.enableCull();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableBlend();
         modelViewStack.popPose();
         RenderSystem.applyModelViewMatrix();
      }
   }

   private static void drawCarriers(Minecraft client, ClientLevel level, BufferBuilder buffer, Vec3 cameraPos, Camera camera, float partialTick, float timeSeconds) {
      if (!(client.player instanceof HeroPlayer heroPlayer)) {
         return;
      }
      HeroPublicSnapshot snapshot = heroPlayer.getHeroSnapshot();
      if (snapshot == null || snapshot.heroId() != HeroId.REGULUS) {
         return;
      }
      for (int id : ClientHeroData.carriers()) {
         Entity carrier = level.getEntity(id);
         if (carrier == null || !carrier.isAlive()) {
            continue;
         }
         Vec3 chest = carrier.getPosition(partialTick).add(0.0, carrier.getBbHeight() * 0.55, 0.0);
         if (level.clip(new ClipContext(cameraPos, chest, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, client.player)).getType() != HitResult.Type.MISS) {
            continue;
         }
         float pulse = 0.5F + 0.5F * (float)Math.sin(timeSeconds * 2.4F);
         RegulusPixelVfx.billboardPixel(buffer, cameraPos, camera, chest, 0.055F, 180, 20, 15, 35 + (int)(25 * pulse));
         RegulusPixelVfx.billboardPixel(buffer, cameraPos, camera, chest, 0.027F, 245, 35, 25, 100 + (int)(60 * pulse));
      }
   }

   private static boolean localFirstPerson(Player player) {
      Minecraft client = Minecraft.getInstance();
      return player == client.player && client.options.getCameraType().isFirstPerson();
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

   private static class ChestPulse {
      final UUID ownerId;
      final boolean activated;
      int age;

      ChestPulse(UUID ownerId, boolean activated) {
         this.ownerId = ownerId;
         this.activated = activated;
      }
   }
}
