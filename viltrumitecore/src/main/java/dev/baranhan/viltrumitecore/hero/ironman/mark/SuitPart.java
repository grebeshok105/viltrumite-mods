package dev.baranhan.viltrumitecore.hero.ironman.mark;

import java.util.List;

/**
 * One flying piece of a mark (spec §12.4). A part covers a vertical slice
 * (0 = top, 1 = bottom) of one body bone, the front, back or all around.
 * The order of a set is the delivery order: legs → arms → chest → back → helmet.
 * The client draws a piece as that slice of the mark skin; the server only
 * counts parts (bit {@code index} in the present-parts mask).
 */
public record SuitPart(String name, Bone bone, float from, float to, Side side) {
   public enum Bone {
      HEAD,
      BODY,
      RIGHT_ARM,
      LEFT_ARM,
      RIGHT_LEG,
      LEFT_LEG
   }

   public enum Side {
      ALL,
      FRONT,
      BACK
   }

   private static SuitPart p(String name, Bone bone, float from, float to, Side side) {
      return new SuitPart(name, bone, from, to, side);
   }

   /** 7 parts (Mark 15). */
   public static final List<SuitPart> SEVEN = List.of(
      p("left_leg", Bone.LEFT_LEG, 0.0F, 1.0F, Side.ALL),
      p("right_leg", Bone.RIGHT_LEG, 0.0F, 1.0F, Side.ALL),
      p("left_arm", Bone.LEFT_ARM, 0.0F, 1.0F, Side.ALL),
      p("right_arm", Bone.RIGHT_ARM, 0.0F, 1.0F, Side.ALL),
      p("chest", Bone.BODY, 0.0F, 1.0F, Side.FRONT),
      p("back", Bone.BODY, 0.0F, 1.0F, Side.BACK),
      p("helmet", Bone.HEAD, 0.0F, 1.0F, Side.ALL)
   );
   /** 9 parts (Satsu each_part style): shoulders separate from the arms. */
   public static final List<SuitPart> NINE = List.of(
      p("left_leg", Bone.LEFT_LEG, 0.0F, 1.0F, Side.ALL),
      p("right_leg", Bone.RIGHT_LEG, 0.0F, 1.0F, Side.ALL),
      p("left_arm", Bone.LEFT_ARM, 0.25F, 1.0F, Side.ALL),
      p("right_arm", Bone.RIGHT_ARM, 0.25F, 1.0F, Side.ALL),
      p("chest", Bone.BODY, 0.0F, 1.0F, Side.FRONT),
      p("left_shoulder", Bone.LEFT_ARM, 0.0F, 0.25F, Side.ALL),
      p("right_shoulder", Bone.RIGHT_ARM, 0.0F, 0.25F, Side.ALL),
      p("back", Bone.BODY, 0.0F, 1.0F, Side.BACK),
      p("helmet", Bone.HEAD, 0.0F, 1.0F, Side.ALL)
   );
   /** 14 parts (Mark 42, modular). */
   public static final List<SuitPart> FOURTEEN = List.of(
      p("left_boot", Bone.LEFT_LEG, 0.5F, 1.0F, Side.ALL),
      p("right_boot", Bone.RIGHT_LEG, 0.5F, 1.0F, Side.ALL),
      p("left_thigh", Bone.LEFT_LEG, 0.0F, 0.5F, Side.ALL),
      p("right_thigh", Bone.RIGHT_LEG, 0.0F, 0.5F, Side.ALL),
      p("left_gauntlet", Bone.LEFT_ARM, 0.5F, 1.0F, Side.ALL),
      p("right_gauntlet", Bone.RIGHT_ARM, 0.5F, 1.0F, Side.ALL),
      p("left_upper_arm", Bone.LEFT_ARM, 0.25F, 0.5F, Side.ALL),
      p("right_upper_arm", Bone.RIGHT_ARM, 0.25F, 0.5F, Side.ALL),
      p("left_shoulder", Bone.LEFT_ARM, 0.0F, 0.25F, Side.ALL),
      p("right_shoulder", Bone.RIGHT_ARM, 0.0F, 0.25F, Side.ALL),
      p("abdomen", Bone.BODY, 0.55F, 1.0F, Side.FRONT),
      p("chest", Bone.BODY, 0.0F, 0.55F, Side.FRONT),
      p("back", Bone.BODY, 0.0F, 1.0F, Side.BACK),
      p("helmet", Bone.HEAD, 0.0F, 1.0F, Side.ALL)
   );

   public static List<SuitPart> of(MarkId mark) {
      return switch (MarkSpec.of(mark).parts()) {
         case 7 -> SEVEN;
         case 14 -> FOURTEEN;
         default -> NINE;
      };
   }

   /** Mask with every part of the set present. */
   public static int fullMask(MarkId mark) {
      return (1 << of(mark).size()) - 1;
   }

   /** Index of a named part in the mark's set, or -1. */
   public static int indexOf(MarkId mark, String name) {
      List<SuitPart> parts = of(mark);
      for (int i = 0; i < parts.size(); i++) {
         if (parts.get(i).name().equals(name)) {
            return i;
         }
      }

      return -1;
   }
}
