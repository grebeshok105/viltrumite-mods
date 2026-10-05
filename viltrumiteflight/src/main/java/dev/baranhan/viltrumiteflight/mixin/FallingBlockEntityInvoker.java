package dev.baranhan.viltrumiteflight.mixin;

import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({FallingBlockEntity.class})
public interface FallingBlockEntityInvoker {
   @Invoker("<init>")
   static FallingBlockEntity invokeConstructor(Level level, double x, double y, double z, BlockState state) {
      throw new AssertionError("Bu hata asla f\u0131rlat\u0131lmamal\u0131. Mixin \u00e7al\u0131\u015fmad\u0131!");
   }
}
