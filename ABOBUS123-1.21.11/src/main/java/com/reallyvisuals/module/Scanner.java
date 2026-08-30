package com.reallyvisuals.module;

import net.minecraft.client.MinecraftClient;

public class Scanner extends Module {
   public Module.NumberSetting radiusSetting = new Module.NumberSetting("Радиус", 25.0, 1.0, 200.0, 1.0);
   public Module.NumberSetting intervalSetting = new Module.NumberSetting("Интервал (сек)", 5.0, 1.0, 60.0, 1.0);
   private long lastScanTime = 0L;

   public Scanner() {
      super("Scanner", "Сканирование игроков через /near", Category.UTILITIES, false, true);
      this.addSetting(this.radiusSetting);
      this.addSetting(this.intervalSetting);
   }

   @Override
   public void onEnable() {
      this.lastScanTime = System.currentTimeMillis();
   }

   @Override
   public void onTick() {
      if (this.isEnabled()) {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client.player != null) {
            long now = System.currentTimeMillis();
            long delayMs;
            if (now - this.lastScanTime >= (delayMs = (long)(this.intervalSetting.value * 1000.0))) {
               int radius = (int)this.radiusSetting.value;
               client.player.networkHandler.sendChatMessage("/near " + radius);
               this.lastScanTime = now;
            }
         }
      }
   }
}
