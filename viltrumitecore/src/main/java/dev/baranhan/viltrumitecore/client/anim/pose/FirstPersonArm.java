package dev.baranhan.viltrumitecore.client.anim.pose;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Quaternionf;

/**
 * First-person arm pose in shoulder-pivot form (animation-system §5.1). A key
 * {@code {pitch, yaw, roll, x, y, z}} (degrees, view-space blocks, authored
 * for the right arm, mirrored for the left) turns the arm about the vanilla
 * first-person shoulder instead of the view origin, and keys blend on the
 * shortest arc. A half-weight pose is therefore the same arm halfway there,
 * never an arm swinging through the air. Timelines use the {@link PoseKeys}
 * rows; {@code under} rows mean the vanilla arm. Apply right after the arm's
 * pushPose in {@code renderArmWithItem}, like every FirstPerson*Mixin.
 * Solve keys from a wanted palm point and arm direction with
 * {@code tools/preview_fp_arm.py --solve}.
 */
public final class FirstPersonArm {
   /** Vanilla right shoulder in first-person view space at swing 0, equip 0 (tools/preview_fp_arm.py). */
   public static final float SHOULDER_X = 0.4787F;
   public static final float SHOULDER_Y = -0.8435F;
   public static final float SHOULDER_Z = -0.5281F;
   private static final Quaternionf A = new Quaternionf();
   private static final Quaternionf B = new Quaternionf();
   private static final float[] OFFSET = new float[3];
   private final Quaternionf rotation = new Quaternionf();
   private float x;
   private float y;
   private float z;

   public FirstPersonArm reset() {
      this.rotation.identity();
      this.x = 0.0F;
      this.y = 0.0F;
      this.z = 0.0F;
      return this;
   }

   /** Blends a timeline sample over the current pose: later layers win, like absolute model keys. */
   public FirstPersonArm blend(float[][] keys, float elapsed, float weight) {
      if (weight <= 0.001F) {
         return this;
      }

      sample(keys, elapsed, A, OFFSET);
      float w = Math.min(1.0F, weight);
      this.rotation.slerp(A, w);
      this.x += (OFFSET[0] - this.x) * w;
      this.y += (OFFSET[1] - this.y) * w;
      this.z += (OFFSET[2] - this.z) * w;
      return this;
   }

   /** Blends one constant key over the current pose. */
   public FirstPersonArm blend(float[] key, float weight) {
      if (weight <= 0.001F) {
         return this;
      }

      euler(key[0], key[1], key[2], A);
      float w = Math.min(1.0F, weight);
      this.rotation.slerp(A, w);
      this.x += (key[3] - this.x) * w;
      this.y += (key[4] - this.y) * w;
      this.z += (key[5] - this.z) * w;
      return this;
   }

   /** Adds a turn (degrees) about the shoulder and an offset on top of the pose: recoil kicks, tremble. */
   public FirstPersonArm add(float pitch, float yaw, float roll, float dx, float dy, float dz) {
      euler(pitch, yaw, roll, A);
      this.rotation.premul(A);
      this.x += dx;
      this.y += dy;
      this.z += dz;
      return this;
   }

   public boolean idle() {
      return Math.abs(this.rotation.w) > 0.99999F && Math.abs(this.x) < 1.0E-5F && Math.abs(this.y) < 1.0E-5F && Math.abs(this.z) < 1.0E-5F;
   }

   /** translate(shoulder + offset) · rotation · translate(-shoulder), mirrored for the left arm ({@code side} -1). */
   public void apply(PoseStack poseStack, float side) {
      if (this.idle()) {
         return;
      }

      float px = SHOULDER_X * side;
      poseStack.translate(px + this.x * side, SHOULDER_Y + this.y, SHOULDER_Z + this.z);
      // Mirroring across the YZ plane keeps the X turn and negates the Y and Z turns.
      poseStack.mulPose(side < 0.0F ? new Quaternionf(this.rotation.x, -this.rotation.y, -this.rotation.z, this.rotation.w) : new Quaternionf(this.rotation));
      poseStack.translate(-px, -SHOULDER_Y, -SHOULDER_Z);
   }

   /** Timeline sample: smoothstep segments (PoseKeys.locate), shortest-arc rotation, linear offset. */
   static void sample(float[][] keys, float elapsed, Quaternionf rotation, float[] offset) {
      float at = PoseKeys.locate(keys, elapsed);
      int i = (int)at;
      float t = at - (float)i;
      row(keys[i], rotation, offset);
      if (t > 0.0F && i + 1 < keys.length) {
         float ax = offset[0];
         float ay = offset[1];
         float az = offset[2];
         row(keys[i + 1], B, offset);
         rotation.slerp(B, t);
         offset[0] = ax + (offset[0] - ax) * t;
         offset[1] = ay + (offset[1] - ay) * t;
         offset[2] = az + (offset[2] - az) * t;
      }
   }

   private static void row(float[] row, Quaternionf rotation, float[] offset) {
      if (PoseKeys.isUnder(row)) {
         rotation.identity();
         offset[0] = 0.0F;
         offset[1] = 0.0F;
         offset[2] = 0.0F;
         return;
      }

      euler(row[1], row[2], row[3], rotation);
      offset[0] = row[4];
      offset[1] = row[5];
      offset[2] = row[6];
   }

   /** X, then Y, then Z turn: the order of PoseRig.firstPersonRaw. */
   private static void euler(float pitch, float yaw, float roll, Quaternionf out) {
      out.identity().rotateX((float)Math.toRadians(pitch)).rotateY((float)Math.toRadians(yaw)).rotateZ((float)Math.toRadians(roll));
   }
}
