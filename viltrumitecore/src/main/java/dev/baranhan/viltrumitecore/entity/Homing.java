package dev.baranhan.viltrumitecore.entity;

import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;

/** A homing projectile; countermeasures (Stage 3) retarget or drop it. */
public interface Homing {
   void retarget(@Nullable Entity target);

   @Nullable
   Entity homingTarget();
}
