package com.reallyvisuals.module;

import com.reallyvisuals.gui.HUDManager;
import com.reallyvisuals.gui.ReallyVisualsScreen;
import com.reallyvisuals.gui.font.CustomFont;
import com.reallyvisuals.gui.font.FontManager;
import com.reallyvisuals.gui.render.RenderUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class Hotkeys extends Module {
   public Hotkeys() {
      super("Hot Keys", "Отображение активных биндов", Category.HUD, false, false);
   }

   @Override
   public void onRenderHUD(DrawContext context) {
      if (this.isEnabled()) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.player != null && mc.world != null) {
            List<Hotkeys.KeybindSnapshot> list = this.getActiveKeybinds(mc);
            if (list.isEmpty()) {
               if (!(mc.currentScreen instanceof ReallyVisualsScreen)) {
                  return;
               }

               list.add(new Hotkeys.KeybindSnapshot("Armor HUD", "R"));
            }

            CustomFont mainFont = FontManager.getMainFont();
            CustomFont subFont = FontManager.getSubFont();
            int x = (int)HUDManager.hotkeys.x;
            int y = (int)HUDManager.hotkeys.y;
            int cardWidth = 118;
            int rowHeight = 16;
            int headerHeight = 20;
            int cardHeight = headerHeight + list.size() * rowHeight + 4;
            HUDManager.hotkeys.setContentSize(cardWidth, cardHeight);
            HUDManager.hotkeys.beginScale(context);
            RenderUtils.drawRoundedRect(context, x, y, cardWidth, cardHeight, 7.0F, -300871403);
            RenderUtils.drawRoundedRect(context, x + 1, y + 1, cardWidth - 2, cardHeight - 2, 6.5F, -15461351);
            int iconX = x + 8;
            int iconY = y + 5;
            RenderUtils.drawRoundedRect(context, iconX, iconY, 10.0F, 7.0F, 2.0F, ReallyVisualsScreen.clientColor);
            RenderUtils.drawRoundedRect(context, iconX + 1.0F, iconY + 1.0F, 8.0F, 5.0F, 1.5F, -15461351);
            RenderUtils.drawRect(context, iconX + 2.5F, iconY + 2.5F, 1.0F, 1.0F, ReallyVisualsScreen.clientColor);
            RenderUtils.drawRect(context, iconX + 4.5F, iconY + 2.5F, 1.0F, 1.0F, ReallyVisualsScreen.clientColor);
            RenderUtils.drawRect(context, iconX + 6.5F, iconY + 2.5F, 1.0F, 1.0F, ReallyVisualsScreen.clientColor);
            RenderUtils.drawRect(context, iconX + 3.5F, iconY + 4.5F, 3.0F, 1.0F, ReallyVisualsScreen.clientColor);
            mainFont.drawString(context, "Hot Keys", x + 22, y + 4, -1);
            int rowY = y + headerHeight;

            for (Hotkeys.KeybindSnapshot ks : list) {
               subFont.drawString(context, ks.moduleName, x + 8, rowY + 3, -1);
               int pillWidth = subFont.getStringWidth(ks.keyName) + 8;
               int pillX = x + cardWidth - 6 - pillWidth;
               int pillY = rowY + 1;
               RenderUtils.drawRoundedRect(context, pillX, pillY, pillWidth, 12.0F, 4.0F, -14737626);
               subFont.drawString(context, ks.keyName, pillX + 4, pillY + 2, -1);
               rowY += rowHeight;
            }

            HUDManager.hotkeys.endScale(context);
         }
      }
   }

   private List<Hotkeys.KeybindSnapshot> getActiveKeybinds(MinecraftClient mc) {
      ArrayList<Hotkeys.KeybindSnapshot> list = new ArrayList<>();

      for (Module m : ModuleManager.getInstance().getModules()) {
         String keyStr;
         if (m.isEnabled() && !(keyStr = this.getKeyName(m.getKey())).equals("NONE")) {
            list.add(new Hotkeys.KeybindSnapshot(m.getName(), keyStr));
         }
      }

      return list;
   }

   private String getKeyName(int key) {
      if (key <= 0) {
         return "NONE";
      } else {
         String name = com.reallyvisuals.utils.KeyUtils.getShortKeyName(key);
         return name == null || name.isEmpty() ? "NONE" : name.toUpperCase();
      }
   }

   public static class KeybindSnapshot {
      public final String moduleName;
      public final String keyName;

      public KeybindSnapshot(String moduleName, String keyName) {
         this.moduleName = moduleName;
         this.keyName = keyName;
      }
   }
}
