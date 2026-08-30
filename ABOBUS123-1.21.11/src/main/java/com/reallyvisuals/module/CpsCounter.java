package com.reallyvisuals.module;

import com.reallyvisuals.gui.HUDManager;
import com.reallyvisuals.gui.ReallyVisualsScreen;
import com.reallyvisuals.gui.font.CustomFont;
import com.reallyvisuals.gui.font.FontManager;
import com.reallyvisuals.gui.render.RenderUtils;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class CpsCounter extends Module {
   public final Module.BooleanSetting rightClick = new Module.BooleanSetting("Показывать RMB", true);
   private final Deque<Long> leftClicks = new ArrayDeque<>();
   private final Deque<Long> rightClicks = new ArrayDeque<>();
   private boolean lastLeft;
   private boolean lastRight;

   public CpsCounter() {
      super("CPS Counter", "Счётчик кликов мыши за секунду", Category.HUD, false, true);
      this.addSetting(this.rightClick);
   }

   @Override
   public void onTick() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.options == null) return;
      long now = System.currentTimeMillis();
      boolean left = mc.options.attackKey.isPressed();
      boolean right = mc.options.useKey.isPressed();
      if (left && !this.lastLeft) this.leftClicks.addLast(now);
      if (right && !this.lastRight) this.rightClicks.addLast(now);
      this.lastLeft = left;
      this.lastRight = right;
      purge(this.leftClicks, now);
      purge(this.rightClicks, now);
   }

   @Override
   public void onRenderHUD(DrawContext context) {
      if (!this.isEnabled()) return;
      long now = System.currentTimeMillis();
      purge(this.leftClicks, now);
      purge(this.rightClicks, now);
      String text = this.rightClick.value ? this.leftClicks.size() + " CPS  •  " + this.rightClicks.size() + " RMB" : this.leftClicks.size() + " CPS";
      CustomFont font = FontManager.getSubFont();
      int width = font.getStringWidth(text) + 16;
      int height = 20;
      int x = (int)HUDManager.cpsHud.x;
      int y = (int)HUDManager.cpsHud.y;
      HUDManager.cpsHud.setContentSize(width, height);
      HUDManager.cpsHud.beginScale(context);
      RenderUtils.drawRoundedRect(context, x, y, width, height, 6.0F, 0xEE111116);
      RenderUtils.drawRect(context, x, y, 3.0F, height, ReallyVisualsScreen.clientColor);
      font.drawString(context, text, x + 8.0F, font.getCenteredTextY(y, height), -1);
      HUDManager.cpsHud.endScale(context);
   }

   private static void purge(Deque<Long> q, long now) {
      while (!q.isEmpty() && now - q.peekFirst() > 1000L) q.removeFirst();
   }
}
