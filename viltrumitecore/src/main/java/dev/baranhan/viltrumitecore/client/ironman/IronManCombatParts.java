package dev.baranhan.viltrumitecore.client.ironman;

import dev.baranhan.viltrumitecore.client.anim.geo.BakedGeoModel;
import dev.baranhan.viltrumitecore.client.anim.geo.GeoBone;
import dev.baranhan.viltrumitecore.client.anim.render.PlayerGeoLayer;
import dev.baranhan.viltrumitecore.client.ironman.mark.MarkState;
import dev.baranhan.viltrumitecore.client.ironman.mark.MarkTextures;
import dev.baranhan.viltrumitecore.hero.HeroAction;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import dev.baranhan.viltrumitecore.hero.ironman.IronManFlags;
import dev.baranhan.viltrumitecore.hero.ironman.IronManVariant;
import dev.baranhan.viltrumitecore.hero.ironman.combat.RightTool;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkId;
import java.util.List;
import java.util.WeakHashMap;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Stage 2 geo parts on the player (animation-system §7.1), converted from the
 * Satsu addon by tools/assets/convert_ironman_stage2_parts.py: the nano blade
 * (katar) or hammer (mallet) on the right fist, the shield plate on the left
 * forearm (nano texture, or the hex force field in a mark), the shoulder rockets and the nano forearm
 * rocket launcher while missiles are held, and the repulsor palm glow while
 * charging. Forming and dissolving grow / shrink the weapon from the wrist
 * (nanite wave, spec §4.2, §8.4) from the synced NANO_ARSENAL timeline. Arm
 * parts are also drawn on the first-person arm.
 */
public final class IronManCombatParts {
   public static final ResourceLocation BLADE = geo("nano_blade");
   public static final ResourceLocation HAMMER = geo("nano_hammer");
   public static final ResourceLocation SHIELD = geo("nano_shield");
   public static final ResourceLocation ROCKETS = geo("shoulder_rockets");
   public static final ResourceLocation LAUNCHER = geo("rocket_launcher");
   public static final ResourceLocation LAUNCHER_FIRST_PERSON = geo("rocket_launcher_first_person");
   private static final List<PlayerGeoLayer.Pass> BLADE_PASSES = List.of(PlayerGeoLayer.Pass.cutout(texture("nano_blade")), PlayerGeoLayer.Pass.glow(texture("nano_blade_glow")));
   private static final List<PlayerGeoLayer.Pass> HAMMER_PASSES = List.of(PlayerGeoLayer.Pass.cutout(texture("nano_hammer")), PlayerGeoLayer.Pass.glow(texture("nano_hammer_glow")));
   private static final List<PlayerGeoLayer.Pass> SHIELD_PASSES = List.of(PlayerGeoLayer.Pass.cutout(texture("nano_shield")));
   private static final List<PlayerGeoLayer.Pass> LAUNCHER_PASSES = List.of(PlayerGeoLayer.Pass.cutout(texture("rocket_launcher")));
   private static final List<PlayerGeoLayer.Pass> FIELD_PASSES = List.of(PlayerGeoLayer.Pass.glow(MarkTextures.ENERGY_SHIELD));
   private static final List<PlayerGeoLayer.Pass> NANO_SKIN_PASSES = List.of(PlayerGeoLayer.Pass.cutout(IronManSuitTextures.SUIT), PlayerGeoLayer.Pass.glow(IronManSuitTextures.SUIT_GLOW));
   /** Last formed weapon per player: the dissolve wave still knows what to shrink. */
   private static final WeakHashMap<AbstractClientPlayer, RightTool> LAST_WEAPON = new WeakHashMap<>();
   /** Missile flap opening 0..1 and the frame time it was last stepped, per player (render thread). */
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
         boolean blade = drawn == RightTool.NANO_BLADE;
         String bone = blade ? "blade" : "hammer";
         out.add(new PlayerGeoLayer.Part(blade ? BLADE : HAMMER, blade ? BLADE_PASSES : HAMMER_PASSES, geo -> grow(geo, bone, scale)));
      } else if (!wave) {
         LAST_WEAPON.remove(player);
      }

      if (IronManFlags.is(snapshot.heroFlags(), IronManFlags.Field.SHIELD_UP)) {
         // Same forearm plate in both views; a mark fills it with the hex force field.
         out.add(new PlayerGeoLayer.Part(SHIELD, mark ? FIELD_PASSES : SHIELD_PASSES));
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

      float k = flapOpen(player, snapshot, partialTick);
      if (k <= 0.02F) {
         return;
      }

      if (!mark) {
         out.add(new PlayerGeoLayer.Part(firstPerson ? LAUNCHER_FIRST_PERSON : LAUNCHER, LAUNCHER_PASSES, geo -> grow(geo, "rocket_launcher", k)));
      }

      MarkId worn = IronManVariant.mark(snapshot.variant());
      if (!firstPerson && ownShoulderRockets(worn)) {
         List<PlayerGeoLayer.Pass> skin = worn == null || !mark ? NANO_SKIN_PASSES
            : List.of(PlayerGeoLayer.Pass.cutout(MarkTextures.skin(worn)), PlayerGeoLayer.Pass.glow(MarkTextures.glow(worn)));
         out.add(new PlayerGeoLayer.Part(ROCKETS, skin, geo -> rockets(geo, k)));
      }
   }

   /**
    * Smoothed missile flap opening 0..1 (held missiles or the synced flap flag).
    * Stepped once per frame, so both views and the mark flap parts share it.
    */
   public static float flapOpen(AbstractClientPlayer player, HeroPublicSnapshot snapshot, float partialTick) {
      float[] flap = FLAPS.computeIfAbsent(player, p -> new float[]{0.0F, -1.0F});
      float now = player.tickCount + partialTick;
      if (now != flap[1]) {
         boolean open = IronManFlags.is(snapshot.heroFlags(), IronManFlags.Field.MISSILE_FLAPS) || IronManView.channel(snapshot, HeroAction.MISSILES);
         flap[0] = Mth.lerp(0.25F, flap[0], open ? 1.0F : 0.0F);
         flap[1] = now;
      }

      return flap[0];
   }

   /**
    * The full_body shoulder rockets of the nano and most marks. Mark 7 and War
    * Machine open their own Satsu flaps / shoulder modules (spec §3.1), and the
    * Sind Mark 42 has no full_body geometry.
    */
   private static boolean ownShoulderRockets(MarkId worn) {
      return worn != MarkId.MARK_7 && worn != MarkId.WAR_MACHINE_MK2 && worn != MarkId.MARK_42;
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

   /** Satsu shoulder_rockets clip: the rockets slide up and forward out of the shoulders (offset -1, 1, -2 px). */
   private static void rockets(BakedGeoModel geo, float open) {
      GeoBone rockets = geo.getBone("shoulder_rockets");
      if (rockets != null) {
         rockets.posX = -open;
         rockets.posY = open;
         rockets.posZ = -2.0F * open;
      }
   }

   private static ResourceLocation geo(String name) {
      return new ResourceLocation("viltrumitecore", "geo/ironman/nano/" + name + ".geo.json");
   }

   private static ResourceLocation texture(String name) {
      return new ResourceLocation("viltrumitecore", "textures/entity/ironman/nano/" + name + ".png");
   }
}
