package dev.baranhan.viltrumitecore.client.ironman.mark.sig;

import dev.baranhan.viltrumitecore.client.anim.render.PlayerGeoLayer;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import java.util.List;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;

/** Client entry of the seven mark signatures (spec §13): parts, beams, camo, crosshairs. */
public final class SignatureVisuals {
   private SignatureVisuals() {
   }

   public static void init() {
   }

   public static void addLayers(PlayerRenderer renderer) {
   }

   public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
      event.registerEntityRenderer(dev.baranhan.viltrumitecore.entity.ViltrumiteEntities.ROCKET_FIST.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
   }

   public static void collectParts(AbstractClientPlayer player, HeroPublicSnapshot snapshot, float partialTick, boolean firstPerson, List<PlayerGeoLayer.Part> out) {
   }
}
