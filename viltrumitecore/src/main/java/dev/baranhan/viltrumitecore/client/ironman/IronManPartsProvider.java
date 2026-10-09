package dev.baranhan.viltrumitecore.client.ironman;

import dev.baranhan.viltrumitecore.client.anim.render.PlayerGeoLayer;
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

      int frame = IronManView.frame(snapshot, partialTick);
      if (!firstPerson && IronManView.helmet(frame)) {
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
   }
}
