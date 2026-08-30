package com.reallyvisuals.module;

public class ChunkAnimator extends Module {
   public static final AnimationHandler animationHandler = new AnimationHandler();
   public Module.ModeSetting mode = new Module.ModeSetting("Режим", new String[]{"Снизу", "Сверху", "От горизонта", "Сбоку", "Сбоку (альт.)"}, "Снизу");
   public Module.ModeSetting easing = new Module.ModeSetting("Плавность", new String[]{"Sine", "Linear", "Quad", "Cubic", "Quart"}, "Sine");
   public Module.NumberSetting duration = new Module.NumberSetting("Длительность (мс)", 1000.0, 100.0, 3000.0, 50.0);
   public Module.BooleanSetting dontAnimateNear = new Module.BooleanSetting("Не анимировать рядом", false);

   public ChunkAnimator() {
      super("Chunk Animator", "Плавная анимация появления чанков", Category.VISUALS, false, true);
      this.addSetting(this.mode);
      this.addSetting(this.easing);
      this.addSetting(this.duration);
      this.addSetting(this.dontAnimateNear);
   }

   @Override
   public void setEnabled(boolean enabled) {
      super.setEnabled(enabled);
      animationHandler.clear();
   }
}
