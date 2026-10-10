package dev.baranhan.viltrumitecore.client.ironman;

import dev.baranhan.viltrumitecore.client.anim.AnimCache;
import dev.baranhan.viltrumitecore.client.anim.animation.Animation;
import dev.baranhan.viltrumitecore.client.anim.animation.AnimationController;
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
 * Stage 2 geo parts on the player (animation-system §7.1), converted from the
 * Satsu addon by tools/assets/convert_ironman_stage2_parts.py: the nano blade
 * (katar) or hammer (mallet) on the right fist, the shield plate on the left
 * forearm (nano texture, or the hex force field in a mark), the Sind shoulder
 * launchers (every suit) and the nano forearm launcher with its rocket while
 * missiles are held, and the repulsor palm glow while
 * charging. Forming and dissolving grow / shrink the weapon from the wrist
 * (nanite wave, spec §4.2, §8.4) from the synced NANO_ARSENAL timeline. Arm
 * parts are also drawn on the first-person arm.
 */
public final class IronManCombatParts {
   public static final ResourceLocation BLADE = geo("nano_blade");
   public static final ResourceLocation HAMMER = geo("nano_hammer");
   public static final ResourceLocation SHIELD = geo("nano_shield");
   public static final ResourceLocation LAUNCHERS = new ResourceLocation("viltrumitecore", "geo/ironman/missiles/shoulder_launchers.geo.json");
   public static final ResourceLocation LAUNCHER_ROCKETS = new ResourceLocation("viltrumitecore", "geo/ironman/missiles/shoulder_rockets.geo.json");
   public static final ResourceLocation ARM_ROCKET = new ResourceLocation("viltrumitecore", "geo/ironman/missiles/arm_rocket.geo.json");
   public static final ResourceLocation MISSILE_CLIPS = new ResourceLocation("viltrumitecore", "animations/ironman/missiles.animation.json");
   public static final ResourceLocation LAUNCHER = geo("rocket_launcher");
   public static final ResourceLocation LAUNCHER_FIRST_PERSON = geo("rocket_launcher_first_person");
   private static final List<PlayerGeoLayer.Pass> BLADE_PASSES = List.of(PlayerGeoLayer.Pass.cutout(texture("nano_blade")), PlayerGeoLayer.Pass.glow(texture("nano_blade_glow")));
   private static final List<PlayerGeoLayer.Pass> HAMMER_PASSES = List.of(PlayerGeoLayer.Pass.cutout(texture("nano_hammer")), PlayerGeoLayer.Pass.glow(texture("nano_hammer_glow")));
   private static final List<PlayerGeoLayer.Pass> SHIELD_PASSES = List.of(PlayerGeoLayer.Pass.cutout(texture("nano_shield")));
   private static final List<PlayerGeoLayer.Pass> LAUNCHER_PASSES = List.of(PlayerGeoLayer.Pass.cutout(texture("rocket_launcher")));
   private static final List<PlayerGeoLayer.Pass> FIELD_PASSES = List.of(PlayerGeoLayer.Pass.glow(MarkTextures.ENERGY_SHIELD));
   private static final List<PlayerGeoLayer.Pass> LAUNCHER_POD_PASSES = List.of(PlayerGeoLayer.Pass.cutout(missileTexture("launcher")));
   private static final List<PlayerGeoLayer.Pass> ROCKET_TIP_PASSES = List.of(PlayerGeoLayer.Pass.cutout(missileTexture("rocket_tips")));
   private static final List<PlayerGeoLayer.Pass> ARM_ROCKET_PASSES = List.of(PlayerGeoLayer.Pass.cutout(missileTexture("rocket")));
   /** Shoulder pods rise this far (px) out of the shoulders while open. */
   private static final float POD_RISE = 2.5F;
   /** The arm rocket slides this far (px) towards the fist. */
   private static final float ARM_ROCKET_SLIDE = 3.0F;
   /** Ticks the launchers stay open, empty, after the volley. */
   private static final float FIRED_HOLD_TICKS = 12.0F;
   /** Open / close speed of the launchers (1/s). */
   private static final float LAUNCHER_RATE = 14.0F;
   /** Last formed weapon per player: the dissolve wave still knows what to shrink. */
   private static final WeakHashMap<AbstractClientPlayer, RightTool> LAST_WEAPON = new WeakHashMap<>();
   /** Per player: launcher opening 0..1, last step (s), last tick the missiles were held, loaded flag (render thread). */
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

      boolean loaded = launchersLoaded(player);
      if (!mark) {
         out.add(new PlayerGeoLayer.Part(firstPerson ? LAUNCHER_FIRST_PERSON : LAUNCHER, LAUNCHER_PASSES, geo -> grow(geo, "rocket_launcher", k)));
         out.add(new PlayerGeoLayer.Part(ARM_ROCKET, ARM_ROCKET_PASSES, geo -> armRocket(geo, k, loaded)));
      }

      if (!firstPerson) {
         out.add(new PlayerGeoLayer.Part(LAUNCHERS, LAUNCHER_POD_PASSES, geo -> pods(geo, k)));
         out.add(new PlayerGeoLayer.Part(LAUNCHER_ROCKETS, ROCKET_TIP_PASSES, geo -> rocketTips(geo, k, loaded)));
      }
   }

   /**
    * Launcher opening 0..1: open while the missiles are held (or the synced flap
    * flag), held open and empty for a moment after the volley, then closed.
    * Eased by real time, so it runs the same at any frame rate.
    */
   public static float flapOpen(AbstractClientPlayer player, HeroPublicSnapshot snapshot, float partialTick) {
      float[] st = FLAPS.computeIfAbsent(player, p -> new float[]{0.0F, Float.NaN, -1000.0F, 0.0F});
      float now = player.tickCount + partialTick;
      boolean held = IronManFlags.is(snapshot.heroFlags(), IronManFlags.Field.MISSILE_FLAPS) || IronManView.channel(snapshot, HeroAction.MISSILES);
      if (held) {
         st[2] = now;
         st[3] = 1.0F;
      } else if (now - st[2] > FIRED_HOLD_TICKS || now < st[2]) {
         st[3] = 0.0F;
      }

      boolean open = held || now - st[2] <= FIRED_HOLD_TICKS && now >= st[2];
      float seconds = System.nanoTime() / 1.0E9F;
      float dt = Float.isNaN(st[1]) ? 0.0F : Math.min(0.1F, seconds - st[1]);
      st[1] = seconds;
      st[0] = Mth.lerp(1.0F - (float)Math.exp(-LAUNCHER_RATE * dt), st[0], open ? 1.0F : 0.0F);
      return st[0];
   }

   /** Rockets sit in the tubes while the missiles are held; empty after the volley. */
   private static boolean launchersLoaded(AbstractClientPlayer player) {
      float[] st = FLAPS.get(player);
      return st != null && st[3] > 0.5F && st[2] >= player.tickCount - 1;
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

   /** Sind shoulder pods: rise out of the shoulders, side panels open (cannons.fsk, baked). */
   private static void pods(BakedGeoModel geo, float open) {
      Animation clip = AnimCache.animation(MISSILE_CLIPS, "open");
      if (clip != null) {
         AnimationController.seek(clip, geo, open * clip.lengthSeconds());
      }

      for (String pod : new String[]{"leftcannon", "rightcannon"}) {
         GeoBone bone = geo.getBone(pod);
         if (bone != null) {
            bone.posY = POD_RISE * open;
         }
      }
   }

   /** Rocket tips rise with the pods; the rows are empty after the volley (hidden is not reset, set every frame). */
   private static void rocketTips(BakedGeoModel geo, float open, boolean loaded) {
      for (String side : new String[]{"bone2", "bone3"}) {
         GeoBone bone = geo.getBone(side);
         if (bone != null) {
            bone.posY = POD_RISE * open;
         }
      }

      for (String row : new String[]{"leftrockets1", "leftrockets2", "leftrockets3", "rightrockets1", "rightrockets2", "rightrockets3"}) {
         GeoBone bone = geo.getBone(row);
         if (bone != null) {
            bone.hidden = !loaded;
         }
      }
   }

   /** The arm rocket slides out past the fist while loaded. */
   private static void armRocket(BakedGeoModel geo, float open, boolean loaded) {
      GeoBone rocket = geo.getBone("rocket");
      if (rocket != null) {
         rocket.posY = -ARM_ROCKET_SLIDE * open;
         rocket.hidden = !loaded || open < 0.05F;
      }
   }

   private static ResourceLocation geo(String name) {
      return new ResourceLocation("viltrumitecore", "geo/ironman/nano/" + name + ".geo.json");
   }

   private static ResourceLocation missileTexture(String name) {
      return new ResourceLocation("viltrumitecore", "textures/entity/ironman/missiles/" + name + ".png");
   }

   private static ResourceLocation texture(String name) {
      return new ResourceLocation("viltrumitecore", "textures/entity/ironman/nano/" + name + ".png");
   }
}
