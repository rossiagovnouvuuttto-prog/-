package com.reallyvisuals.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

public class CreateMarker extends Module {
   public CreateMarker() {
      super("Создание метки", "Создавайте новые метки в одно касание!", Category.MARKERS, false, true);
   }

   public static void onClickCreate() {
      MinecraftClient mc = MinecraftClient.getInstance();
      Vec3d pos = mc.player != null ? mc.player.getEntityPos() : new Vec3d(88.0, 73.0, -141.0);
      WaypointManager.addWaypointLimited("Быстрая метка", pos, -34019, 6, "Создана только что");
   }
}
