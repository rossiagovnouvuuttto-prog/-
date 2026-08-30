package com.reallyvisuals.module;

import java.util.Random;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

public class AutoSethome extends Module {
   public Module.ModeSetting conditionSetting = new Module.ModeSetting("Условие", new String[]{"По кнопке", "При телепортации"}, "По кнопке");
   public Module.KeySetting keySetting = new Module.KeySetting("Кнопка", 0);
   public Module.BooleanSetting randomNameSetting = new Module.BooleanSetting("Рандомное название", false);
   public Module.TextSetting nameSetting = new Module.TextSetting("Название", "home");
   public Module.BooleanSetting markerSetting = new Module.BooleanSetting("Ставить метку", false);
   private Vec3d lastPos = null;

   public AutoSethome() {
      super("Auto Sethome", "Автоматическая установка сетхома", Category.UTILITIES, false, true);
      this.addSetting(this.conditionSetting);
      this.addSetting(this.keySetting.visible(() -> this.conditionSetting.value.equals("По кнопке")));
      this.addSetting(this.randomNameSetting);
      this.addSetting(this.nameSetting);
      this.addSetting(this.markerSetting);
   }

   @Override
   public void onKeyPressed(int key) {
      if (this.isEnabled()) {
         if (this.conditionSetting.value.equals("По кнопке") && this.keySetting.key != 0 && this.keySetting.key == key) {
            this.executeSethome();
         }
      }
   }

   @Override
   public void onTick() {
      if (this.isEnabled()) {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client.player != null) {
            if (this.conditionSetting.value.equals("При телепортации")) {
               Vec3d currentPos = client.player.getEntityPos();
               if (this.lastPos != null && this.lastPos.squaredDistanceTo(currentPos) > 100.0) {
                  this.executeSethome();
               }

               this.lastPos = currentPos;
            }
         }
      }
   }

   private void executeSethome() {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player != null) {
         String homeName = this.nameSetting.value;
         if (this.randomNameSetting.value) {
            homeName = homeName + new Random().nextInt(1000);
         }

         client.player.networkHandler.sendChatMessage("/sethome " + homeName);
         if (this.markerSetting.value) {
            Vec3d p = client.player.getEntityPos();
            WaypointManager.addWaypointLimited("Дом: " + homeName, new Vec3d(Math.floor(p.x) + 0.5, Math.floor(p.y), Math.floor(p.z) + 0.5), 0xFF55FF55, 7, "Создано Auto Sethome");
         }
      }
   }
}
