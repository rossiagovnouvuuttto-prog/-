package com.reallyvisuals.module;

import com.reallyvisuals.audio.UISoundHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

public class MarkerSettings extends Module {
   public Module.BooleanSetting showDistance = new Module.BooleanSetting("Показывать дистанцию", true);
   public Module.BooleanSetting showIcons = new Module.BooleanSetting("Показывать иконки", true);
   public Module.BooleanSetting beaconBeam = new Module.BooleanSetting("Луч света", true);
   public Module.NumberSetting distance = new Module.NumberSetting("Дистанция меток", 500.0, 50.0, 5000.0, 50.0);
   public Module.NumberSetting limit = new Module.NumberSetting("Лимит меток", 50.0, 5.0, 200.0, 5.0);
   public Module.KeySetting quickMarkerKey = new Module.KeySetting("Быстрая метка", 72);
   public double limitValue = 50.0;

   public MarkerSettings() {
      super("MarkerSettings", "Глобальные параметры системы меток", Category.MARKERS, false, true);
      this.addSetting(this.showDistance);
      this.addSetting(this.showIcons);
      this.addSetting(this.beaconBeam);
      this.addSetting(this.distance);
      this.addSetting(this.limit);
      this.addSetting(this.quickMarkerKey);
   }

   @Override
   public void onTick() { this.limitValue = this.limit.value; }

   @Override
   public void onKeyPressed(int key) {
      if (this.quickMarkerKey.key != 0 && key == this.quickMarkerKey.key) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.player != null) {
            Vec3d p = mc.player.getEntityPos();
            WaypointManager.addWaypointLimited("Быстрая метка", new Vec3d(Math.floor(p.x), Math.floor(p.y), Math.floor(p.z)), -34019, 6, "Создана только что");
            UISoundHelper.playSound("ui.toggle_on");
         }
      }
   }
}
