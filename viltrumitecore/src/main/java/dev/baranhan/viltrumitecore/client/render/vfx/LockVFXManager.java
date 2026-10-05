package dev.baranhan.viltrumitecore.client.render.vfx;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Axis;
import dev.baranhan.viltrumitecore.client.TargetLockManager;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.joml.Matrix4f;

@EventBusSubscriber(
   modid = "viltrumitecore",
   bus = Bus.FORGE,
   value = {Dist.CLIENT}
)
public class LockVFXManager {
   @SubscribeEvent
   public static void onRenderLevel(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_PARTICLES) {
         LivingEntity target = TargetLockManager.lockedTarget;
         if (target != null && target.isAlive()) {
            Camera camera = event.getCamera();
            PoseStack matrices = new PoseStack();
            matrices.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
            matrices.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));
            double x = Mth.lerp((double)event.getPartialTick(), target.xo, target.getX()) - camera.getPosition().x;
            double y = Mth.lerp((double)event.getPartialTick(), target.yo, target.getY())
               - camera.getPosition().y
               + (double)target.getBbHeight() * 0.6;
            double z = Mth.lerp((double)event.getPartialTick(), target.zo, target.getZ()) - camera.getPosition().z;
            matrices.translate(x, y, z);
            matrices.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
            matrices.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
            long time = System.currentTimeMillis();
            float pulse = 1.0F + 0.15F * (float)Math.sin((double)((float)(time % 800L) / 800.0F) * Math.PI * 2.0);
            float rotate = (float)(time % 3000L) / 3000.0F * 360.0F;
            matrices.scale(pulse, pulse, pulse);
            matrices.mulPose(Axis.ZP.rotationDegrees(rotate));
            PoseStack modelViewStack = RenderSystem.getModelViewStack();
            modelViewStack.pushPose();
            modelViewStack.setIdentity();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder buffer = tesselator.getBuilder();
            buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            Matrix4f matrix = matrices.last().pose();
            float size = 0.4F;
            float thickness = 0.05F;
            int r = 255;
            int g = 20;
            int b = 20;
            int a = 180;
            buffer.vertex(matrix, -size, -thickness, 0.0F).color(r, g, b, a).endVertex();
            buffer.vertex(matrix, size, -thickness, 0.0F).color(r, g, b, a).endVertex();
            buffer.vertex(matrix, size, thickness, 0.0F).color(r, g, b, a).endVertex();
            buffer.vertex(matrix, -size, thickness, 0.0F).color(r, g, b, a).endVertex();
            buffer.vertex(matrix, -thickness, -size, 0.0F).color(r, g, b, a).endVertex();
            buffer.vertex(matrix, thickness, -size, 0.0F).color(r, g, b, a).endVertex();
            buffer.vertex(matrix, thickness, size, 0.0F).color(r, g, b, a).endVertex();
            buffer.vertex(matrix, -thickness, size, 0.0F).color(r, g, b, a).endVertex();
            tesselator.end();
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            modelViewStack.popPose();
            RenderSystem.applyModelViewMatrix();
         }
      }
   }
}
