package dev.baranhan.viltrumitecore.mixin;

import java.util.UUID;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Vanilla homing bullet target, for Iron Man countermeasures (no public setter in 1.20.1). */
@Mixin(ShulkerBullet.class)
public interface ShulkerBulletAccessor {
   @Accessor("finalTarget")
   Entity viltrumitecore$getFinalTarget();

   @Accessor("finalTarget")
   void viltrumitecore$setFinalTarget(Entity target);

   @Accessor("targetId")
   void viltrumitecore$setTargetId(UUID id);
}
