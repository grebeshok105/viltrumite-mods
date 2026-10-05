package dev.baranhan.viltrumitecore.particle;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ViltrumiteParticles {
   public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, "viltrumitecore");
   public static final RegistryObject<SimpleParticleType> GIANT_METEOR_SMOKE = PARTICLES.register("giant_meteor_smoke", () -> new SimpleParticleType(true));

   public static void register(IEventBus eventBus) {
      PARTICLES.register(eventBus);
   }
}
