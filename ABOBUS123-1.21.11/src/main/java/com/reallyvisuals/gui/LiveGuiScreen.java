package com.reallyvisuals.gui;

import com.reallyvisuals.module.Module;
import com.reallyvisuals.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Lightweight in-game overlay. It does not pause the world and lets the player
 * toggle the most frequently used visual/performance modules with one click.
 */
public class LiveGuiScreen extends Screen {
   private static final String[] MODULE_NAMES = {
      "Full Bright", "Zoom", "Free Look", "Performance Boost", "Sprint"
   };
   private static final String[] LABELS = {
      "Fullbright", "Zoom", "Freelook", "SuperRes", "Sprint"
   };
   private static final String[] THEMES = {
      "Crystall Glow", "Neon Cyan", "Sunset Gradient", "Dark Gray"
   };
   private static final int[] ACCENTS = {
      0xFF6E7CFF, 0xFF00E5FF, 0xFFFF7A59, 0xFFA8A8B3
   };

   private int panelX = 24;
   private int panelY = 36;
   private final int panelWidth = 190;
   private final int headerHeight = 30;
   private final int rowHeight = 24;
   private boolean dragging;
   private int dragOffsetX;
   private int dragOffsetY;
   private int themeIndex = 0;

   public LiveGuiScreen() {
      super(Text.literal("ABOBUS123 LiveGUI"));
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      int accent = ACCENTS[themeIndex];
      int panelHeight = headerHeight + 8 + MODULE_NAMES.length * rowHeight + 48;

      context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xD91A1B20);
      context.fill(panelX, panelY, panelX + panelWidth, panelY + 2, accent);
      context.fill(panelX, panelY + headerHeight, panelX + panelWidth, panelY + headerHeight + 1, 0x553A3A44);

      MinecraftClient mc = MinecraftClient.getInstance();
      int fps = mc.getCurrentFps();
      int active = getActiveCount();

      context.drawTextWithShadow(this.textRenderer, Text.literal("ABOBUS123 LiveGUI"), panelX + 10, panelY + 8, 0xFFFFFFFF);
      String stats = fps + " FPS  •  " + active + "/5";
      int statsWidth = this.textRenderer.getWidth(stats);
      context.drawTextWithShadow(this.textRenderer, Text.literal(stats), panelX + panelWidth - statsWidth - 10, panelY + 8, accent);

      int y = panelY + headerHeight + 8;
      for (int i = 0; i < MODULE_NAMES.length; i++) {
         Module module = ModuleManager.getInstance().getModule(MODULE_NAMES[i]);
         boolean enabled = module != null && module.isEnabled();
         boolean hovered = isInside(mouseX, mouseY, panelX + 8, y, panelWidth - 16, rowHeight - 4);
         int rowColor = hovered ? 0xCC2D2F37 : 0xAA24252B;
         context.fill(panelX + 8, y, panelX + panelWidth - 8, y + rowHeight - 4, rowColor);
         context.fill(panelX + 8, y, panelX + 11, y + rowHeight - 4, enabled ? accent : 0xFF555761);
         context.drawTextWithShadow(this.textRenderer, Text.literal(LABELS[i]), panelX + 18, y + 6, enabled ? 0xFFFFFFFF : 0xFFB0B2BA);
         String state = enabled ? "ON" : "OFF";
         int sw = this.textRenderer.getWidth(state);
         context.drawTextWithShadow(this.textRenderer, Text.literal(state), panelX + panelWidth - sw - 16, y + 6, enabled ? accent : 0xFF777983);
         y += rowHeight;
      }

      boolean themeHover = isInside(mouseX, mouseY, panelX + 8, y + 2, panelWidth - 16, 20);
      context.fill(panelX + 8, y + 2, panelX + panelWidth - 8, y + 22, themeHover ? 0xCC2D2F37 : 0xAA24252B);
      context.drawTextWithShadow(this.textRenderer, Text.literal("Theme: " + THEMES[themeIndex]), panelX + 14, y + 8, accent);

      boolean guiHover = isInside(mouseX, mouseY, panelX + 8, y + 26, panelWidth - 16, 20);
      context.fill(panelX + 8, y + 26, panelX + panelWidth - 8, y + 46, guiHover ? 0xCC2D2F37 : 0xAA24252B);
      context.drawTextWithShadow(this.textRenderer, Text.literal("Open full client GUI"), panelX + 14, y + 32, 0xFFE0E0E6);
   }

   @Override
   public boolean mouseClicked(Click click, boolean doubled) {
      double mouseX = click.x();
      double mouseY = click.y();
      int button = click.button();

      if (button == 0 && isInside(mouseX, mouseY, panelX, panelY, panelWidth, headerHeight)) {
         dragging = true;
         dragOffsetX = (int) mouseX - panelX;
         dragOffsetY = (int) mouseY - panelY;
         return true;
      }

      if (button == 0) {
         int y = panelY + headerHeight + 8;
         for (int i = 0; i < MODULE_NAMES.length; i++) {
            if (isInside(mouseX, mouseY, panelX + 8, y, panelWidth - 16, rowHeight - 4)) {
               Module module = ModuleManager.getInstance().getModule(MODULE_NAMES[i]);
               if (module != null) module.toggle();
               return true;
            }
            y += rowHeight;
         }

         if (isInside(mouseX, mouseY, panelX + 8, y + 2, panelWidth - 16, 20)) {
            themeIndex = (themeIndex + 1) % THEMES.length;
            return true;
         }

         if (isInside(mouseX, mouseY, panelX + 8, y + 26, panelWidth - 16, 20)) {
            if (this.client != null) this.client.setScreen(new ReallyVisualsScreen());
            return true;
         }
      }

      return super.mouseClicked(click, doubled);
   }

   @Override
   public boolean mouseDragged(Click click, double offsetX, double offsetY) {
      if (dragging && click.button() == 0) {
         panelX = clamp((int) click.x() - dragOffsetX, 2, Math.max(2, this.width - panelWidth - 2));
         panelY = clamp((int) click.y() - dragOffsetY, 2, Math.max(2, this.height - 64));
         return true;
      }
      return super.mouseDragged(click, offsetX, offsetY);
   }

   @Override
   public boolean mouseReleased(Click click) {
      dragging = false;
      return super.mouseReleased(click);
   }

   private int getActiveCount() {
      int count = 0;
      for (String moduleName : MODULE_NAMES) {
         Module module = ModuleManager.getInstance().getModule(moduleName);
         if (module != null && module.isEnabled()) count++;
      }
      return count;
   }

   private static boolean isInside(double mouseX, double mouseY, int x, int y, int w, int h) {
      return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
   }

   private static int clamp(int value, int min, int max) {
      return Math.max(min, Math.min(max, value));
   }
}
