package com.reallyvisuals.module;

import com.reallyvisuals.gui.HUDManager;
import com.reallyvisuals.gui.ReallyVisualsScreen;
import com.reallyvisuals.gui.font.CustomFont;
import com.reallyvisuals.gui.font.FontManager;
import com.reallyvisuals.gui.render.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;

public class ComboCounter extends Module {
   public final Module.NumberSetting resetTime = new Module.NumberSetting("Сброс через, сек", 2.0, 0.5, 5.0, 0.1);
   private int combo;
   private int targetId = Integer.MIN_VALUE;
   private long lastHit;

   public ComboCounter() {
      super("Combo Counter", "Счётчик последовательных ударов по одной цели", Category.HUD, false, true);
      this.addSetting(this.resetTime);
   }

   public void onAttack(LivingEntity target) {
      if (!this.isEnabled() || target == null) return;
      long now = System.currentTimeMillis();
      long timeout = (long)(this.resetTime.value * 1000.0);
      if (target.getId() == this.targetId && now - this.lastHit <= timeout) this.combo++;
      else this.combo = 1;
      this.targetId = target.getId();
      this.lastHit = now;
   }

   @Override
   public void onTick() {
      MinecraftClient mc = MinecraftClient.getInstance();
      long timeout = (long)(this.resetTime.value * 1000.0);
      if (this.combo > 0 && System.currentTimeMillis() - this.lastHit > timeout) this.combo = 0;
      if (mc.player != null && mc.player.hurtTime > 0) this.combo = 0;
   }

   @Override
   public void onRenderHUD(DrawContext context) {
      if (!this.isEnabled()) return;
      String text = this.combo > 0 ? "Combo  " + this.combo : "Combo  0";
      CustomFont font = FontManager.getSubFont();
      int width = font.getStringWidth(text) + 16;
      int height = 20;
      int x = (int)HUDManager.comboHud.x;
      int y = (int)HUDManager.comboHud.y;
      HUDManager.comboHud.setContentSize(width, height);
      HUDManager.comboHud.beginScale(context);
      RenderUtils.drawRoundedRect(context, x, y, width, height, 6.0F, 0xEE111116);
      RenderUtils.drawRect(context, x, y, 3.0F, height, ReallyVisualsScreen.clientColor);
      font.drawString(context, text, x + 8.0F, font.getCenteredTextY(y, height), -1);
      HUDManager.comboHud.endScale(context);
   }
}
