package com.reallyvisuals.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Hand;

public class CustomHand extends Module {
   public Module.ModeSetting animationMode = new Module.ModeSetting(
      "Анимация",
      new String[]{
         "Без анимации", "Стандартная", "Наклон", "Взмах", "Вращение", "Увеличение", "Уменьшение", "Растяжение", "Пружина", "Удар", "Разрез", "Рывок"
      },
      "Без анимации"
   );
   public Module.NumberSetting animationSpeed = new Module.NumberSetting("Скорость анимации", 6.0, 1.0, 20.0, 1.0);
   public Module.NumberSetting swingStrength = new Module.NumberSetting("Сила размаха", 5.0, 1.0, 10.0, 0.5);
   public Module.BooleanSetting preview = new Module.BooleanSetting("Превью", false);
   public Module.NumberSetting mainHandScale = new Module.NumberSetting("Размер главной", 1.0, 0.5, 2.0, 0.05);
   public Module.NumberSetting mainHandX = new Module.NumberSetting("Главная X", 0.0, -2.0, 2.0, 0.05);
   public Module.NumberSetting mainHandY = new Module.NumberSetting("Главная Y", 0.0, -2.0, 2.0, 0.05);
   public Module.NumberSetting mainHandZ = new Module.NumberSetting("Главная Z", 0.0, -2.0, 2.0, 0.05);
   public Module.NumberSetting offHandScale = new Module.NumberSetting("Размер второй", 1.0, 0.5, 2.0, 0.05);
   public Module.NumberSetting offHandX = new Module.NumberSetting("Вторая X", 0.0, -2.0, 2.0, 0.05);
   public Module.NumberSetting offHandY = new Module.NumberSetting("Вторая Y", 0.0, -2.0, 2.0, 0.05);
   public Module.NumberSetting offHandZ = new Module.NumberSetting("Вторая Z", 0.0, -2.0, 2.0, 0.05);
   private int previewTicks = 0;

   public CustomHand() {
      super("Custom Hand", "Настройка отображения и анимаций руки", Category.VISUALS, false, true);
      this.addSetting(this.animationMode);
      this.addSetting(this.animationSpeed);
      this.addSetting(this.swingStrength);
      this.addSetting(this.preview);
      this.addSetting(this.mainHandScale);
      this.addSetting(this.mainHandX);
      this.addSetting(this.mainHandY);
      this.addSetting(this.mainHandZ);
      this.addSetting(this.offHandScale);
      this.addSetting(this.offHandX);
      this.addSetting(this.offHandY);
      this.addSetting(this.offHandZ);
   }

   public void tickPreview() {
      if (this.isEnabled() && this.preview.value) {
         this.previewTicks++;
         if (this.previewTicks >= 60) {
            this.previewTicks = 0;
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) {
               mc.player.swingHand(Hand.MAIN_HAND);
            }
         }
      } else {
         this.previewTicks = 0;
      }
   }
}
