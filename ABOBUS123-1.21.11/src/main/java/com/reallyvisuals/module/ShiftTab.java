package com.reallyvisuals.module;

import net.minecraft.client.MinecraftClient;

public class ShiftTab extends Module {
   public static ShiftTab INSTANCE;
   public Module.BooleanSetting onlyCritSetting = new Module.BooleanSetting("Только при крите", false);
   private boolean sneaking = false;
   private long sneakStartTime = 0L;

   public ShiftTab() {
      super("Shift Tab", "Автоматическое приседание при атаке", Category.UTILITIES, false, true);
      INSTANCE = this;
      this.addSetting(this.onlyCritSetting);
   }

   public void onAttack(boolean isCrit) {
      if (this.isEnabled()) {
         if (!this.onlyCritSetting.value || isCrit) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
               this.sneaking = true;
               this.sneakStartTime = System.currentTimeMillis();
               client.options.sneakKey.setPressed(true);
            }
         }
      }
   }

   @Override
   public void onTick() {
      if (this.sneaking && System.currentTimeMillis() - this.sneakStartTime >= 150L) {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client.options != null) {
            client.options.sneakKey.setPressed(false);
         }

         this.sneaking = false;
      }
   }
}
