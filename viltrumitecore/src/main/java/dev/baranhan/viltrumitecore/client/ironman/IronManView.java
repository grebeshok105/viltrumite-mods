package dev.baranhan.viltrumitecore.client.ironman;

import dev.baranhan.viltrumitecore.client.anim.render.RevealMask;
import dev.baranhan.viltrumitecore.hero.HeroAction;
import dev.baranhan.viltrumitecore.hero.HeroId;
import dev.baranhan.viltrumitecore.hero.HeroPlayer;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import dev.baranhan.viltrumitecore.hero.ironman.IronManFlags;
import dev.baranhan.viltrumitecore.hero.ironman.IronManRules;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;

/**
 * Read-only client view of the synced Iron Man snapshot: suit reveal, flags
 * and energy. Every visual layer reads state through this class.
 */
public final class IronManView {
   private IronManView() {
   }

   /** Iron Man snapshot of this entity, or null. */
   @Nullable
   public static HeroPublicSnapshot of(@Nullable Entity entity) {
      if (entity instanceof HeroPlayer heroPlayer) {
         HeroPublicSnapshot snapshot = heroPlayer.getHeroSnapshot();
         if (snapshot != null && snapshot.heroId() == HeroId.IRON_MAN) {
            return snapshot;
         }
      }

      return null;
   }

   /** Suit coverage 0..1 (0 = Tony, 1 = full suit) for a snapshot. */
   public static float reveal(HeroPublicSnapshot snapshot, float partialTick) {
      return reveal(snapshot.heroFlags(), snapshot.actionId(), snapshot.actionElapsed(), snapshot.actionLength(), partialTick);
   }

   /**
    * Pure form: deploy grows 0 → 1, retract shrinks 1 → 0 over the synced wave
    * timeline; outside a wave the worn flag decides.
    */
   public static float reveal(int flags, int actionId, int elapsed, int length, float partialTick) {
      boolean deploying = IronManFlags.is(flags, IronManFlags.Field.DEPLOYING);
      boolean retracting = IronManFlags.is(flags, IronManFlags.Field.RETRACTING);
      if ((deploying || retracting) && actionId == HeroAction.SUIT.ordinal() && length > 0) {
         float t = Math.max(0.0F, Math.min(1.0F, (elapsed + partialTick) / (float)length));
         return deploying ? t : 1.0F - t;
      }

      return IronManFlags.is(flags, IronManFlags.Field.SUIT_WORN) ? 1.0F : 0.0F;
   }

   /** Reveal frame 0..16 of the baked suit textures. */
   public static int frame(HeroPublicSnapshot snapshot, float partialTick) {
      return RevealMask.frame(reveal(snapshot, partialTick));
   }

   /** Helmet part drawn from this frame on (the head reveals last). */
   public static boolean helmet(int frame) {
      return frame > 0 && frame >= RevealMask.field().headStartFrame();
   }

   /** Suit fully on and no wave running: thrusters, poses and HUD are active. */
   public static boolean suitReady(HeroPublicSnapshot snapshot) {
      return worn(snapshot) && !transitioning(snapshot);
   }

   public static boolean worn(HeroPublicSnapshot snapshot) {
      return IronManFlags.is(snapshot.heroFlags(), IronManFlags.Field.SUIT_WORN);
   }

   public static boolean deploying(HeroPublicSnapshot snapshot) {
      return IronManFlags.is(snapshot.heroFlags(), IronManFlags.Field.DEPLOYING);
   }

   public static boolean retracting(HeroPublicSnapshot snapshot) {
      return IronManFlags.is(snapshot.heroFlags(), IronManFlags.Field.RETRACTING);
   }

   public static boolean transitioning(HeroPublicSnapshot snapshot) {
      return deploying(snapshot) || retracting(snapshot);
   }

   public static boolean glide(HeroPublicSnapshot snapshot) {
      return IronManFlags.is(snapshot.heroFlags(), IronManFlags.Field.GLIDE);
   }

   public static boolean heavyLanding(HeroPublicSnapshot snapshot) {
      return IronManFlags.is(snapshot.heroFlags(), IronManFlags.Field.HEAVY_LANDING);
   }

   /** Energy 0..100 (snapshot resource is energy x10). */
   public static float energy(HeroPublicSnapshot snapshot) {
      return Math.max(0.0F, Math.min(IronManRules.ENERGY_MAX, snapshot.resource() / 10.0F));
   }
}
