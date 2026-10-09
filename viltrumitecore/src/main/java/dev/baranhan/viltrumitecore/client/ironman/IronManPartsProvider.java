package dev.baranhan.viltrumitecore.client.ironman;

import dev.baranhan.viltrumitecore.client.anim.render.PlayerGeoLayer;
import dev.baranhan.viltrumitecore.client.ironman.mark.MarkState;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Iron Man 3D parts on the player model: the Mark 50 helmet (head mask,
 * drawn with the baked "cut" frames so it closes last, spec §4.2), the
 * thruster flames and the Stage 2 combat parts (IronManCombatParts).
 */
public final class IronManPartsProvider implements PlayerGeoLayer.Provider {
   public static final ResourceLocation HEAD_MASK = new ResourceLocation("viltrumitecore", "geo/ironman/mark_50/head_mask.geo.json");

   @Override
   public void collect(AbstractClientPlayer player, float partialTick, boolean firstPerson, List<PlayerGeoLayer.Part> out) {
      HeroPublicSnapshot snapshot = IronManView.of(player);
      if (snapshot == null) {
         return;
      }

      // Stage 3: an open helmet folds the mask back along the head (HelmetAnim), Tony's face shows.
      int frame = helmetFrame(player, snapshot, partialTick);
      if (!firstPerson && IronManView.helmet(frame) && !MarkState.of(snapshot).markOn()) {
         ResourceLocation cut = IronManSuitTextures.INSTANCE.cut(frame);
         if (cut != null) {
            List<PlayerGeoLayer.Pass> passes = new ArrayList<>(3);
            passes.add(PlayerGeoLayer.Pass.cutout(cut));
            ResourceLocation glow = IronManSuitTextures.INSTANCE.glow(frame);
            if (glow != null) {
               passes.add(PlayerGeoLayer.Pass.glow(glow));
            }

            ResourceLocation damage = NanoDamageVisuals.INSTANCE.texture(player, snapshot);
            if (damage != null) {
               passes.add(PlayerGeoLayer.Pass.cutout(damage));
            }

            ResourceLocation rim = IronManSuitTextures.INSTANCE.rim(frame);
            if (rim != null) {
               passes.add(PlayerGeoLayer.Pass.glow(rim));
            }

            out.add(new PlayerGeoLayer.Part(HEAD_MASK, passes));
         }
      }

      ThrusterFlames.collect(player, snapshot, partialTick, firstPerson, out);
      IronManCombatParts.collect(player, snapshot, partialTick, firstPerson, out);
      dev.baranhan.viltrumitecore.client.ironman.mark.MarkVisuals.collectParts(player, snapshot, partialTick, firstPerson, out);
      dev.baranhan.viltrumitecore.client.ironman.mark.sig.SignatureVisuals.collectParts(player, snapshot, partialTick, firstPerson, out);
   }

   /** Reveal frame of the helmet part: the suit wave frame, capped by the helmet fold. */
   public static int helmetFrame(net.minecraft.world.entity.Entity player, HeroPublicSnapshot snapshot, float partialTick) {
      return Math.min(IronManView.frame(snapshot, partialTick), HelmetAnim.frame(HelmetAnim.progress(player, snapshot, partialTick)));
   }
}
