package dev.baranhan.viltrumitecore.client.ironman;

import dev.baranhan.viltrumitecore.client.anim.render.RevealMask;
import dev.baranhan.viltrumitecore.hero.HeroId;
import dev.baranhan.viltrumitecore.hero.HeroPlayer;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import dev.baranhan.viltrumitecore.hero.ironman.IronManFlags;
import net.minecraft.world.entity.player.Player;

/** Which reveal frame a player shows, from the synced snapshot only. */
public final class IronManSkinView {
   private IronManSkinView() {
   }

   /** Pure: deploy grows 0→16, retract shrinks 16→0, worn 16, otherwise 0. */
   static int frame(int flags, int elapsed, int length, float partialTick) {
      if (IronManFlags.is(flags, IronManFlags.Field.SUIT_WORN)) {
         return RevealMask.FRAMES;
      }

      boolean deploy = IronManFlags.is(flags, IronManFlags.Field.DEPLOYING);
      boolean retract = IronManFlags.is(flags, IronManFlags.Field.RETRACTING);
      if ((!deploy && !retract) || length <= 0) {
         return 0;
      }

      float progress = Math.max(0.0F, Math.min(1.0F, (elapsed + partialTick) / length));
      return RevealMask.frame(deploy ? progress : 1.0F - progress);
   }

   public static int frame(Player player, float partialTick) {
      if (!(player instanceof HeroPlayer heroPlayer)) {
         return 0;
      }

      HeroPublicSnapshot snapshot = heroPlayer.getHeroSnapshot();
      if (snapshot.heroId() != HeroId.IRON_MAN) {
         return 0;
      }

      return frame(snapshot.heroFlags(), snapshot.actionElapsed(), snapshot.actionLength(), partialTick);
   }
}
