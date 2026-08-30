package com.reallyvisuals.module;

import java.util.ArrayList;

public class Crosshair extends Module {
   public Module.ModeSetting type = new Module.ModeSetting("Тип", new String[]{"Крест", "Круг", "Точка"}, "Крест");
   public Module.NumberSetting size = new Module.NumberSetting("Размер", 3.0, 1.0, 20.0, 0.5);
   public Module.NumberSetting thickness = new Module.NumberSetting("Толщина", 1.0, 0.5, 5.0, 0.5);
   public Module.NumberSetting gap = new Module.NumberSetting("Отступ", 0.0, 0.0, 15.0, 0.5);
   public boolean lineTop = true;
   public boolean lineBottom = true;
   public boolean lineLeft = true;
   public boolean lineRight = true;
   public Module.NumberSetting circleRadius = new Module.NumberSetting("Радиус круга", 5.0, 1.0, 20.0, 0.5);
   public Module.NumberSetting circleThickness = new Module.NumberSetting("Толщина круга", 1.0, 0.5, 5.0, 0.5);
   public Module.BooleanSetting outline = new Module.BooleanSetting("Обводка", true);
   public Module.BooleanSetting dot = new Module.BooleanSetting("Точка", false);
   public Module.NumberSetting dotSize = new Module.NumberSetting("Размер точки", 2.0, 0.5, 10.0, 0.5);
   public Module.NumberSetting rotation = new Module.NumberSetting("Поворот", 0.0, 0.0, 360.0, 5.0);
   public Module.BooleanSetting dynamic = new Module.BooleanSetting("Динамический", false);
   public Module.BooleanSetting targetColor = new Module.BooleanSetting("Менять цвет при наводке", true);
   public Module.BooleanSetting clientColor = new Module.BooleanSetting("Цвет клиента", false);
   public int customColor = -13369345;
   public boolean linesDropdownOpen = false;

   public Crosshair() {
      super("Crosshair", "Настройка отображения прицела", Category.VISUALS, false, true);
      this.addSetting(this.type);
      this.addSetting(this.size);
      this.addSetting(this.thickness);
      this.addSetting(this.gap);
      this.addSetting(this.circleRadius);
      this.addSetting(this.circleThickness);
      this.addSetting(this.outline);
      this.addSetting(this.dot);
      this.addSetting(this.dotSize);
      this.addSetting(this.rotation);
      this.addSetting(this.dynamic);
      this.addSetting(this.targetColor);
      this.addSetting(this.clientColor);
   }

   public String getLinesString() {
      ArrayList<String> list = new ArrayList<>();
      if (this.lineTop) {
         list.add("Верх");
      }

      if (this.lineBottom) {
         list.add("Низ");
      }

      if (this.lineLeft) {
         list.add("Лево");
      }

      if (this.lineRight) {
         list.add("Право");
      }

      return list.isEmpty() ? "Ничего" : String.join(", ", list);
   }
}
