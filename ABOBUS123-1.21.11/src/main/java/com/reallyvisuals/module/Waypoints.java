package com.reallyvisuals.module;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reallyvisuals.gui.font.CustomFont;
import com.reallyvisuals.gui.font.FontManager;
import com.reallyvisuals.gui.render.RenderUtils;
import com.reallyvisuals.gui.render.RenderUtils3D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

public class Waypoints extends Module {
   public Module.BooleanSetting showDistance = new Module.BooleanSetting("Показывать дистанцию", true);
   public Module.BooleanSetting showBeacon = new Module.BooleanSetting("Луч света", true);

   public Waypoints() {
      super("Waypoints", "Отображение пользовательских меток в мире", Category.MARKERS, true, true);
      this.addSetting(this.showDistance);
      this.addSetting(this.showBeacon);
   }

   public void onRender3D(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Camera camera, float tickDelta) {
      if (this.isEnabled()) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.world != null && mc.player != null) {
            Vec3d camPos = camera.getCameraPos();
            CustomFont mainFont = FontManager.getMainFont();
            CustomFont subFont = FontManager.getSubFont();
            CustomFont iconFont = FontManager.getIconFont();
            MarkerSettings settings = (MarkerSettings)ModuleManager.getInstance().getModule("MarkerSettings");
            double maxDist = settings != null ? settings.distance.value : 5000.0;
            for (WaypointManager.Waypoint wp : WaypointManager.getWaypoints()) {
               if (wp.visible) {
                  double dist = camPos.distanceTo(wp.pos);
                  boolean beam = this.showBeacon.value && (settings == null || settings.beaconBeam.value);
                  if (beam && dist <= maxDist) {
                     matrices.push();
                     matrices.translate(-camPos.x, -camPos.y, -camPos.z);
                     RenderUtils3D.draw3DLine(matrices, vertexConsumers, wp.pos, wp.pos.add(0.0, 48.0, 0.0), 1.0F, 0.65F, 0.15F, 0.55F);
                     matrices.pop();
                  }
                  // Labels moved to the HUD pass: world rendering has no DrawContext in 1.21.11.

               }
            }
         }
      }
   }
}
