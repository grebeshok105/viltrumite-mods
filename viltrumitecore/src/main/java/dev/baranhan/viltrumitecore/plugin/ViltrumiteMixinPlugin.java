package dev.baranhan.viltrumitecore.plugin;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class ViltrumiteMixinPlugin implements IMixinConfigPlugin {
   public void onLoad(String mixinPackage) {
   }

   public String getRefMapperConfig() {
      return null;
   }

   public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
      if (mixinClassName.endsWith("GeckoLibGrabRendererMixin")) {
         return this.getClass().getClassLoader().getResource("software/bernie/geckolib/renderer/GeoEntityRenderer.class") != null;
      } else if (mixinClassName.endsWith("MowziesRepelBypassMixin")) {
         return this.getClass().getClassLoader().getResource("com/bobmowzie/mowziesmobs/server/entity/MowzieEntity.class") != null;
      } else {
         return mixinClassName.endsWith("WroughtnautDamageBypassMixin")
            ? this.getClass().getClassLoader().getResource("com/bobmowzie/mowziesmobs/server/entity/wroughtnaut/EntityWroughtnaut.class") != null
            : true;
      }
   }

   public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
   }

   public List<String> getMixins() {
      return null;
   }

   public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }

   public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }
}
