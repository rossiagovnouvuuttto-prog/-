package com.reallyvisuals.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;

public class HideACBot extends Module {
   public HideACBot() {
      super("Hide AC Bot", "Скрытие ботов анти-чита вокруг игрока", Category.UTILITIES, false, true);
   }

   public static boolean shouldHideEntity(Entity entity) {
      HideACBot module = (HideACBot)ModuleManager.getInstance().getModule("Hide AC Bot");
      if (module != null && module.isEnabled()) {
         if (entity instanceof OtherClientPlayerEntity) {
            OtherClientPlayerEntity other = (OtherClientPlayerEntity)entity;
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) {
               return false;
            }

            double dist = mc.player.distanceTo(other);
            if (dist < 8.0) {
               if (mc.getNetworkHandler() != null && mc.getNetworkHandler().getPlayerListEntry(other.getUuid()) == null) {
                  return true;
               }

               if (other.getAbilities().flying || other.getY() > mc.player.getY() + 2.0 && dist < 5.0) {
                  return true;
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }
}
