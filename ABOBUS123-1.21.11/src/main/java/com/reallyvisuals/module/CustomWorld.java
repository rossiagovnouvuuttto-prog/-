package com.reallyvisuals.module;

import com.reallyvisuals.gui.ReallyVisualsScreen;

public class CustomWorld extends Module {
   public Module.BooleanSetting fog = new Module.BooleanSetting("Туман", true);
   public Module.NumberSetting distanceStart = new Module.NumberSetting("Дистанция старт", 10.0, 0.0, 100.0, 1.0);
   public Module.NumberSetting distanceEnd = new Module.NumberSetting("Дистанция конец", 50.0, 5.0, 200.0, 1.0);
   public Module.BooleanSetting sky = new Module.BooleanSetting("Небо", true);
   public Module.BooleanSetting clientColor = new Module.BooleanSetting("Цвет клиента", true);

   public CustomWorld() {
      super("Custom World", "Кастомизация тумана и неба", Category.VISUALS, false, true);
      this.customColor = -38400;
      this.addSetting(this.fog);
      this.addSetting(this.distanceStart);
      this.addSetting(this.distanceEnd);
      this.addSetting(this.sky);
      this.addSetting(this.clientColor);
   }

   public int getColor() {
      return this.clientColor.value ? ReallyVisualsScreen.clientColor : this.customColor;
   }
}
