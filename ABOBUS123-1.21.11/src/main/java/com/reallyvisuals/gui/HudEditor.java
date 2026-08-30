package com.reallyvisuals.gui;

import com.reallyvisuals.config.ConfigManager;
import com.reallyvisuals.gui.font.CustomFont;
import com.reallyvisuals.gui.font.FontManager;
import com.reallyvisuals.gui.render.RenderUtils;
import com.reallyvisuals.module.Module;
import com.reallyvisuals.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Lightweight HUD editor that is active while the vanilla chat is open.
 * Left mouse drags, right mouse resets, mouse wheel changes scale.
 */
public final class HudEditor {
   private static HUDManager.HUDElement dragging;
   private static float dragOffsetX;
   private static float dragOffsetY;
   private static boolean dirty;

   private HudEditor() {
   }

   public static boolean isDragging() {
      return dragging != null;
   }

   public static void render(DrawContext context, int mouseX, int mouseY) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.getWindow() == null) {
         return;
      }

      int screenW = mc.getWindow().getScaledWidth();
      int screenH = mc.getWindow().getScaledHeight();
      HUDManager.clampAll(screenW, screenH);
      CustomFont font = FontManager.getSubFont();

      for (HUDManager.HUDElement e : HUDManager.getAllElements()) {
         if (!isEditable(e)) {
            continue;
         }

         boolean hovered = e.contains(mouseX, mouseY);
         int outline = hovered || e == dragging ? ReallyVisualsScreen.clientColor : 0x80FFFFFF;
         int fill = hovered || e == dragging ? 0x2219A7FF : 0x10000000;
         float x = e.x;
         float y = e.y;
         float w = Math.max(12.0F, e.width);
         float h = Math.max(10.0F, e.height);

         RenderUtils.drawRect(context, x, y, w, h, fill);
         RenderUtils.drawRect(context, x, y, w, 1.0F, outline);
         RenderUtils.drawRect(context, x, y + h - 1.0F, w, 1.0F, outline);
         RenderUtils.drawRect(context, x, y, 1.0F, h, outline);
         RenderUtils.drawRect(context, x + w - 1.0F, y, 1.0F, h, outline);

         int labelW = font.getStringWidth(e.name) + 8;
         float labelY = Math.max(1.0F, y - 12.0F);
         RenderUtils.drawRoundedRect(context, x, labelY, labelW, 10.0F, 3.0F, 0xD0101014);
         font.drawString(context, e.name, x + 4.0F, labelY + 1.0F, -1);
      }

      String help = "HUD: LMB drag  •  RMB reset  •  Wheel scale";
      int helpW = font.getStringWidth(help) + 12;
      float helpX = Math.max(4.0F, (screenW - helpW) / 2.0F);
      float helpY = 6.0F;
      RenderUtils.drawRoundedRect(context, helpX, helpY, helpW, 14.0F, 5.0F, 0xD0101014);
      font.drawString(context, help, helpX + 6.0F, helpY + 3.0F, -1);
   }

   public static boolean mousePressed(double mouseX, double mouseY, int button) {
      HUDManager.HUDElement hovered = findHovered((float)mouseX, (float)mouseY);
      if (hovered == null) {
         return false;
      }

      if (button == 0) {
         dragging = hovered;
         dragOffsetX = (float)mouseX - hovered.x;
         dragOffsetY = (float)mouseY - hovered.y;
         return true;
      }

      if (button == 1) {
         hovered.x = hovered.defaultX;
         hovered.y = hovered.defaultY;
         hovered.scale = "Medium";
         clamp(hovered);
         dirty = true;
         ConfigManager.saveConfig();
         return true;
      }

      return false;
   }

   public static void mouseMoved(double mouseX, double mouseY) {
      if (dragging == null) {
         return;
      }

      dragging.x = (float)mouseX - dragOffsetX;
      dragging.y = (int)mouseY - dragOffsetY;
      clamp(dragging);
      dirty = true;
   }

   public static boolean mouseReleased(int button) {
      if (button == 0 && dragging != null) {
         dragging = null;
         if (dirty) {
            ConfigManager.saveConfig();
            dirty = false;
         }
         return true;
      }
      return false;
   }

   public static boolean mouseScrolled(double mouseX, double mouseY, double amount) {
      HUDManager.HUDElement hovered = findHovered((float)mouseX, (float)mouseY);
      if (hovered == null || amount == 0.0) {
         return false;
      }

      if (amount > 0.0) {
         hovered.scale = "Small".equals(hovered.scale) ? "Medium" : "Large";
      } else {
         hovered.scale = "Large".equals(hovered.scale) ? "Medium" : "Small";
      }
      clamp(hovered);
      ConfigManager.saveConfig();
      return true;
   }

   public static void close() {
      dragging = null;
      if (dirty) {
         ConfigManager.saveConfig();
         dirty = false;
      }
   }

   private static HUDManager.HUDElement findHovered(float mouseX, float mouseY) {
      HUDManager.HUDElement[] elements = HUDManager.getAllElements();
      for (int i = elements.length - 1; i >= 0; --i) {
         HUDManager.HUDElement e = elements[i];
         if (isEditable(e) && e.contains(mouseX, mouseY)) {
            return e;
         }
      }
      return null;
   }

   private static void clamp(HUDManager.HUDElement e) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.getWindow() != null) {
         e.clamp(mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
      }
   }

   private static boolean isEditable(HUDManager.HUDElement e) {
      if (!e.enabled) {
         return false;
      }

      String moduleName = null;
      if (e == HUDManager.potions) moduleName = "Potions";
      else if (e == HUDManager.hotkeys) moduleName = "Hot Keys";
      else if (e == HUDManager.cooldowns) moduleName = "Cooldowns";
      else if (e == HUDManager.targetHud) moduleName = "Target HUD";
      else if (e == HUDManager.watermark) moduleName = "Watermark";
      else if (e == HUDManager.inventoryHud) moduleName = "Inventory HUD";
      else if (e == HUDManager.betterNear) moduleName = "Better Near";
      else if (e == HUDManager.keystrokes) moduleName = "Keystrokes";
      else if (e == HUDManager.performanceHud) moduleName = "Performance HUD";
      else if (e == HUDManager.coordinatesHud) moduleName = "Coordinates HUD";
      else if (e == HUDManager.speedHud) moduleName = "Speed HUD";
      else if (e == HUDManager.cpsHud) moduleName = "CPS Counter";
      else if (e == HUDManager.comboHud) moduleName = "Combo Counter";

      if (moduleName == null) {
         return e == HUDManager.scoreboard || e == HUDManager.bossbar;
      }

      Module m = ModuleManager.getInstance().getModule(moduleName);
      return m != null && m.isEnabled();
   }
}
