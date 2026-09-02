package com.reallyvisuals.module;

import com.reallyvisuals.gui.HUDManager;
import com.reallyvisuals.gui.ReallyVisualsScreen;
import com.reallyvisuals.gui.font.CustomFont;
import com.reallyvisuals.gui.font.FontManager;
import com.reallyvisuals.gui.render.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class PerformanceHud extends Module {
   private int fps = 60;
   private long lastUpdate;

   public PerformanceHud() {
      super("Performance HUD", "FPS, время кадра и использование памяти", Category.HUD, false, false);
   }

   @Override
   public void onRenderHUD(DrawContext context) {
      if (!this.isEnabled()) return;
      long now = System.currentTimeMillis();
      if (now - this.lastUpdate > 250L) {
         this.fps = readFps();
         this.lastUpdate = now;
      }

      Runtime rt = Runtime.getRuntime();
      long used = rt.totalMemory() - rt.freeMemory();
      int mem = (int)Math.max(0L, Math.min(100L, used * 100L / Math.max(1L, rt.maxMemory())));
      double frameMs = 1000.0 / Math.max(1, this.fps);

      CustomFont main = FontManager.getMainFont();
      CustomFont sub = FontManager.getSubFont();
      String top = this.fps + " FPS";
      String bottom = String.format(java.util.Locale.ROOT, "%.1f ms  •  RAM %d%%", frameMs, mem);
      int width = Math.max(main.getStringWidth(top), sub.getStringWidth(bottom)) + 18;
      int height = 30;
      int x = (int)HUDManager.performanceHud.x;
      int y = (int)HUDManager.performanceHud.y;
      HUDManager.performanceHud.setContentSize(width, height);
      HUDManager.performanceHud.beginScale(context);

      RenderUtils.drawRoundedRect(context, x, y, width, height, 7.0F, 0xEE111116);
      RenderUtils.drawRect(context, x, y, 3.0F, height, ReallyVisualsScreen.clientColor);
      main.drawString(context, top, x + 9, y + 5, -1);
      sub.drawString(context, bottom, x + 9, y + 17, 0xFFB6B6C2);

      HUDManager.performanceHud.endScale(context);
   }

   private static int readFps() {
      return Math.max(0, MinecraftClient.getInstance().getCurrentFps());
   }
}
