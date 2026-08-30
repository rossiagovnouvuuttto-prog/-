package com.reallyvisuals.module;

import com.reallyvisuals.gui.ReallyVisualsScreen;

public class EntityBoxes extends Module {
   public Module.ModeSetting style = new Module.ModeSetting("Стиль контура", new String[]{"Полный", "Уголки"}, "Полный");
   public Module.BooleanSetting lookVector = new Module.BooleanSetting("Направление взгляда", true);
   public Module.NumberSetting rayLength = new Module.NumberSetting("Длина луча", 2.0, 0.5, 10.0, 0.5);
   public Module.BooleanSetting fill = new Module.BooleanSetting("Заливка", true);
   public Module.NumberSetting fillOpacity = new Module.NumberSetting("Прозрачность заливки", 0.25, 0.05, 1.0, 0.05);
   public Module.BooleanSetting clientColor = new Module.BooleanSetting("Цвет клиента", true);

   public EntityBoxes() {
      super("Entity Boxes", "Кастомизация границ сущностей", Category.VISUALS, false, true);
      this.customColor = -38400;
      this.addSetting(this.style);
      this.addSetting(this.lookVector);
      this.addSetting(this.rayLength);
      this.addSetting(this.fill);
      this.addSetting(this.fillOpacity);
      this.addSetting(this.clientColor);
   }

   public int getColor() {
      return this.clientColor.value ? ReallyVisualsScreen.clientColor : this.customColor;
   }
}
