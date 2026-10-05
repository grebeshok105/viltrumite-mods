package dev.baranhan.viltrumitecore.client.anim.animation;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public record Animation(
   String name, double lengthSeconds, Animation.LoopType loopType, Map<String, Animation.BoneAnimation> boneAnimations, List<Animation.Event> events
) {
   public Animation(
      String name, double lengthSeconds, Animation.LoopType loopType, Map<String, Animation.BoneAnimation> boneAnimations, List<Animation.Event> events
   ) {
      boneAnimations = Collections.unmodifiableMap(boneAnimations);
      events = Collections.unmodifiableList(events);
      this.name = name;
      this.lengthSeconds = lengthSeconds;
      this.loopType = loopType;
      this.boneAnimations = boneAnimations;
      this.events = events;
   }

   public static record BoneAnimation(
      String boneName,
      KeyframeStack rotX,
      KeyframeStack rotY,
      KeyframeStack rotZ,
      KeyframeStack posX,
      KeyframeStack posY,
      KeyframeStack posZ,
      KeyframeStack scaleX,
      KeyframeStack scaleY,
      KeyframeStack scaleZ
   ) {
   }

   public static record Event(double time, Animation.Event.Type type, String data) {
      public static enum Type {
         SOUND,
         PARTICLE,
         INSTRUCTION;
      }
   }

   public static enum LoopType {
      LOOP,
      PLAY_ONCE,
      HOLD_ON_LAST_FRAME;
   }
}
