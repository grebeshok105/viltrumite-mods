package dev.baranhan.viltrumitecore.client.ironman.mark.sig;

import dev.baranhan.viltrumitecore.client.anim.AnimCache;
import dev.baranhan.viltrumitecore.client.anim.animation.Animation;
import dev.baranhan.viltrumitecore.client.anim.animation.AnimationController;
import dev.baranhan.viltrumitecore.client.anim.geo.GeoBone;
import dev.baranhan.viltrumitecore.client.anim.render.PlayerGeoLayer;
import dev.baranhan.viltrumitecore.client.ironman.IronManView;
import dev.baranhan.viltrumitecore.client.ironman.ThrusterFlames;
import dev.baranhan.viltrumitecore.client.ironman.mark.MarkExtras;
import dev.baranhan.viltrumitecore.entity.ViltrumiteEntities;
import dev.baranhan.viltrumitecore.hero.HeroAction;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import dev.baranhan.viltrumitecore.hero.ironman.IronManFlags;
import dev.baranhan.viltrumitecore.hero.ironman.IronManVariant;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkId;
import java.util.List;
import java.util.WeakHashMap;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.EntityRenderersEvent;

/**
 * Client entry of the seven mark signatures (spec §13): geo parts on the body
 * (Mark 7 emitters, Mark 42 glove, Mark 15 shimmer shell, Mark 39 booster,
 * War Machine turret), the rocket fist entity renderer. Beams, tracers and
 * casings are in {@link SignatureVfx}, the poses in {@link SignaturePoser}.
 */
public final class SignatureVisuals {
   private static final String TEXTURE_DIR = "textures/entity/ironman/marks/";
   static final ResourceLocation LASER = geo("laser_emitters");
   static final ResourceLocation BOOSTER = geo("booster");
   static final ResourceLocation BLAST = geo("booster_blast");
   static final ResourceLocation TURRET = geo("turret");
   static final ResourceLocation GLOVE = geo("glove");
   static final ResourceLocation CAMO = geo("camo_shell");

   private static final List<PlayerGeoLayer.Pass> LASER_IDLE = List.of(cutout("laser"), glow("laser_glow", 0.55F));
   private static final List<PlayerGeoLayer.Pass> LASER_LIVE = List.of(cutout("laser"), glow("laser_glow", 1.0F));
   /** Satsu Mark 39 jetpack and War Machine turret: drawn with the raw Satsu suit textures. */
   private static final List<PlayerGeoLayer.Pass> BOOSTER_PASSES = MarkExtras.passes(MarkId.MARK_39);
   private static final List<PlayerGeoLayer.Pass> TURRET_PASSES = MarkExtras.passes(MarkId.WAR_MACHINE_MK2);
   private static final ResourceLocation TURRET_CLIPS = new ResourceLocation("viltrumitecore", "animations/ironman/marks/war_machine_turret.animation.json");
   /** Satsu start_on clip: the gun swings over the shoulder by 0.83 s. */
   private static final double TURRET_UNFOLD_SECONDS = 0.8333;
   /** Turret unfold 0..1 and the frame time it was last stepped, per player (render thread). */
   private static final WeakHashMap<AbstractClientPlayer, float[]> TURRET_OPEN = new WeakHashMap<>();
   private static final List<PlayerGeoLayer.Pass> GLOVE_PASSES = List.of(cutout("glove"));

   private SignatureVisuals() {
   }

   public static void init() {
   }

   public static void addLayers(PlayerRenderer renderer) {
   }

   public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
      event.registerEntityRenderer(ViltrumiteEntities.ROCKET_FIST.get(), RocketFistRenderer::new);
   }

   public static void collectParts(AbstractClientPlayer player, HeroPublicSnapshot snapshot, float partialTick, boolean firstPerson, List<PlayerGeoLayer.Part> out) {
      if (!IronManView.worn(snapshot)) {
         return;
      }

      MarkId mark = IronManVariant.mark(snapshot.variant());
      if (mark == null) {
         return;
      }

      int flags = snapshot.heroFlags();
      boolean active = IronManFlags.is(flags, IronManFlags.Field.SIGNATURE_ACTIVE);
      switch (mark) {
         case MARK_7 -> out.add(new PlayerGeoLayer.Part(LASER, active ? LASER_LIVE : LASER_IDLE));
         case MARK_42 -> {
            if (!active) {
               out.add(new PlayerGeoLayer.Part(GLOVE, GLOVE_PASSES));
            }
         }
         case MARK_15 -> {
            if (IronManFlags.is(flags, IronManFlags.Field.MARK_CAMO) && !firstPerson) {
               out.add(new PlayerGeoLayer.Part(CAMO, List.of(camoPass(player))));
            }
         }
         case MARK_39 -> {
            if (!firstPerson) {
               out.add(new PlayerGeoLayer.Part(BOOSTER, BOOSTER_PASSES));
               if (active) {
                  out.add(blast(snapshot, partialTick, player));
               }
            }
         }
         case WAR_MACHINE_MK2 -> {
            float open = turretOpen(player, active, partialTick);
            float yaw = turretYaw(player, snapshot, partialTick) * open;
            out.add(new PlayerGeoLayer.Part(TURRET, TURRET_PASSES, geo -> {
               Animation unfold = AnimCache.animation(TURRET_CLIPS, "start_on");
               if (unfold != null && open > 0.0F) {
                  AnimationController.seek(unfold, geo, open * TURRET_UNFOLD_SECONDS);
               }

               GeoBone turret = geo.getBone("turret");
               if (turret != null) {
                  turret.rotY = yaw;
               }
            }));
         }
         default -> {
         }
      }
   }

   /** Booster flame: a cone that shrinks over the blast timeline, flickering like the thrusters. */
   private static PlayerGeoLayer.Part blast(HeroPublicSnapshot snapshot, float partialTick, AbstractClientPlayer player) {
      float progress = IronManView.progress(snapshot, HeroAction.SIGNATURE, partialTick);
      float length = Mth.clamp(1.0F - progress, 0.0F, 1.0F);
      float time = player.tickCount + partialTick;
      ResourceLocation texture = ThrusterFlames.texture((int)(time * 0.75F));
      List<PlayerGeoLayer.Pass> passes = List.of(PlayerGeoLayer.Pass.glow(texture, 0.85F, 0.95F, 1.0F), PlayerGeoLayer.Pass.glow(texture, 0.8F, 0.9F, 1.0F));
      return new PlayerGeoLayer.Part(BLAST, passes, geo -> {
         GeoBone flame = geo.getBone("boosterBlast");
         if (flame != null) {
            float flicker = 0.9F + 0.1F * Mth.sin(time * 2.3F);
            flame.scaleY = length * flicker;
            flame.hidden = length <= 0.01F;
         }
      });
   }

   /** Ripple strength: faint in hover, stronger while moving or attacking (spec §13.4). */
   private static PlayerGeoLayer.Pass camoPass(AbstractClientPlayer player) {
      float speed = (float)Mth.clamp(player.getDeltaMovement().horizontalDistance() * 4.0, 0.0, 1.0);
      float alpha = 0.12F + 0.5F * speed + (player.swinging ? 0.3F : 0.0F);
      return new PlayerGeoLayer.Pass(texture("camo_ripple"), PlayerGeoLayer.Pass.Kind.GLOW, 0.75F, 0.92F, 1.0F, alpha);
   }

   /** Smoothed turret unfold: the folded gun on the back swings out while the signature fires. */
   private static float turretOpen(AbstractClientPlayer player, boolean active, float partialTick) {
      float[] open = TURRET_OPEN.computeIfAbsent(player, p -> new float[]{0.0F, -1.0F});
      float now = player.tickCount + partialTick;
      if (now != open[1]) {
         open[0] = Mth.lerp(0.2F, open[0], active ? 1.0F : 0.0F);
         open[1] = now;
      }

      return open[0];
   }

   /** Turret yaw (radians) that turns the barrel towards the synced aim point, relative to the body. */
   static float turretYaw(AbstractClientPlayer player, HeroPublicSnapshot snapshot, float partialTick) {
      Vec3 target = snapshot.actionTarget();
      Vec3 from = player.getPosition(partialTick).add(0.0, 1.42, 0.0);
      Vec3 to = target == null ? player.getViewVector(partialTick) : target.subtract(from);
      double yaw = Math.toRadians(Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot));
      double cos = Math.cos(yaw);
      double sin = Math.sin(yaw);
      double forward = -to.x * sin + to.z * cos;
      double left = to.x * cos + to.z * sin;
      return (float)Math.atan2(-left, forward);
   }

   private static PlayerGeoLayer.Pass cutout(String name) {
      return PlayerGeoLayer.Pass.cutout(texture(name));
   }

   private static PlayerGeoLayer.Pass glow(String name, float alpha) {
      return new PlayerGeoLayer.Pass(texture(name), PlayerGeoLayer.Pass.Kind.GLOW, 1.0F, 1.0F, 1.0F, alpha);
   }

   private static ResourceLocation texture(String name) {
      return new ResourceLocation("viltrumitecore", TEXTURE_DIR + name + ".png");
   }

   private static ResourceLocation geo(String name) {
      return new ResourceLocation("viltrumitecore", "geo/ironman/marks/" + name + ".geo.json");
   }
}
