package dev.baranhan.viltrumitecore.client.ironman.mark;

import dev.baranhan.viltrumitecore.client.anim.geo.BakedGeoModel;
import dev.baranhan.viltrumitecore.client.anim.geo.GeoBone;
import dev.baranhan.viltrumitecore.client.anim.render.PlayerGeoLayer;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkId;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

/**
 * The marks' own Satsu 3D parts on a whole suit (spec §3.2): Mark 7 flaps,
 * Mark 17 heartbreaker chest, War Machine shoulder pads, Iron Heart plates.
 * Drawn with the raw Satsu suit texture: the baked skins carry the helmet in
 * the head rows, which these parts' UVs also read. Top-level bones are
 * lowercase with the empty-suit pivots, so the empty suit clip drives them too.
 */
public final class MarkExtras {
   /** Satsu flames_flaps clip, last frame (degrees, Bedrock signs): the Mark 7 missile flaps open (spec §3.1). */
   private static final float[][] FLAPS_OPEN = {
      {20.21F, 6.49F, 3.77F}, {20.21F, -6.49F, -3.77F}, {22.5F, -22.5F, 0.0F}, {27.5F, 15.0F, 0.0F}};
   private static final String[] FLAP_BONES = {"leftupflap", "rightupflap", "lowrightflap", "lowleftflap"};

   private MarkExtras() {
   }

   @Nullable
   public static ResourceLocation geo(MarkId mark) {
      return switch (mark) {
         case MARK_7, MARK_17, WAR_MACHINE_MK2, IRON_HEART_MK3 -> new ResourceLocation("viltrumitecore", "geo/ironman/marks/extras/" + mark.key() + ".geo.json");
         default -> null;
      };
   }

   public static ResourceLocation suit(MarkId mark) {
      return new ResourceLocation("viltrumitecore", "textures/entity/ironman/marks/" + mark.key() + "_suit.png");
   }

   public static ResourceLocation suitGlow(MarkId mark) {
      return new ResourceLocation("viltrumitecore", "textures/entity/ironman/marks/" + mark.key() + "_suit_glow.png");
   }

   public static List<PlayerGeoLayer.Pass> passes(MarkId mark) {
      return List.of(PlayerGeoLayer.Pass.cutout(suit(mark)), PlayerGeoLayer.Pass.glow(suitGlow(mark)));
   }

   /** The extras part of a whole mark, or null; {@code flaps} 0..1 opens the Mark 7 flaps. */
   @Nullable
   public static PlayerGeoLayer.Part part(MarkId mark, float flaps) {
      ResourceLocation geo = geo(mark);
      if (geo == null) {
         return null;
      }

      Consumer<BakedGeoModel> pose = mark == MarkId.MARK_7 && flaps > 0.0F ? model -> openFlaps(model, flaps) : null;
      return new PlayerGeoLayer.Part(geo, passes(mark), pose);
   }

   /** Same conversion as BakedGeoModel: X and Y turns negated, Z kept. */
   public static void openFlaps(BakedGeoModel model, float open) {
      for (int i = 0; i < FLAP_BONES.length; i++) {
         GeoBone bone = model.getBone(FLAP_BONES[i]);
         if (bone != null) {
            bone.rotX = bone.initRotX - (float)Math.toRadians(FLAPS_OPEN[i][0] * open);
            bone.rotY = bone.initRotY - (float)Math.toRadians(FLAPS_OPEN[i][1] * open);
            bone.rotZ = bone.initRotZ + (float)Math.toRadians(FLAPS_OPEN[i][2] * open);
         }
      }
   }
}
