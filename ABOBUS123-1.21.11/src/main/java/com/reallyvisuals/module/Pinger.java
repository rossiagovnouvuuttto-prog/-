package com.reallyvisuals.module;

import net.minecraft.text.Text;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;

public class Pinger extends Module {
   public Module.NumberSetting threshold = new Module.NumberSetting("Порог (%)", 20.0, 1.0, 50.0, 1.0);
   private long lastNotifyTime = 0L;

   public Pinger() {
      super("Pinger", "Уведомляет о низкой прочности брони", Category.UTILITIES, false, true);
      this.addSetting(this.threshold);
   }

   @Override
   public void onTick() {
      if (this.isEnabled()) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.player != null && mc.world != null) {
            if (System.currentTimeMillis() - this.lastNotifyTime >= 3000L) {
               for (int i = 36; i <= 39; i++) {
                  ItemStack stack = mc.player.getInventory().getStack(i);
                  int maxDamage;
                  if (!stack.isEmpty()
                     && stack.isDamageable()
                     && (double)((maxDamage = stack.getMaxDamage()) - stack.getDamage()) / maxDamage * 100.0 <= this.threshold.value) {
                     this.lastNotifyTime = System.currentTimeMillis();
                     mc.inGameHud
                        .getChatHud()
                        .addMessage(Text.literal("§c[ABOBUS123] » §fВНИМАНИЕ! Мало прочности у брони (меньше " + (int)this.threshold.value + "%)."));
                     break;
                  }
               }
            }
         }
      }
   }
}
