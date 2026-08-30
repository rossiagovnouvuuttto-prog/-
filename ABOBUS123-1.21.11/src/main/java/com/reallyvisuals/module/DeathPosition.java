package com.reallyvisuals.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

public class DeathPosition extends Module {
   public Module.BooleanSetting showCoords = new Module.BooleanSetting("Координаты", true);
   public Module.BooleanSetting autoRemove = new Module.BooleanSetting("Автоудаление", false);
   private boolean wasAlive = true;
   private Vec3d lastAlivePos;
   private WaypointManager.Waypoint deathMarker;

   public DeathPosition() {
      super("Death Markers", "Метка места последней смерти", Category.MARKERS, false, true);
      this.addSetting(this.showCoords);
      this.addSetting(this.autoRemove);
   }

   @Override
   public void onTick() {
      if (!this.isEnabled()) return;
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player == null || mc.world == null) {
         this.wasAlive = true;
         this.lastAlivePos = null;
         return;
      }
      boolean alive = mc.player.isAlive() && mc.player.getHealth() > 0.0F;
      if (alive) {
         this.lastAlivePos = mc.player.getEntityPos();
         if (this.autoRemove.value && this.deathMarker != null && mc.player.squaredDistanceTo(this.deathMarker.pos) < 9.0) {
            WaypointManager.removeWaypoint(this.deathMarker);
            this.deathMarker = null;
         }
      } else if (this.wasAlive && this.lastAlivePos != null) {
         if (this.deathMarker != null) WaypointManager.removeWaypoint(this.deathMarker);
         String name = "Место смерти";
         if (this.showCoords.value) {
            name += String.format(" [%d %d %d]", (int)Math.floor(this.lastAlivePos.x), (int)Math.floor(this.lastAlivePos.y), (int)Math.floor(this.lastAlivePos.z));
         }
         WaypointManager.addWaypointLimited(name, this.lastAlivePos, 0xFFFF5555, 4, "Последняя смерть");
         this.deathMarker = WaypointManager.getLastWaypoint();
      }
      this.wasAlive = alive;
   }
}
