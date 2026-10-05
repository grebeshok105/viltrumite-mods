package dev.baranhan.viltrumitecore.client.mixin;

import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({LocalPlayer.class})
public abstract class ClientPlayerEntityMixin implements ViltrumiteCorePlayer {
   @Override
   public boolean isViltrumiteLocal() {
      return true;
   }
}
