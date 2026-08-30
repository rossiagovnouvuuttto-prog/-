package com.reallyvisuals.module;

public class TimeChanger extends Module {
   public Module.ModeSetting timeMode = new Module.ModeSetting("Время суток", new String[]{"День", "Рассвет", "Закат", "Ночь", "Полночь"}, "День");

   public TimeChanger() {
      super("Time Changer", "Изменение времени суток в мире", Category.VISUALS, false, true);
      this.addSetting(this.timeMode);
   }

   public long getTime() {
      switch (this.timeMode.value) {
         case "Рассвет":
            return 23000L;
         case "Закат":
            return 12000L;
         case "Ночь":
            return 15000L;
         case "Полночь":
            return 18000L;
         default:
            return 1000L;
      }
   }
}
