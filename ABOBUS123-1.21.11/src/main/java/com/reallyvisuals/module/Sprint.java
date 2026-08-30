package com.reallyvisuals.module;

import net.minecraft.client.MinecraftClient;

public class Sprint extends Module {
   public Sprint() {
      super("Sprint", "Автоматическое включение режима бега", Category.UTILITIES, true, false);
   }

   @Override
   public void onTick() {
      if (this.isEnabled()) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.player != null
            && mc.options != null
            && mc.player.forwardSpeed > 0.0F
            && !mc.player.isSneaking()
            && !mc.player.horizontalCollision
            && !mc.player.isUsingItem()) {
            mc.player.setSprinting(true);
         }
      }
   }
}
