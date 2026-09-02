package com.reallyvisuals.module;

import com.reallyvisuals.gui.HUDManager;
import com.reallyvisuals.gui.font.CustomFont;
import com.reallyvisuals.gui.font.FontManager;
import com.reallyvisuals.gui.render.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public class Watermark extends Module {
   private static final Identifier LOGO = Identifier.of("really", "textures/logo/rv_colored.png");

   public Watermark() {
      super("Watermark", "Отображение водяного знака клиента", Category.HUD, true, false);
   }

   @Override
   public void onRenderHUD(DrawContext context) {
      if (this.isEnabled()) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.player != null) {
            CustomFont font = FontManager.getSubFont();
            int fps = this.getFps(mc);
            int ping = -1;
            PlayerListEntry entry;
            if (mc.getNetworkHandler() != null && (entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid())) != null && entry.getLatency() >= 0) {
               ping = entry.getLatency();
            }

            String titleText = "ABOBUS123";
            String fpsText = fps + " FPS";
            String pingText = ping >= 0 ? ping + " ms" : "-- ms";
            int titleW = font.getStringWidth(titleText);
            int fpsW = font.getStringWidth(fpsText);
            int pingW = font.getStringWidth(pingText);
            int dotSize = 3;
            int gap = 6;
            int x = (int)HUDManager.watermark.x;
            int y = (int)HUDManager.watermark.y;
            int logoH = 9;
            int logoW = 16;
            int cardHeight = 19;
            float radius = 6.0F;
            int cardWidth = 7 + logoW + 7 + titleW + gap + dotSize + gap + fpsW + gap + dotSize + gap + pingW + 8;
            HUDManager.watermark.setContentSize(cardWidth, cardHeight);
            HUDManager.watermark.beginScale(context);
            RenderUtils.drawRoundedRect(context, x, y, cardWidth, cardHeight, radius, -300871403);
            RenderUtils.drawRoundedRect(context, x + 1, y + 1, cardWidth - 2, cardHeight - 2, radius - 0.5F, -15461351);
            int curX = x + 7;

            RenderUtils.drawTexture(context, LOGO, curX, y + (cardHeight - logoH) / 2.0F, logoW, logoH);

            int var24;
            font.drawString(context, titleText, var24 = curX + logoW + 7, font.getCenteredTextY(y, cardHeight), -1);
            RenderUtils.drawRoundedRect(context, curX = var24 + titleW + gap, y + 8.0F, dotSize, dotSize, dotSize / 2.0F, -8750459);
            int var26;
            font.drawString(context, fpsText, var26 = curX + dotSize + gap, font.getCenteredTextY(y, cardHeight), -1);
            RenderUtils.drawRoundedRect(context, curX = var26 + fpsW + gap, y + 8.0F, dotSize, dotSize, dotSize / 2.0F, -8750459);
            int var28;
            font.drawString(context, pingText, var28 = curX + dotSize + gap, font.getCenteredTextY(y, cardHeight), -1);
            HUDManager.watermark.endScale(context);
         }
      }
   }

   /**
    * 1.18.2 exposed fpsDebugString ("60 fps T: ..."), so this used to split on a
    * space. getCurrentFps() returns an int, the split never matched and the
    * watermark reported a hardcoded 60 forever.
    */
   private int getFps(MinecraftClient mc) {
      return Math.max(0, mc.getCurrentFps());
   }
}
