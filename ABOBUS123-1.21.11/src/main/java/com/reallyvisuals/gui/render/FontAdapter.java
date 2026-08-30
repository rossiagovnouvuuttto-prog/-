package com.reallyvisuals.gui.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public class FontAdapter {
   private static final TextRenderer renderer = MinecraftClient.getInstance().textRenderer;

   public static void drawString(DrawContext context, String text, float x, float y, int color) {
      context.drawText(renderer, text, Math.round(x), Math.round(y), color, false);
   }

   public static void drawStringWithShadow(DrawContext context, String text, float x, float y, int color) {
      context.drawTextWithShadow(renderer, text, Math.round(x), Math.round(y), color);
   }

   public static int getStringWidth(String text) {
      return renderer.getWidth(text);
   }

   public static int getFontHeight() {
      return 9;
   }
}
