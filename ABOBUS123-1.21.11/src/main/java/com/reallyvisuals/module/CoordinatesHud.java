package com.reallyvisuals.module;

import com.reallyvisuals.gui.HUDManager;
import com.reallyvisuals.gui.ReallyVisualsScreen;
import com.reallyvisuals.gui.font.CustomFont;
import com.reallyvisuals.gui.font.FontManager;
import com.reallyvisuals.gui.render.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

public class CoordinatesHud extends Module {
   public final Module.BooleanSetting showY = new Module.BooleanSetting("Показывать Y", true);
   public final Module.BooleanSetting showDirection = new Module.BooleanSetting("Направление", true);

   public CoordinatesHud() {
      super("Coordinates HUD", "Координаты и направление движения", Category.HUD, false, true);
      this.addSetting(this.showY);
      this.addSetting(this.showDirection);
   }

   @Override
   public void onRenderHUD(DrawContext context) {
      if (!this.isEnabled()) return;
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player == null) return;

      int bx = MathHelper.floor(mc.player.getX());
      int by = MathHelper.floor(mc.player.getY());
      int bz = MathHelper.floor(mc.player.getZ());
      String coords = this.showY.value ? "XYZ  " + bx + "  " + by + "  " + bz : "XZ  " + bx + "  " + bz;
      String dir = this.showDirection.value ? direction(mc.player.getYaw()) : "";

      CustomFont main = FontManager.getSubFont();
      int width = main.getStringWidth(coords) + 16;
      if (!dir.isEmpty()) width += main.getStringWidth(dir) + 12;
      int height = 20;
      int x = (int)HUDManager.coordinatesHud.x;
      int y = (int)HUDManager.coordinatesHud.y;
      HUDManager.coordinatesHud.setContentSize(width, height);
      HUDManager.coordinatesHud.beginScale(context);
      RenderUtils.drawRoundedRect(context, x, y, width, height, 6.0F, 0xEE111116);
      RenderUtils.drawRect(context, x, y, 3.0F, height, ReallyVisualsScreen.clientColor);
      main.drawString(context, coords, x + 8.0F, main.getCenteredTextY(y, height), -1);
      if (!dir.isEmpty()) {
         int dx = x + 8 + main.getStringWidth(coords) + 8;
         main.drawString(context, dir, dx, main.getCenteredTextY(y, height), 0xFFB8B8C5);
      }
      HUDManager.coordinatesHud.endScale(context);
   }

   private static String direction(float yaw) {
      int idx = Math.floorMod(Math.round(yaw / 45.0F), 8);
      String[] dirs = new String[]{"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
      return dirs[idx];
   }
}
