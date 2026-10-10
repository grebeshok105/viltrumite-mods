package dev.baranhan.viltrumitecore.client.ironman.hulkbuster;

import net.minecraft.resources.ResourceLocation;

/** Mark 48 art paths (Sind Hulkbuster, converted by tools/assets/convert_ironman_sind.py; native 68 px tall). */
public final class HulkbusterAssets {
   public static final ResourceLocation BODY = loc("geo/ironman/hulkbuster/mark48.geo.json");
   public static final ResourceLocation FP_ARM = loc("geo/ironman/hulkbuster/fp_arm.geo.json");
   public static final ResourceLocation PARTS = loc("geo/ironman/hulkbuster/parts.geo.json");
   public static final ResourceLocation ANIMATIONS = loc("animations/ironman/hulkbuster/mark48.animation.json");
   public static final ResourceLocation TEXTURE = loc("textures/entity/ironman/hulkbuster.png");
   public static final ResourceLocation GLOW = loc("textures/entity/ironman/hulkbuster_glow.png");
   /** Jackhammer arm: replaces the left arm while the RMB tool is the jackhammer (own 114 px texture). */
   public static final ResourceLocation JACKHAMMER = loc("geo/ironman/hulkbuster/jackhammer.geo.json");
   public static final ResourceLocation JACKHAMMER_TEXTURE = loc("textures/entity/ironman/hulkbuster_jackhammer.png");
   public static final ResourceLocation JACKHAMMER_GLOW = loc("textures/entity/ironman/hulkbuster_jackhammer_glow.png");
   /** Thruster flames, posed with the body clips and drawn with the repulsor flame texture. */
   public static final ResourceLocation FIRE = loc("geo/ironman/hulkbuster/fire.geo.json");
   public static final ResourceLocation JACKHAMMER_FIRE = loc("geo/ironman/hulkbuster/jackhammer_fire.geo.json");
   /** Body bone of the normal left arm (hidden while the jackhammer arm is drawn). */
   public static final String LEFT_ARM_BONE = "pivotLeft";
   public static final String HEAD_BONE = "headBuster";
   /** Sind model units: 68 px tall, drawn at this factor times the body scale. */
   public static final float SIND_UNITS = 32.0F / 68.0F;

   private HulkbusterAssets() {
   }

   private static ResourceLocation loc(String path) {
      return new ResourceLocation("viltrumitecore", path);
   }
}
