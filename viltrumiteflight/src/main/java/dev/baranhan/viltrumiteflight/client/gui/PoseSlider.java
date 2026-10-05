package dev.baranhan.viltrumiteflight.client.gui;

import java.util.function.Consumer;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

public class PoseSlider extends AbstractSliderButton {
   private final float min;
   private final float max;
   private final String prefix;
   private final Consumer<Float> onValueChanged;

   public PoseSlider(int x, int y, int width, int height, String prefix, float min, float max, float currentValue, Consumer<Float> onValueChanged) {
      super(x, y, width, height, Component.empty(), (double)((currentValue - min) / (max - min)));
      this.min = min;
      this.max = max;
      this.prefix = prefix;
      this.onValueChanged = onValueChanged;
      this.updateMessage();
   }

   protected void updateMessage() {
      float actualValue = this.getActualValue();
      this.setMessage(Component.literal(this.prefix + String.format("%.2f", actualValue)));
   }

   protected void applyValue() {
      if (this.onValueChanged != null) {
         this.onValueChanged.accept(this.getActualValue());
      }
   }

   private float getActualValue() {
      return this.min + (float)this.value * (this.max - this.min);
   }
}
