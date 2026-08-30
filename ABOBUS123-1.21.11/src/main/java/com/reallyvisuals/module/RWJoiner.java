package com.reallyvisuals.module;

import net.minecraft.client.MinecraftClient;

public class RWJoiner extends Module {
   public Module.NumberSetting griefNumber = new Module.NumberSetting("Номер грифа", 1.0, 1.0, 100.0, 1.0);

   public RWJoiner() {
      super("RW Joiner", "Автоматическое подключение к выбранному грифу ReallyWorld", Category.UTILITIES, false, true);
      this.addSetting(this.griefNumber);
   }

   @Override
   public void onEnable() {
      this.tryJoin();
   }

   @Override
   public void onTick() {
      if (this.isEnabled()) {
         this.tryJoin();
      }
   }

   private void tryJoin() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player != null) {
         int num = (int)this.griefNumber.value;
         // Keep both historical ReallyWorld command variants for compatibility.
         mc.player.networkHandler.sendChatMessage("/grief " + num);
         mc.player.networkHandler.sendChatMessage("/grief-" + num);
         this.setEnabled(false);
      }
   }
}
