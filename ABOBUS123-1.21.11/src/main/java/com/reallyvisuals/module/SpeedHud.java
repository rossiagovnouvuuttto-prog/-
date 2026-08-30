package com.reallyvisuals.module;

import com.reallyvisuals.gui.HUDManager;
import com.reallyvisuals.gui.ReallyVisualsScreen;
import com.reallyvisuals.gui.font.CustomFont;
import com.reallyvisuals.gui.font.FontManager;
import com.reallyvisuals.gui.render.RenderUtils;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class SpeedHud extends Module {
   public final Module.ModeSetting unit = new Module.ModeSetting("Единицы", new String[]{"Блок/с", "км/ч"}, "Блок/с");
   private double lastX;
   private double lastZ;
   private double speed;
   private boolean hasLast;

   public SpeedHud() {
      super("Speed HUD", "Скорость движения игрока", Category.HUD, false, true);
      this.addSetting(this.unit);
   }

   @Override
   public void onTick() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player == null) {
         this.hasLast = false;
         this.speed = 0.0;
         return;
      }
      double x = mc.player.getX();
      double z = mc.player.getZ();
      if (this.hasLast) {
         double dx = x - this.lastX;
         double dz = z - this.lastZ;
         double instant = Math.sqrt(dx * dx + dz * dz) * 20.0;
         this.speed += (instant - this.speed) * 0.35;
      } else {
         this.hasLast = true;
      }
      this.lastX = x;
      this.lastZ = z;
   }

   @Override
   public void onRenderHUD(DrawContext context) {
      if (!this.isEnabled()) return;
      double shown = "км/ч".equals(this.unit.value) ? this.speed * 3.6 : this.speed;
      String suffix = "км/ч".equals(this.unit.value) ? " km/h" : " b/s";
      String text = String.format(Locale.ROOT, "%.1f%s", shown, suffix);
      CustomFont font = FontManager.getSubFont();
      int width = font.getStringWidth(text) + 16;
      int height = 20;
      int x = (int)HUDManager.speedHud.x;
      int y = (int)HUDManager.speedHud.y;
      HUDManager.speedHud.setContentSize(width, height);
      HUDManager.speedHud.beginScale(context);
      RenderUtils.drawRoundedRect(context, x, y, width, height, 6.0F, 0xEE111116);
      RenderUtils.drawRect(context, x, y, 3.0F, height, ReallyVisualsScreen.clientColor);
      font.drawString(context, text, x + 8.0F, font.getCenteredTextY(y, height), -1);
      HUDManager.speedHud.endScale(context);
   }
}
