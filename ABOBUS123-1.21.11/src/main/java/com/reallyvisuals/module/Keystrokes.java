package com.reallyvisuals.module;

import com.reallyvisuals.gui.HUDManager;
import com.reallyvisuals.gui.ReallyVisualsScreen;
import com.reallyvisuals.gui.font.CustomFont;
import com.reallyvisuals.gui.font.FontManager;
import com.reallyvisuals.gui.render.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.gui.DrawContext;

public class Keystrokes extends Module {
   public final Module.BooleanSetting mouseButtons = new Module.BooleanSetting("LMB / RMB", true);
   public final Module.BooleanSetting space = new Module.BooleanSetting("Space", true);

   public Keystrokes() {
      super("Keystrokes", "WASD и нажатия мыши на HUD", Category.HUD, false, true);
      this.addSetting(this.mouseButtons);
      this.addSetting(this.space);
   }

   @Override
   public void onRenderHUD(DrawContext context) {
      if (!this.isEnabled()) return;
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.options == null) return;

      int x = (int)HUDManager.keystrokes.x;
      int y = (int)HUDManager.keystrokes.y;
      int key = 18;
      int gap = 2;
      int totalW = key * 3 + gap * 2;
      int height = key * 2 + gap;
      if (this.mouseButtons.value) height += 14 + gap;
      if (this.space.value) height += 8 + gap;
      HUDManager.keystrokes.setContentSize(totalW, height);
      HUDManager.keystrokes.beginScale(context);

      drawKey(context, x + key + gap, y, key, key, "W", mc.options.forwardKey);
      drawKey(context, x, y + key + gap, key, key, "A", mc.options.leftKey);
      drawKey(context, x + key + gap, y + key + gap, key, key, "S", mc.options.backKey);
      drawKey(context, x + (key + gap) * 2, y + key + gap, key, key, "D", mc.options.rightKey);

      int rowY = y + key * 2 + gap * 2;
      if (this.mouseButtons.value) {
         int mouseW = (totalW - gap) / 2;
         drawKey(context, x, rowY, mouseW, 14, "LMB", mc.options.attackKey);
         drawKey(context, x + mouseW + gap, rowY, mouseW, 14, "RMB", mc.options.useKey);
         rowY += 14 + gap;
      }
      if (this.space.value) {
         drawKey(context, x, rowY, totalW, 8, "SPACE", mc.options.jumpKey);
      }

      HUDManager.keystrokes.endScale(context);
   }

   private void drawKey(DrawContext context, int x, int y, int w, int h, String label, KeyBinding binding) {
      boolean pressed = binding != null && binding.isPressed();
      int bg = pressed ? ReallyVisualsScreen.clientColor : 0xD016161B;
      int outline = pressed ? 0xAAFFFFFF : 0x552E2E36;
      RenderUtils.drawRoundedRect(context, x, y, w, h, 4.0F, outline);
      RenderUtils.drawRoundedRect(context, x + 1, y + 1, w - 2, h - 2, 3.5F, bg);
      CustomFont font = FontManager.getSubFont();
      float tx = x + (w - font.getStringWidth(label)) / 2.0F;
      float ty = font.getCenteredTextY(y, h);
      font.drawString(context, label, tx, ty, -1);
   }
}
