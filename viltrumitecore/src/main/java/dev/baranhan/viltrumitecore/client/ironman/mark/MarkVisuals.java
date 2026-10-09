package dev.baranhan.viltrumitecore.client.ironman.mark;

import dev.baranhan.viltrumitecore.client.anim.render.PlayerGeoLayer;
import dev.baranhan.viltrumitecore.client.ironman.veronica.PartWrapAnimator;
import dev.baranhan.viltrumitecore.entity.ViltrumiteEntities;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import dev.baranhan.viltrumitecore.hero.ironman.mark.EquipTimeline;
import dev.baranhan.viltrumitecore.hero.ironman.mark.SuitPart;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.EntityRenderersEvent;

/**
 * Client entry of the Stage 4 mark visuals (animation-system §7.1, §7.2):
 * the mark skin when the whole suit is on; otherwise Tony's skin with mark
 * plates on the parts that are on or wrapping; parts still flying are drawn
 * in the world by {@link PartFlightVisuals}. Pods, empty suits and debris use
 * their own whole-entity renderers.
 */
public final class MarkVisuals {
   private MarkVisuals() {
   }

   public static void init() {
   }

   public static void addLayers(PlayerRenderer renderer) {
   }

   public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
      event.registerEntityRenderer(ViltrumiteEntities.VERONICA_POD.get(), dev.baranhan.viltrumitecore.client.ironman.veronica.VeronicaPodRenderer::new);
      event.registerEntityRenderer(ViltrumiteEntities.EMPTY_SUIT.get(), EmptySuitRenderer::new);
      event.registerEntityRenderer(ViltrumiteEntities.SUIT_DEBRIS.get(), SuitDebrisRenderer::new);
   }

   /** Whole-suit skin: the mark (helmet open = Tony's head). Empty when Tony's skin or the plates show. */
   public static Optional<ResourceLocation> skin(AbstractClientPlayer player, HeroPublicSnapshot snapshot) {
      MarkState state = MarkState.of(snapshot);
      if (!state.full()) {
         return Optional.empty();
      }

      return Optional.of(MarkSkins.INSTANCE.skin(state.mark(), !state.helmetClosed()));
   }

   public static void collectParts(AbstractClientPlayer player, HeroPublicSnapshot snapshot, float partialTick, boolean firstPerson, List<PlayerGeoLayer.Part> out) {
      MarkState state = MarkState.of(snapshot);
      if (!state.markOn() || state.full()) {
         return;
      }

      List<SuitPart> parts = SuitPart.of(state.mark());
      int count = parts.size();
      float elapsed = state.elapsed() + partialTick;
      boolean delivery = state.delivery();
      ResourceLocation skin = MarkTextures.skin(state.mark());
      ResourceLocation glow = MarkTextures.glow(state.mark());
      List<PlayerGeoLayer.Pass> passes = List.of(PlayerGeoLayer.Pass.cutout(skin), PlayerGeoLayer.Pass.glow(glow));
      for (int i = 0; i < count; i++) {
         SuitPart part = parts.get(i);
         boolean helmet = part.bone() == SuitPart.Bone.HEAD;
         if (state.equipping() && EquipTimeline.phase(i, count, delivery, elapsed) == EquipTimeline.Phase.WRAPPING) {
            float progress = EquipTimeline.phaseProgress(i, count, delivery, elapsed);
            List<PlayerGeoLayer.Pass> wrapPasses = PartWrapAnimator.flash(progress) > 0.02F
               ? List.of(PlayerGeoLayer.Pass.cutout(skin), PlayerGeoLayer.Pass.glow(glow), PlayerGeoLayer.Pass.glow(skin))
               : passes;
            out.add(new PlayerGeoLayer.Part(PlateParts.geo(part), wrapPasses, geo -> PartWrapAnimator.pose(geo, progress)));
            continue;
         }

         if (!state.partPresent(i)) {
            continue;
         }

         if (helmet && !state.helmetClosed() && state.equipPhase() == 0) {
            continue;
         }

         out.add(new PlayerGeoLayer.Part(PlateParts.geo(part), passes));
      }
   }
}
