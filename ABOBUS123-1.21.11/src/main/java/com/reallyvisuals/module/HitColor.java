package com.reallyvisuals.module;

import com.reallyvisuals.gui.ReallyVisualsScreen;

public class HitColor extends Module {
   public Module.ModeSetting mode = new Module.ModeSetting("Режим", new String[]{"Полностью", "Скин"}, "Полностью");
   public Module.BooleanSetting clientColor = new Module.BooleanSetting("Цвет клиента", false);

   public HitColor() {
      super("Hit Color", "Изменение цвета сущностей при получении урона", Category.VISUALS, false, true);
      this.customColor = -16448251;
      this.addSetting(this.mode);
      this.addSetting(this.clientColor);
   }

   public int getColor() {
      return this.clientColor.value ? ReallyVisualsScreen.clientColor : this.customColor;
   }
}
