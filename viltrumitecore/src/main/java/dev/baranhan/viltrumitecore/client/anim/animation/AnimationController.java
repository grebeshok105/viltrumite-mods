package dev.baranhan.viltrumitecore.client.anim.animation;

import dev.baranhan.viltrumitecore.client.anim.geo.BakedGeoModel;
import dev.baranhan.viltrumitecore.client.anim.geo.GeoBone;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class AnimationController {
   private static final int ROT_X = 0;
   private static final int ROT_Y = 1;
   private static final int ROT_Z = 2;
   private static final int POS_X = 3;
   private static final int POS_Y = 4;
   private static final int POS_Z = 5;
   private static final int SCL_X = 6;
   private static final int SCL_Y = 7;
   private static final int SCL_Z = 8;
   private final String name;
   private Animation current;
   private double startTime = Double.NaN;
   private float transitionTicks = 0.0F;
   private final Map<String, float[]> snapshot = new HashMap<>();
   private boolean snapshotPending = false;
   private Consumer<Animation.Event> eventListener;
   private double lastEventSeconds = -1.0;

   public AnimationController(String name) {
      this.name = name;
   }

   public String name() {
      return this.name;
   }

   public Animation currentAnimation() {
      return this.current;
   }

   public boolean isPlaying(String animationName) {
      return this.current != null && this.current.name().equals(animationName);
   }

   public AnimationController transitionLength(float ticks) {
      this.transitionTicks = Math.max(0.0F, ticks);
      return this;
   }

   public AnimationController onEvent(Consumer<Animation.Event> listener) {
      this.eventListener = listener;
      return this;
   }

   public void play(Animation animation) {
      if (animation == null) {
         this.stop();
      } else if (this.current == null || !this.current.name().equals(animation.name())) {
         this.beginNew(animation);
      }
   }

   public void restart(Animation animation) {
      if (animation == null) {
         this.stop();
      } else {
         this.beginNew(animation);
      }
   }

   public void stop() {
      this.current = null;
      this.startTime = Double.NaN;
      this.snapshotPending = false;
      this.lastEventSeconds = -1.0;
   }

   private void beginNew(Animation animation) {
      this.current = animation;
      this.startTime = Double.NaN;
      this.snapshotPending = this.transitionTicks > 0.0F;
      this.lastEventSeconds = -1.0;
   }

   public boolean isFinished(double seekTime) {
      if (this.current == null || this.current.loopType() != Animation.LoopType.PLAY_ONCE) {
         return false;
      } else {
         return Double.isNaN(this.startTime) ? false : (seekTime - this.startTime) / 20.0 >= this.current.lengthSeconds();
      }
   }

   public double elapsedSeconds(double seekTime) {
      return Double.isNaN(this.startTime) ? 0.0 : (seekTime - this.startTime) / 20.0;
   }

   public void apply(BakedGeoModel model, double seekTime) {
      if (this.snapshotPending) {
         this.captureSnapshot(model);
         this.snapshotPending = false;
      }

      model.resetBones();
      if (this.current != null) {
         if (Double.isNaN(this.startTime)) {
            this.startTime = seekTime;
         }

         double elapsedTicks = Math.max(0.0, seekTime - this.startTime);
         double elapsed = elapsedTicks / 20.0;
         double length = this.current.lengthSeconds();

         double t = switch (this.current.loopType()) {
            case LOOP -> length <= 0.0 ? 0.0 : elapsed % length;
            case HOLD_ON_LAST_FRAME, PLAY_ONCE -> Math.min(elapsed, length);
            default -> elapsed;
         };
         this.fireEvents(t, this.current.loopType() == Animation.LoopType.LOOP);
         float blend = 1.0F;
         if (this.transitionTicks > 0.0F && elapsedTicks < (double)this.transitionTicks) {
            blend = (float)(elapsedTicks / (double)this.transitionTicks);
         }

         for (Animation.BoneAnimation ba : this.current.boneAnimations().values()) {
            GeoBone bone = model.getBone(ba.boneName());
            if (bone != null) {
               float rx = ba.rotX().sample(t);
               float ry = ba.rotY().sample(t);
               float rz = ba.rotZ().sample(t);
               float px = ba.posX().sample(t);
               float py = ba.posY().sample(t);
               float pz = ba.posZ().sample(t);
               float sx = ba.scaleX().sample(t);
               float sy = ba.scaleY().sample(t);
               float sz = ba.scaleZ().sample(t);
               float targetRotX = bone.initRotX + rx;
               float targetRotY = bone.initRotY + ry;
               float targetRotZ = bone.initRotZ + rz;
               if (blend < 1.0F) {
                  float[] from = this.snapshot.get(bone.name);
                  if (from != null) {
                     targetRotX = lerp(from[0], targetRotX, blend);
                     targetRotY = lerp(from[1], targetRotY, blend);
                     targetRotZ = lerp(from[2], targetRotZ, blend);
                     px = lerp(from[3], px, blend);
                     py = lerp(from[4], py, blend);
                     pz = lerp(from[5], pz, blend);
                     sx = lerp(from[6], sx, blend);
                     sy = lerp(from[7], sy, blend);
                     sz = lerp(from[8], sz, blend);
                  }
               }

               bone.rotX = targetRotX;
               bone.rotY = targetRotY;
               bone.rotZ = targetRotZ;
               bone.posX = px;
               bone.posY = py;
               bone.posZ = pz;
               bone.scaleX = sx;
               bone.scaleY = sy;
               bone.scaleZ = sz;
            }
         }
      }
   }

   private void captureSnapshot(BakedGeoModel model) {
      this.snapshot.clear();

      for (GeoBone bone : model.allBones()) {
         this.snapshot.put(bone.name, new float[]{bone.rotX, bone.rotY, bone.rotZ, bone.posX, bone.posY, bone.posZ, bone.scaleX, bone.scaleY, bone.scaleZ});
      }
   }

   private void fireEvents(double t, boolean looping) {
      if (this.eventListener != null && !this.current.events().isEmpty()) {
         if (looping && t < this.lastEventSeconds) {
            this.lastEventSeconds = -1.0;
         }

         for (Animation.Event event : this.current.events()) {
            if (event.time() > this.lastEventSeconds && event.time() <= t) {
               this.eventListener.accept(event);
            }
         }

         this.lastEventSeconds = t;
      }
   }

   private static float lerp(float from, float to, float f) {
      return from + (to - from) * f;
   }
}
