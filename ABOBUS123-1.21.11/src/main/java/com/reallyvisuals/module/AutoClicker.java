package com.reallyvisuals.module;

import com.reallyvisuals.mixin.accessor.MinecraftClientAccessor;
import net.minecraft.client.MinecraftClient;

public class AutoClicker extends Module {
   public Module.NumberSetting delay = new Module.NumberSetting("Задержка (тики)", 10.0, 1.0, 100.0, 1.0);
   public Module.ModeSetting button = new Module.ModeSetting("Кнопка мыши", new String[]{"Левая", "Правая"}, "Левая");
   private int ticksPassed = 0;

   public AutoClicker() {
      super("Auto Clicker", "Автокликер", Category.UTILITIES, false, true);
      this.addSetting(this.delay);
      this.addSetting(this.button);
   }

   @Override
   public void onTick() {
      if (this.isEnabled()) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.player != null && mc.world != null && !mc.player.isUsingItem() && mc.currentScreen == null) {
            if (this.ticksPassed > 0) {
               this.ticksPassed--;
            } else {
               if (this.button.value.equals("Левая")) {
                  ((MinecraftClientAccessor)mc).invokeDoAttack();
               } else {
                  ((MinecraftClientAccessor)mc).invokeDoItemUse();
               }

               this.ticksPassed = (int)this.delay.value;
            }
         }
      }
   }

   @Override
   public void setEnabled(boolean enabled) {
      super.setEnabled(enabled);
      this.ticksPassed = 0;
   }
}
