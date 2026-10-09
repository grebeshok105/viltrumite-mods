package dev.baranhan.viltrumitecore.client.ironman.mark;

import dev.baranhan.viltrumitecore.client.anim.render.PlayerGeoLayer;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.EntityRenderersEvent;

/**
 * Client entry of the Stage 4 mark visuals: mark skins on the player, parts
 * flying from Veronica and wrapping the limbs, exit, empty suit, debris, pod.
 */
public final class MarkVisuals {
   private MarkVisuals() {
   }

   public static void init() {
   }

   public static void addLayers(PlayerRenderer renderer) {
   }

   public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
      event.registerEntityRenderer(dev.baranhan.viltrumitecore.entity.ViltrumiteEntities.VERONICA_POD.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
      event.registerEntityRenderer(dev.baranhan.viltrumitecore.entity.ViltrumiteEntities.EMPTY_SUIT.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
      event.registerEntityRenderer(dev.baranhan.viltrumitecore.entity.ViltrumiteEntities.SUIT_DEBRIS.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
   }

   /** Player skin while a mark is (partly) on, or empty for the nano / Tony path. */
   public static Optional<ResourceLocation> skin(AbstractClientPlayer player, HeroPublicSnapshot snapshot) {
      return Optional.empty();
   }

   public static void collectParts(AbstractClientPlayer player, HeroPublicSnapshot snapshot, float partialTick, boolean firstPerson, List<PlayerGeoLayer.Part> out) {
   }
}
