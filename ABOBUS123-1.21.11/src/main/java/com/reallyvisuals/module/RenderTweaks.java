package com.reallyvisuals.module;

public class RenderTweaks extends Module {
   public static final String[] TWEAK_OPTIONS = new String[]{
      "Погода", "Тряска урона", "Оверлей огня", "Анимация тотема", "Скорборд", "Боссбар", "Черные сердца", "Пузырьки воздуха", "Свечение игроков"
   };
   public Module.MultiSelectSetting tweaks = new Module.MultiSelectSetting(
      "Твики", TWEAK_OPTIONS, new String[]{"Погода", "Тряска урона", "Оверлей огня", "Анимация тотема", "Черные сердца", "Пузырьки воздуха"}
   );

   public RenderTweaks() {
      super("Render Tweaks", "Отключение различных элементов рендеринга", Category.VISUALS, false, true);
      this.addSetting(this.tweaks);
   }
}
