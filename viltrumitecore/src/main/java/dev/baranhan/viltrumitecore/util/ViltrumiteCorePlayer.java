package dev.baranhan.viltrumitecore.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public interface ViltrumiteCorePlayer {
   boolean isViltrumite();

   void setViltrumite(boolean var1);

   boolean hasChosenRace();

   void setChosenRace(boolean var1);

   boolean isDashing();

   void setDashing(boolean var1);

   void startDash();

   float getDashProgress(float var1);

   int getPunchTicks();

   void setPunchTicks(int var1);

   boolean isLeftArmPunch();

   void setLeftArmPunch(boolean var1);

   float getPunchStrength();

   void setPunchStrength(float var1);

   int getPunchCooldown();

   void setPunchCooldown(int var1);

   boolean isTryingToGrab();

   void setTryingToGrab(boolean var1);

   LivingEntity getGrabbedTarget();

   void setGrabbedTarget(LivingEntity var1);

   void releaseTarget();

   Vec3 getCalculatedHandPos();

   void setCalculatedHandPos(Vec3 var1);

   Vec3 getServerHandPos();

   void setServerHandPos(Vec3 var1);

   Vec3 getChopHandPos();

   void setChopHandPos(Vec3 var1);

   boolean isBlocking();

   void setBlocking(boolean var1);

   int getBlockCooldown();

   void setBlockCooldown(int var1);

   boolean isSuperSpeed();

   void setSuperSpeed(boolean var1);

   default boolean isViltrumiteLocal() {
      return false;
   }

   int getChopTicks();

   void setChopTicks(int var1);

   boolean isLeftChop();

   void setLeftChop(boolean var1);

   int getChopType();

   void setChopType(int var1);

   void setFirstPersonLocalHandPos(Vec3 var1);

   Vec3 getFirstPersonLocalHandPos();

   int getThunderclapTicks();

   void setThunderclapTicks(int var1);

   Vec3 getCalculatedHandOffset();

   void setCalculatedHandOffset(Vec3 var1);

   float getCloneScale();

   void setCloneScale(float var1);

   boolean isBarraging();

   void setBarraging(boolean var1);

   int getBarrageCooldown();

   void setBarrageCooldown(int var1);

   int getBarrageTicks();

   void setBarrageTicks(int var1);

   boolean isLeftBarrageArm();

   void setLeftBarrageArm(boolean var1);

   float getCalculatedHandPitch();

   void setCalculatedHandPitch(float var1);

   float getCalculatedHandYaw();

   void setCalculatedHandYaw(float var1);

   void setCalculatedBodyPitch(float var1);

   float getCalculatedBodyPitch();

   void setCalculatedBodyYaw(float var1);

   float getCalculatedBodyYaw();
}
