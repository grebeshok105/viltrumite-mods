package dev.baranhan.viltrumitecore.client.anim.pose;

import net.minecraft.client.model.geom.ModelPart;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Twist-free third-person arm aiming at a direction (first used by
 * Iron Man, PR 19 iteration 2). Third person: an arm is turned so its axis
 * points along a model-space direction, swinging from the "arm straight
 * forward" pose (xRot -90) by the shortest arc, so the arm never rolls
 * around its own axis while the direction moves (the hanging pose is the
 * singular one for straight-up aims, forward is not). The result is blended
 * with what the part already shows by quaternion slerp, never by Euler
 * lerp, so a blend cannot flip through an axis (the same rule as
 * {@link PoseRig#slerp}). Apply after {@link PoseRig#finish}; first-person
 * arms use {@link FirstPersonArm}.
 */
public final class ArmAim {
   private static final Vector3f FORWARD = new Vector3f(0.0F, 0.0F, -1.0F);
   private static final Quaternionf ARM_FORWARD = new Quaternionf().rotationX((float)Math.toRadians(-90.0));

   private ArmAim() {
   }

   /** Look direction of the head in model space (head.xRot/yRot follow the camera, also in cruise flight). */
   public static Vector3f look(ModelPart head) {
      return new Quaternionf().rotationZYX(head.zRot, head.yRot, head.xRot).transform(new Vector3f(FORWARD));
   }

   /** Head up axis in model space (perpendicular to {@link #look}). */
   public static Vector3f up(ModelPart head) {
      return new Quaternionf().rotationZYX(head.zRot, head.yRot, head.xRot).transform(new Vector3f(0.0F, -1.0F, 0.0F));
   }

   /** Head right axis in model space (the player's right, model -X when not turned). */
   public static Vector3f right(ModelPart head) {
      return new Quaternionf().rotationZYX(head.zRot, head.yRot, head.xRot).transform(new Vector3f(-1.0F, 0.0F, 0.0F));
   }

   /** {@code dir} turned by {@code deg} towards {@code axis} (both unit, perpendicular). */
   public static Vector3f tilt(Vector3f dir, Vector3f axis, float deg) {
      float a = (float)Math.toRadians(deg);
      return new Vector3f(dir).mul((float)Math.cos(a)).add(new Vector3f(axis).mul((float)Math.sin(a))).normalize();
   }

   /** Orientation that points the arm axis along {@code dir}, rolled by {@code rollDeg} about the arm. */
   public static Quaternionf orientation(Vector3f dir, float rollDeg) {
      Vector3f d = new Vector3f(dir).normalize();
      Quaternionf swing;
      if (d.dot(FORWARD) < -0.9999F) {
         swing = new Quaternionf().rotationY((float)Math.PI);
      } else {
         swing = new Quaternionf().rotationTo(FORWARD, d);
      }

      return swing.mul(ARM_FORWARD).rotateY((float)Math.toRadians(rollDeg));
   }

   /** Blends a part towards an orientation by {@code weight} (slerp) and writes its Euler angles back. */
   public static void apply(ModelPart part, Quaternionf target, float weight) {
      if (weight <= 0.001F) {
         return;
      }

      Quaternionf current = new Quaternionf().rotationZYX(part.zRot, part.yRot, part.xRot);
      if (current.dot(target) < 0.0F) {
         target = new Quaternionf(target).scale(-1.0F);
      }

      Quaternionf q = weight >= 0.999F ? new Quaternionf(target) : current.slerp(target, Math.min(1.0F, weight));
      Vector3f euler = q.normalize().getEulerAnglesZYX(new Vector3f());
      part.xRot = euler.x;
      part.yRot = euler.y;
      part.zRot = euler.z;
   }

   /** Points the arm along {@code dir} (model space). */
   public static void aim(ModelPart arm, Vector3f dir, float rollDeg, float weight) {
      apply(arm, orientation(dir, rollDeg), weight);
   }
}
