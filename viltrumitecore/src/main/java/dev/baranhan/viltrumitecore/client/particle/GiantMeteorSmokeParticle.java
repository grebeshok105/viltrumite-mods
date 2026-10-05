package dev.baranhan.viltrumitecore.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public class GiantMeteorSmokeParticle extends TextureSheetParticle {
   protected GiantMeteorSmokeParticle(ClientLevel level, double x, double y, double z, double speedX, double speedY, double speedZ, SpriteSet spriteSet) {
      super(level, x, y - 11.5, z, speedX, speedY, speedZ);
      this.xd = speedX;
      this.yd = speedY;
      this.zd = speedZ;
      this.quadSize = 6.0F + this.random.nextFloat() * 2.0F;
      this.lifetime = 200 + this.random.nextInt(100);
      this.gravity = -0.05F;
      this.friction = 0.96F;
      this.hasPhysics = false;
      float colorTint = 0.2F + this.random.nextFloat() * 0.1F;
      this.rCol = colorTint;
      this.gCol = colorTint;
      this.bCol = colorTint;
      this.pickSprite(spriteSet);
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
   }

   public float getQuadSize(float partialTick) {
      float ageRatio = ((float)this.age + partialTick) / (float)this.lifetime;
      return this.quadSize * (1.0F + ageRatio * 2.0F);
   }

   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet pSprites) {
         this.sprites = pSprites;
      }

      public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
         return new GiantMeteorSmokeParticle(level, x, y, z, dx, dy, dz, this.sprites);
      }
   }
}
