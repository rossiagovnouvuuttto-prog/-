package com.reallyvisuals.module;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.math.Vec3d;

public class WaypointManager {
   private static final List<WaypointManager.Waypoint> waypoints = new ArrayList<>();

   public static List<WaypointManager.Waypoint> getWaypoints() { return waypoints; }

   public static void addWaypoint(String name, Vec3d pos, int color, int iconIndex, String createdText) {
      waypoints.add(new WaypointManager.Waypoint(name, pos, color, iconIndex, createdText));
   }

   public static void addWaypointLimited(String name, Vec3d pos, int color, int iconIndex, String createdText) {
      int limit = 50;
      Module m = ModuleManager.getInstance().getModule("MarkerSettings");
      if (m instanceof MarkerSettings) limit = Math.max(5, (int)((MarkerSettings)m).limitValue);
      while (waypoints.size() >= limit && !waypoints.isEmpty()) waypoints.remove(0);
      addWaypoint(name, pos, color, iconIndex, createdText);
   }

   public static WaypointManager.Waypoint getLastWaypoint() {
      return waypoints.isEmpty() ? null : waypoints.get(waypoints.size() - 1);
   }

   public static void removeWaypoint(WaypointManager.Waypoint waypoint) { waypoints.remove(waypoint); }

   public static class Waypoint {
      public String name;
      public Vec3d pos;
      public int color;
      public int iconIndex;
      public boolean visible;
      public String createdText;
      public boolean expandedInGui;

      public Waypoint(String name, Vec3d pos, int color, int iconIndex, String createdText) {
         this.name = name; this.pos = pos; this.color = color; this.iconIndex = iconIndex;
         this.visible = true; this.createdText = createdText; this.expandedInGui = false;
      }

      public String getIconString() {
         switch (this.iconIndex) {
            case 0: return "\ue934";
            case 1: return "\ue900";
            case 2: return "\ue925";
            case 3: return "\ue910";
            case 4: return "\ue91b";
            case 5: return "\ue91a";
            case 6: return "\ue926";
            case 7: return "\ue905";
            default: return "\ue926";
         }
      }
   }
}
