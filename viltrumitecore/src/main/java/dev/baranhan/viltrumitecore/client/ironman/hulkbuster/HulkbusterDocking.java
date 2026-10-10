package dev.baranhan.viltrumitecore.client.ironman.hulkbuster;

import dev.baranhan.viltrumitecore.client.anim.geo.BakedGeoModel;
import dev.baranhan.viltrumitecore.client.anim.geo.GeoBone;
import dev.baranhan.viltrumitecore.client.anim.render.PlayerGeoLayer;
import dev.baranhan.viltrumitecore.client.ironman.IronManView;
import dev.baranhan.viltrumitecore.client.ironman.veronica.PartWrapAnimator;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import dev.baranhan.viltrumitecore.hero.ironman.IronManVariant;
import dev.baranhan.viltrumitecore.hero.ironman.hulkbuster.HulkbusterLayer;
import java.util.List;
import net.minecraft.client.player.AbstractClientPlayer;

/**
 * Locked Hulkbuster parts on the normal-size player (DROPPING, ASSEMBLING, PARTIAL):
 * the four groups of parts.geo.json are drawn on the vanilla limbs by
 * {@link PlayerGeoLayer}, so they follow every pose and show in first person too.
 * A group wraps (opens and closes around its limb) for {@link HulkbusterAssembly#WRAP_TICKS}
 * after it locks.
 */
public final class HulkbusterDocking implements PlayerGeoLayer.Provider {
   private static final List<PlayerGeoLayer.Pass> PASSES = List.of(
      PlayerGeoLayer.Pass.cutout(HulkbusterAssets.TEXTURE), PlayerGeoLayer.Pass.glow(HulkbusterAssets.GLOW));

   @Override
   public void collect(AbstractClientPlayer player, float partialTick, boolean firstPerson, List<PlayerGeoLayer.Part> out) {
      HeroPublicSnapshot snapshot = IronManView.of(player);
      if (snapshot == null) {
         return;
      }

      HulkbusterLayer.Phase phase = HulkbusterView.phase(snapshot);
      int mask = IronManVariant.hulkParts(snapshot.variant());
      if (!HulkbusterView.docking(phase) || mask == 0) {
         return;
      }

      int phaseOrdinal = phase.ordinal();
      double assembleTicks = snapshot.actionElapsed() + partialTick;
      out.add(new PlayerGeoLayer.Part(HulkbusterAssets.PARTS, PASSES, geo -> pose(geo, mask, phaseOrdinal, assembleTicks)));
   }

   /** Shows the locked groups, hides the rest, and sets each group's wrap scale. */
   static void pose(BakedGeoModel geo, int mask, int phase, double assembleTicks) {
      for (int group = 0; group < HulkbusterAssembly.GROUPS; group++) {
         boolean on = HulkbusterAssembly.locked(mask, group);
         float scale = PartWrapAnimator.scale(HulkbusterAssembly.wrapProgress(group, phase, assembleTicks));
         for (String name : HulkbusterAssembly.BONES[group]) {
            GeoBone bone = geo.getBone(name);
            if (bone != null) {
               bone.hidden = !on;
               bone.scaleX = scale;
               bone.scaleZ = scale;
            }
         }
      }
   }
}
