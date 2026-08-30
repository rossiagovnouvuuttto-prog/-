package com.reallyvisuals.module;

public class Animations extends Module {
   public Module.BooleanSetting tabAnim = new Module.BooleanSetting("Анимация таба", true);
   public Module.NumberSetting tabDuration = new Module.NumberSetting("Длительность таба", 200.0, 50.0, 500.0, 10.0);
   public Module.BooleanSetting chatAnim = new Module.BooleanSetting("Анимация чата", true);
   public Module.NumberSetting chatDuration = new Module.NumberSetting("Длительность чата", 200.0, 50.0, 500.0, 10.0);
   public Module.BooleanSetting hotbarAnim = new Module.BooleanSetting("Анимация хотбара", true);
   public Module.NumberSetting hotbarDuration = new Module.NumberSetting("Длительность хотбара", 100.0, 50.0, 500.0, 10.0);
   public Module.BooleanSetting cameraAnim = new Module.BooleanSetting("Анимация перспективы", true);
   public Module.NumberSetting cameraDuration = new Module.NumberSetting("Длительность перспективы", 300.0, 50.0, 1000.0, 10.0);

   public Animations() {
      super("Animations", "Анимации интерфейса", Category.VISUALS, false, true);
      this.addSetting(this.tabAnim);
      this.addSetting(this.tabDuration);
      this.addSetting(this.chatAnim);
      this.addSetting(this.chatDuration);
      this.addSetting(this.hotbarAnim);
      this.addSetting(this.hotbarDuration);
   }
}
