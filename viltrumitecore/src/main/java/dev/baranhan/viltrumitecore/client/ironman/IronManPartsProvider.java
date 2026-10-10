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

      // Mark 15 camouflage: no suit parts on the invisible body, only the signature's own shimmer.
      if (dev.baranhan.viltrumitecore.hero.ironman.IronManFlags.is(snapshot.heroFlags(), dev.baranhan.viltrumitecore.hero.ironman.IronManFlags.Field.MARK_CAMO)) {
         return;
      }

      // Helmet (spec §10): the mask stays on, only the iron faceplate opens. Nano: the plate's nanites
      // recede from the middle of the face to its edges (and flow back when closing).
      int frame = helmetFrame(player, snapshot, partialTick);
      if (!firstPerson && IronManView.helmet(frame) && !MarkState.of(snapshot).markOn()) {
         float closed = HelmetAnim.progress(player, snapshot, partialTick);
         boolean plate = closed >= 0.999F;
         ResourceLocation cut = plate ? IronManSuitTextures.INSTANCE.cut(frame) : IronManSuitTextures.INSTANCE.cutOpen(frame);
         if (cut != null) {
            List<PlayerGeoLayer.Pass> passes = new ArrayList<>(6);
            passes.add(PlayerGeoLayer.Pass.cutout(cut));
            ResourceLocation glow = plate ? IronManSuitTextures.INSTANCE.glow(frame) : IronManSuitTextures.INSTANCE.glowOpen(frame);
            if (glow != null) {
               passes.add(PlayerGeoLayer.Pass.glow(glow));
            }

            if (!plate) {
               int step = (int)Math.ceil(closed * IronManSuitTextures.FACE_FRAMES - 0.001F);
               for (int kind = 0; kind < 3; kind++) {
                  ResourceLocation face = IronManSuitTextures.INSTANCE.face(kind, step);
                  if (face != null) {
                     passes.add(kind == 0 ? PlayerGeoLayer.Pass.cutout(face) : PlayerGeoLayer.Pass.glow(face));
                  }
               }
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

   /** Reveal frame of the helmet part: the suit wave frame (an open helmet only opens the faceplate). */
   public static int helmetFrame(net.minecraft.world.entity.Entity player, HeroPublicSnapshot snapshot, float partialTick) {
      return IronManView.frame(snapshot, partialTick);
   }
}
