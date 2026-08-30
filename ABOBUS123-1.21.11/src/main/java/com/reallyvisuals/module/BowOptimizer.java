package com.reallyvisuals.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BowItem;

public class BowOptimizer extends Module {
   public BowOptimizer() {
      super("Bow Optimizer", "Отпускает лук сразу, как только выстрел возможен", Category.UTILITIES, false, false);
   }

   @Override
   public void onTick() {
      if (this.isEnabled()) {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client.player != null && client.interactionManager != null) {
            if (client.player.isUsingItem() && client.player.getActiveItem().getItem() instanceof BowItem && client.player.getItemUseTime() >= 20) {
               client.interactionManager.stopUsingItem(client.player);
            }
         }
      }
   }
}
