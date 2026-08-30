package com.reallyvisuals.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;

public class AspectRatio extends Module {
   public Module.ModeSetting presets = new Module.ModeSetting("Пресеты", new String[]{"16:9", "4:3", "21:9", "1:1", "Custom"}, "16:9");
   public Module.NumberSetting customRatio = new Module.NumberSetting("Свое значение", 1.78, 0.5, 3.0, 0.01);

   public AspectRatio() {
      super("Aspect Ratio", "Изменение соотношения сторон экрана", Category.VISUALS, false, true);
      this.addSetting(this.presets);
      this.addSetting(this.customRatio);
   }

   public float getRatio() {
      switch (this.presets.value) {
         case "4:3":
            return 1.3333334F;
         case "21:9":
            return 2.3333333F;
         case "1:1":
            return 1.0F;
         case "Custom":
            return (float)this.customRatio.value;
         default:
            Window window = MinecraftClient.getInstance().getWindow();
            return (float)window.getFramebufferWidth() / window.getFramebufferHeight();
      }
   }
}
