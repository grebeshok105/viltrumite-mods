package dev.baranhan.viltrumiteflight.client;

public class PoseDataManager {
   public static class FP {
      public static float translateX = 0.0F;
      public static float translateY = 0.0F;
      public static float translateZ = 0.0F;
      public static float rotateX = 0.0F;
      public static float rotateY = 0.0F;
      public static float rotateZ = 0.0F;

      public static void reset() {
         translateX = 0.0F;
         translateY = 0.0F;
         translateZ = 0.0F;
         rotateX = 0.0F;
         rotateY = 0.0F;
         rotateZ = 0.0F;
      }
   }

   public static class PartTransform {
      public float pitch;
      public float yaw;
      public float roll;
      public float x;
      public float y;
      public float z;

      public PartTransform(float p, float y, float r, float x, float posY, float z) {
         this.pitch = p;
         this.yaw = y;
         this.roll = r;
         this.x = x;
         this.y = posY;
         this.z = z;
      }

      public void reset() {
         this.pitch = 0.0F;
         this.yaw = 0.0F;
         this.roll = 0.0F;
         this.x = 0.0F;
         this.y = 0.0F;
         this.z = 0.0F;
      }
   }

   public static class TP {
      public static PoseDataManager.PartTransform rightArm = new PoseDataManager.PartTransform(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      public static PoseDataManager.PartTransform leftArm = new PoseDataManager.PartTransform(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      public static PoseDataManager.PartTransform head = new PoseDataManager.PartTransform(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      public static PoseDataManager.PartTransform rightLeg = new PoseDataManager.PartTransform(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      public static PoseDataManager.PartTransform leftLeg = new PoseDataManager.PartTransform(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      public static PoseDataManager.PartTransform body = new PoseDataManager.PartTransform(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      public static PoseDataManager.PartTransform cape = new PoseDataManager.PartTransform(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

      public static void resetAll() {
         rightArm.reset();
         leftArm.reset();
         head.reset();
         rightLeg.reset();
         leftLeg.reset();
         body.reset();
         cape.reset();
      }
   }
}
