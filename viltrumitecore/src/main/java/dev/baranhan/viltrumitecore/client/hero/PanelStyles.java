package dev.baranhan.viltrumitecore.client.hero;

import dev.baranhan.viltrumitecore.hero.HeroId;
import dev.baranhan.viltrumitecore.hero.HeroPlayer;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.world.entity.player.Player;

/** Ability panel style per hero; unregistered heroes keep {@link PanelStyle#DEFAULT}. */
public final class PanelStyles {
   private static final Map<HeroId, PanelStyle> STYLES = new EnumMap<>(HeroId.class);

   private PanelStyles() {
   }

   public static synchronized void register(HeroId hero, PanelStyle style) {
      STYLES.put(Objects.requireNonNull(hero, "hero"), Objects.requireNonNull(style, "style"));
   }

   public static synchronized PanelStyle of(@Nullable HeroId hero) {
      PanelStyle style = hero == null ? null : STYLES.get(hero);
      return style == null ? PanelStyle.DEFAULT : style;
   }

   public static PanelStyle of(Player player) {
      return of(player instanceof HeroPlayer heroPlayer ? heroPlayer.getHeroId() : null);
   }

   /** Tests only. */
   static synchronized void clear() {
      STYLES.clear();
   }
}
