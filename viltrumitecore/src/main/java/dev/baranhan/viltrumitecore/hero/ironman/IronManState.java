package dev.baranhan.viltrumitecore.hero.ironman;

import dev.baranhan.viltrumitecore.hero.CleanupReason;
import dev.baranhan.viltrumitecore.hero.HeroId;
import dev.baranhan.viltrumitecore.hero.HeroPlayer;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/** Server-side per-player Iron Man state. Suit and energy survive relog (spec §16). */
public final class IronManState {
   private static final String KEY = "IronMan";
   public final Suit suit = new Suit();
   public final Energy energy = new Energy();
   /** Energy hit 0 in flight: glide profile (Task 9). */
   public boolean glide;
   /** Transient: sonic-ram rehit timestamps per target (bounded). */
   public final java.util.Map<java.util.UUID, Long> ramHits = new dev.baranhan.viltrumitecore.hero.control.BoundedMap<>(64);
   /** Transient: ticks until the next fly-by punch may land. */
   public int flyByCooldown;

   /** Flight grant: only while fully worn. */
   public boolean wantsFlight() {
      return this.suit.worn();
   }

   /** Pure part of HeroDefinition.cleanup (spec §16). */
   public void onCleanup(CleanupReason reason) {
      switch (reason) {
         case DEATH -> {
            this.suit.clear();
            this.glide = false;
         }
         case DISCONNECT -> this.suit.resolve();
         case HERO_CHANGE -> {
            this.suit.clear();
            this.energy.reset();
            this.glide = false;
         }
      }
   }

   /** Death: suit off, energy full; cooldowns carry over (later stages). */
   public static IronManState cloneForRespawn(IronManState original) {
      return new IronManState();
   }

   public void save(CompoundTag nbt) {
      CompoundTag tag = new CompoundTag();
      this.suit.save(tag);
      this.energy.save(tag);
      nbt.put(KEY, tag);
   }

   public void load(CompoundTag nbt) {
      CompoundTag tag = nbt.getCompound(KEY);
      this.suit.load(tag);
      this.energy.load(tag);
      this.glide = false;
   }

   @Nullable
   public static IronManState of(@Nullable Player player) {
      if (player instanceof HeroPlayer heroPlayer && heroPlayer.getHeroId() == HeroId.IRON_MAN) {
         return heroPlayer.viltrumitecore$getHeroState() instanceof IronManState state ? state : null;
      }

      return null;
   }

   public static IronManState ensure(Player player) {
      HeroPlayer heroPlayer = (HeroPlayer)player;
      if (heroPlayer.viltrumitecore$getHeroState() instanceof IronManState existing) {
         return existing;
      }

      IronManState state = new IronManState();
      heroPlayer.viltrumitecore$setHeroState(state);
      return state;
   }
}
