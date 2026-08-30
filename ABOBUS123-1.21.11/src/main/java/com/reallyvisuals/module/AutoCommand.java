package com.reallyvisuals.module;

import net.minecraft.client.MinecraftClient;

public class AutoCommand extends Module {
   public Module.MultiSelectSetting commandsSetting = new Module.MultiSelectSetting(
      "Команды", new String[]{"/heal", "/fix all"}, new String[]{"/heal", "/fix all"}
   );
   public Module.NumberSetting healInterval = new Module.NumberSetting("Интервал /heal (сек)", 30.0, 1.0, 120.0, 1.0);
   public Module.NumberSetting fixAllInterval = new Module.NumberSetting("Интервал /fix all (сек)", 60.0, 1.0, 120.0, 1.0);
   private long lastHeal;
   private long lastFixAll;

   public AutoCommand() {
      super("Auto Command", "Автоотправка команд с заданным интервалом", Category.UTILITIES, false, true);
      this.addSetting(this.commandsSetting);
      this.addSetting(this.healInterval.visible(() -> this.commandsSetting.isSelected("/heal")));
      this.addSetting(this.fixAllInterval.visible(() -> this.commandsSetting.isSelected("/fix all")));
   }

   @Override
   public void onEnable() {
      long now = System.currentTimeMillis();
      this.lastHeal = now;
      this.lastFixAll = now;
   }

   @Override
   public void onTick() {
      if (!this.isEnabled()) return;
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player == null || mc.world == null || mc.currentScreen != null) return;
      long now = System.currentTimeMillis();
      if (this.commandsSetting.isSelected("/heal") && now - this.lastHeal >= (long)(this.healInterval.value * 1000.0)) {
         mc.player.networkHandler.sendChatMessage("/heal");
         this.lastHeal = now;
      }
      if (this.commandsSetting.isSelected("/fix all") && now - this.lastFixAll >= (long)(this.fixAllInterval.value * 1000.0)) {
         mc.player.networkHandler.sendChatMessage("/fix all");
         this.lastFixAll = now;
      }
   }
}
