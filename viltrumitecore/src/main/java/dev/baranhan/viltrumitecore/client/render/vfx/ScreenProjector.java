package dev.baranhan.viltrumitecore.client.render.vfx;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * World → GUI coordinates for HUD hints (brackets, edge arrows). Captures the
 * camera pose and projection of the last rendered level frame; the GUI pass
 * of the same frame projects with them.
 */
@EventBusSubscriber(
   modid = "viltrumitecore",
   bus = Bus.FORGE,
   value = {Dist.CLIENT}
)
public final class ScreenProjector {
   private static final Matrix4f VIEW_PROJ = new Matrix4f();
   private static Vec3 camera = Vec3.ZERO;
   private static boolean valid;

   private ScreenProjector() {
   }

   /** GUI position; behind = the point is behind the camera (x/y keep its side for edge arrows). */
   public record Point(float x, float y, boolean behind) {
      public boolean onScreen(int width, int height) {
         return !this.behind && this.x >= 0 && this.y >= 0 && this.x <= width && this.y <= height;
      }
   }

   @SubscribeEvent
   public static void onRenderLevel(RenderLevelStageEvent event) {
      if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
         return;
      }

      VIEW_PROJ.set(event.getProjectionMatrix()).mul(event.getPoseStack().last().pose());
      camera = event.getCamera().getPosition();
      valid = true;
   }

   @Nullable
   public static Point project(Vec3 world, int guiWidth, int guiHeight) {
      if (!valid || Minecraft.getInstance().level == null) {
         return null;
      }

      Vector4f v = new Vector4f((float)(world.x - camera.x), (float)(world.y - camera.y), (float)(world.z - camera.z), 1.0F);
      VIEW_PROJ.transform(v);
      boolean behind = v.w <= 1.0E-3F;
      float w = Math.abs(v.w) < 1.0E-3F ? 1.0E-3F : Math.abs(v.w);
      // Dividing by |w| keeps the side of a point behind the camera (signed w would mirror it).
      float nx = v.x / w;
      float ny = v.y / w;
      return new Point((nx * 0.5F + 0.5F) * guiWidth, (1.0F - (ny * 0.5F + 0.5F)) * guiHeight, behind);
   }
}
