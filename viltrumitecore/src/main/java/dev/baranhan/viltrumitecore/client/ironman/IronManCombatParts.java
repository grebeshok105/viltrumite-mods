package dev.baranhan.viltrumitecore.client.ironman;

import dev.baranhan.viltrumitecore.client.anim.geo.BakedGeoModel;
import dev.baranhan.viltrumitecore.client.anim.geo.GeoBone;
import dev.baranhan.viltrumitecore.client.anim.render.PlayerGeoLayer;
import dev.baranhan.viltrumitecore.client.ironman.mark.MarkState;
import dev.baranhan.viltrumitecore.client.ironman.mark.MarkTextures;
import dev.baranhan.viltrumitecore.hero.HeroAction;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import dev.baranhan.viltrumitecore.hero.ironman.IronManFlags;
import dev.baranhan.viltrumitecore.hero.ironman.combat.RightTool;
import java.util.List;
import java.util.WeakHashMap;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Stage 2 geo parts on the player (animation-system §7.1): the nano blade or
 * hammer on the right fist, the nano shield on the left forearm, the shoulder
 * missile pods, and the repulsor palm glow while charging. Forming and
 * dissolving grow / shrink the part from the wrist (nanite wave, spec §4.2,
 * §8.4) from the synced NANO_ARSENAL timeline. Both views: the arm parts are
 * also drawn on the first-person arm.
 */
public final class IronManCombatParts {
   public static final ResourceLocation BLADE = geo("nano_blade");
   public static final ResourceLocation HAMMER = geo("nano_hammer");
   public static final ResourceLocation SHIELD = geo("nano_shield");
   public static final ResourceLocation PODS = geo("missile_pods");
   public static final ResourceLocation ENERGY_SHIELD = new ResourceLocation("viltrumitecore", "geo/ironman/marks/energy_shield.geo.json");
   public static final ResourceLocation TEXTURE = new ResourceLocation("viltrumitecore", "textures/entity/hero/ironman_nano_parts.png");
   public static final ResourceLocation GLOW = new ResourceLocation("viltrumitecore", "textures/entity/hero/ironman_nano_parts_glow.png");
   private static final List<PlayerGeoLayer.Pass> PASSES = List.of(PlayerGeoLayer.Pass.cutout(TEXTURE), PlayerGeoLayer.Pass.glow(GLOW));
   /** Last formed weapon per player: the dissolve wave still knows what to shrink. */
   private static final WeakHashMap<AbstractClientPlayer, RightTool> LAST_WEAPON = new WeakHashMap<>();
   /** Pod flap opening 0..1 per player (render thread). */
   private static final WeakHashMap<AbstractClientPlayer, float[]> FLAPS = new WeakHashMap<>();

   private IronManCombatParts() {
   }

   /** Weapon scale 0..1 from the forming / dissolving wave: grows from the wrist. */
   public static float weaponScale(boolean forming, boolean dissolving, float waveProgress) {
      if (forming) {
         return Mth.clamp(waveProgress, 0.05F, 1.0F);
      }

      return dissolving ? Mth.clamp(1.0F - waveProgress, 0.0F, 1.0F) : 1.0F;
   }

   static void collect(AbstractClientPlayer player, HeroPublicSnapshot snapshot, float partialTick, boolean firstPerson, List<PlayerGeoLayer.Part> out) {
      if (!IronManView.worn(snapshot)) {
         LAST_WEAPON.remove(player);
         FLAPS.remove(player);
         return;
      }

      RightTool tool = IronManView.tool(snapshot);
      boolean wave = IronManView.channel(snapshot, HeroAction.NANO_ARSENAL);
      float waveProgress = IronManView.progress(snapshot, HeroAction.NANO_ARSENAL, partialTick);
      boolean mark = MarkState.of(snapshot).markOn();
      RightTool weapon = tool.nanoWeapon() && !mark ? tool : null;
      if (weapon != null) {
         LAST_WEAPON.put(player, weapon);
      }

      boolean dissolving = weapon == null && wave;
      RightTool drawn = weapon != null ? weapon : dissolving ? LAST_WEAPON.get(player) : null;
      if (drawn != null) {
         float scale = weaponScale(weapon != null && wave, dissolving, waveProgress);
         String bone = drawn == RightTool.NANO_BLADE ? "blade" : "hammer";
         out.add(new PlayerGeoLayer.Part(drawn == RightTool.NANO_BLADE ? BLADE : HAMMER, PASSES, geo -> grow(geo, bone, scale)));
      } else if (!wave) {
         LAST_WEAPON.remove(player);
      }

      if (IronManFlags.is(snapshot.heroFlags(), IronManFlags.Field.SHIELD_UP)) {
         // A mark raises an energy hex shield from the forearm instead of the nano plate.
         if (mark) {
            out.add(new PlayerGeoLayer.Part(ENERGY_SHIELD, List.of(PlayerGeoLayer.Pass.glow(MarkTextures.ENERGY_HEX))));
         } else {
            out.add(new PlayerGeoLayer.Part(SHIELD, PASSES));
         }
      }

      // Palm glow while the repulsor charges (reuses the palm flame geo, short and bright).
      if (tool == RightTool.REPULSOR && IronManView.channel(snapshot, HeroAction.SECONDARY_USE)) {
         float charge = IronManView.progress(snapshot, HeroAction.SECONDARY_USE, partialTick);
         float time = player.tickCount + partialTick;
         float length = 0.25F + 0.55F * charge + 0.05F * Mth.sin(time * 2.1F);
         boolean both = charge >= 1.0F;
         boolean right = !IronManFlags.is(snapshot.heroFlags(), IronManFlags.Field.SHOT_HAND) || both;
         out.add(new PlayerGeoLayer.Part(ThrusterFlames.PALMS, List.of(PlayerGeoLayer.Pass.glow(ThrusterFlames.texture(0), 0.55F, 0.85F, 1.0F)),
            geo -> palms(geo, length, right || both, !right || both)));
      }

      if (firstPerson) {
         return;
      }

      float[] flap = FLAPS.computeIfAbsent(player, p -> new float[1]);
      boolean open = IronManFlags.is(snapshot.heroFlags(), IronManFlags.Field.MISSILE_FLAPS) || IronManView.channel(snapshot, HeroAction.MISSILES);
      flap[0] = Mth.lerp(0.25F, flap[0], open ? 1.0F : 0.0F);
      if (flap[0] > 0.02F || open) {
         float k = flap[0];
         out.add(new PlayerGeoLayer.Part(PODS, PASSES, geo -> pods(geo, k)));
      }
   }

   private static void grow(BakedGeoModel geo, String bone, float scale) {
      GeoBone part = geo.getBone(bone);
      if (part != null) {
         part.scaleX = Math.max(0.2F, scale);
         part.scaleY = scale;
         part.scaleZ = Math.max(0.2F, scale);
         part.hidden = scale <= 0.01F;
      }
   }

   private static void palms(BakedGeoModel geo, float length, boolean right, boolean left) {
      GeoBone r = geo.getBone("flameRight");
      GeoBone l = geo.getBone("flameLeft");
      if (r != null) {
         r.scaleY = length;
         r.hidden = !right;
      }

      if (l != null) {
         l.scaleY = length;
         l.hidden = !left;
      }
   }

   /** Pods rise out of the shoulders and the flaps swing open (≈ 100°). */
   private static void pods(BakedGeoModel geo, float open) {
      for (String arm : new String[]{"armorRightArm", "armorLeftArm"}) {
         GeoBone pod = geo.getBone(arm + "Pod");
         GeoBone flap = geo.getBone(arm + "Flap");
         GeoBone tips = geo.getBone(arm + "Tips");
         if (pod != null) {
            pod.scaleY = 0.3F + 0.7F * open;
         }

         if (flap != null) {
            flap.rotX = -1.75F * open;
         }

         if (tips != null) {
            tips.hidden = open < 0.6F;
         }
      }
   }

   private static ResourceLocation geo(String name) {
      return new ResourceLocation("viltrumitecore", "geo/ironman/nano/" + name + ".geo.json");
   }
}
