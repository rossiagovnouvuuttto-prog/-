package com.reallyvisuals.module;

import net.minecraft.client.MinecraftClient;

public class AutoSpec extends Module {
   public Module.TextSetting messageSetting = new Module.TextSetting("Сообщение", "!Спек, читер");
   public Module.KeySetting keySetting = new Module.KeySetting("Кнопка", 0);

   public AutoSpec() {
      super("Auto Spec", "Отправка сообщения по нажатию кнопки", Category.UTILITIES, false, true);
      this.addSetting(this.messageSetting);
      this.addSetting(this.keySetting);
   }

   @Override
   public void onKeyPressed(int key) {
      if (this.isEnabled()) {
         if (this.keySetting.key != 0 && this.keySetting.key == key) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null && !this.messageSetting.value.isEmpty()) {
               client.player.networkHandler.sendChatMessage(this.messageSetting.value);
            }
         }
      }
   }
}
