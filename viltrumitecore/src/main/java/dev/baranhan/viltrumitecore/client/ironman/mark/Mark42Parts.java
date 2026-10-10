package dev.baranhan.viltrumitecore.client.ironman.mark;

import dev.baranhan.viltrumitecore.client.anim.render.PlayerGeoLayer;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkId;
import dev.baranhan.viltrumitecore.hero.ironman.mark.SuitPart;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

/** The 14 Mark 42 pieces (spec §3.1: Sind {@code tabula/mk42}). */
public final class Mark42Parts {
   private Mark42Parts() {
   }

   /** Geo of one Mark 42 suit part: armor space, one top-level armor bone. */
   public static ResourceLocation geo(SuitPart part) {
      return PlateParts.geo(part);
   }

   /** Draw passes (cutout + glow) of that part. */
   public static List<PlayerGeoLayer.Pass> passes(SuitPart part) {
      return List.of(PlayerGeoLayer.Pass.cutout(MarkTextures.skin(MarkId.MARK_42)), PlayerGeoLayer.Pass.glow(MarkTextures.glow(MarkId.MARK_42)));
   }

   /** Thruster flame geo drawn with the part while flying, or null. */
   @Nullable
   public static ResourceLocation fire(SuitPart part) {
      return null;
   }
}
