package dev.baranhan.viltrumitecore.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ViltrumiteEntities {
   public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "viltrumitecore");
   public static final RegistryObject<EntityType<MeteorEntity>> METEOR = ENTITIES.register(
      "meteor", () -> Builder.of(MeteorEntity::new, MobCategory.MISC).sized(3.0F, 3.0F).clientTrackingRange(10).updateInterval(1).build("meteor")
   );
   public static final RegistryObject<EntityType<BloodDropEntity>> BLOOD_DROP = ENTITIES.register(
      "blood_drop",
      () -> Builder.of(BloodDropEntity::new, MobCategory.MISC).sized(0.12F, 0.12F).clientTrackingRange(4).updateInterval(1).fireImmune().build("blood_drop")
   );

   public static void register(IEventBus eventBus) {
      ENTITIES.register(eventBus);
   }
}
