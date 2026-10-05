package dev.baranhan.viltrumitecore.client.event;

import dev.baranhan.viltrumitecore.client.particle.GiantMeteorSmokeParticle;
import dev.baranhan.viltrumitecore.client.render.MeteorRenderer;
import dev.baranhan.viltrumitecore.entity.ViltrumiteEntities;
import dev.baranhan.viltrumitecore.particle.ViltrumiteParticles;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(
   modid = "viltrumitecore",
   bus = Bus.MOD,
   value = {Dist.CLIENT}
)
public class ClientModEvents {
   @SubscribeEvent
   public static void registerRenderers(RegisterRenderers event) {
      event.registerEntityRenderer((EntityType)ViltrumiteEntities.METEOR.get(), MeteorRenderer::new);
   }

   @SubscribeEvent
   public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
      event.registerSpriteSet((ParticleType)ViltrumiteParticles.GIANT_METEOR_SMOKE.get(), GiantMeteorSmokeParticle.Provider::new);
   }
}
