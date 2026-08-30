package com.reallyvisuals.module;

import com.reallyvisuals.gui.ReallyVisualsScreen;
import com.reallyvisuals.gui.render.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class HitMarker extends Module {
   public final Module.NumberSetting duration = new Module.NumberSetting("Длительность, мс", 180.0, 80.0, 500.0, 10.0);
   public final Module.NumberSetting size = new Module.NumberSetting("Размер", 7.0, 4.0, 14.0, 1.0);
   public final Module.BooleanSetting clientColor = new Module.BooleanSetting("Цвет клиента", true);
   private long showUntil;

   public HitMarker() {
      super("Hit Marker", "Маркер попадания в центре экрана", Category.VISUALS, false, true);
      this.customColor = 0xFFFFFFFF;
      this.addSetting(this.duration);
      this.addSetting(this.size);
      this.addSetting(this.clientColor);
   }

   public void onAttack() {
      if (this.isEnabled()) this.showUntil = System.currentTimeMillis() + (long)this.duration.value;
   }

   @Override
   public void onRenderHUD(DrawContext context) {
      if (!this.isEnabled() || System.currentTimeMillis() > this.showUntil) return;
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.getWindow() == null) return;
      float cx = mc.getWindow().getScaledWidth() / 2.0F;
      float cy = mc.getWindow().getScaledHeight() / 2.0F;
      float s = (float)this.size.value;
      int color = this.clientColor.value ? ReallyVisualsScreen.clientColor : this.customColor;
      // Four short blocks form an X without relying on matrix rotation quirks on GLES/FCL.
      RenderUtils.drawRect(context, cx - s, cy - s, 3.0F, 2.0F, color);
      RenderUtils.drawRect(context, cx + s - 3.0F, cy - s, 3.0F, 2.0F, color);
      RenderUtils.drawRect(context, cx - s, cy + s - 2.0F, 3.0F, 2.0F, color);
      RenderUtils.drawRect(context, cx + s - 3.0F, cy + s - 2.0F, 3.0F, 2.0F, color);
   }
}
