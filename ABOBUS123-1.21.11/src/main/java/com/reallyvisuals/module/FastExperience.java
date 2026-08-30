package com.reallyvisuals.module;

import com.reallyvisuals.mixin.accessor.MinecraftClientAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;

public class FastExperience extends Module {
   public FastExperience() {
      super("Fast Experience", "Ускорение использования бутылочек опыта", Category.UTILITIES, false, false);
   }

   @Override
   public void onTick() {
      if (this.isEnabled()) {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client.player != null && client.world != null) {
            if (client.options.useKey.isPressed()
               && (
                  client.player.getMainHandStack().getItem() == Items.EXPERIENCE_BOTTLE || client.player.getOffHandStack().getItem() == Items.EXPERIENCE_BOTTLE
               )) {
               ((MinecraftClientAccessor)client).setItemUseCooldown(0);
            }
         }
      }
   }
}
