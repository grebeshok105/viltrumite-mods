package dev.baranhan.viltrumitecore.event;

import dev.baranhan.viltrumitecore.config.ViltrumiteCoreConfig;
import dev.baranhan.viltrumitecore.entity.BloodDropEntity;
import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(
   modid = "viltrumitecore",
   bus = Bus.FORGE
)
public class BloodEvents {
   private static final float BLEED_THRESHOLD = 0.8F;
   private static final int MAX_DROPS = 8;

   @SubscribeEvent
   public static void onLivingDamage(LivingDamageEvent event) {
      if (event.getEntity() instanceof Player player) {
         if (player.level() instanceof ServerLevel level) {
            if (ViltrumiteCoreConfig.INSTANCE.bloodEnabled) {
               float amount = event.getAmount();
               if (!(amount < 0.8F)) {
                  boolean var10000;
                  label36: {
                     if (player instanceof ViltrumiteCorePlayer core && core.isViltrumite()) {
                        var10000 = true;
                        break label36;
                     }

                     var10000 = false;
                  }

                  boolean viltrumite = var10000;
                  if (viltrumite || ViltrumiteCoreConfig.INSTANCE.shouldHumansBleed) {
                     Vec3 push = Vec3.ZERO;
                     Entity attacker = event.getSource().getEntity();
                     if (attacker != null) {
                        Vec3 away = player.position().subtract(attacker.position());
                        if (away.lengthSqr() > 1.0E-4) {
                           push = away.normalize();
                        }
                     }

                     int drops = Mth.clamp((int)(amount / 0.8F), 1, 8);
                     Vec3 origin = player.position().add(0.0, (double)player.getBbHeight() * 0.6, 0.0);
                     BloodDropEntity.burst(level, origin, push, drops, viltrumite);
                  }
               }
            }
         }
      }
   }
}
