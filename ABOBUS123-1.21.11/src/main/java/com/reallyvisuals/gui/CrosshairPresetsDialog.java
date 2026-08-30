package com.reallyvisuals.gui;

import com.reallyvisuals.gui.font.CustomFont;
import com.reallyvisuals.gui.font.FontManager;
import com.reallyvisuals.gui.render.CrosshairRenderUtils;
import com.reallyvisuals.gui.render.RenderUtils;
import com.reallyvisuals.module.Crosshair;
import com.reallyvisuals.utils.AnimationUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class CrosshairPresetsDialog extends Screen {
   private final Screen parentScreen;
   private final Crosshair module;
   private int selectedPresetIndex = -1;
   private final List<CrosshairPresetsDialog.Preset> presets = new ArrayList<>();
    private final AnimationUtils.Animation openYAnim = new AnimationUtils.Animation(24.0F);

   public CrosshairPresetsDialog(Screen parentScreen, Crosshair module) {
      super(Text.literal("Crosshair Presets"));
      this.parentScreen = parentScreen;
      this.module = module;
      this.presets.add(new CrosshairPresetsDialog.Preset("Классика", "Крест", 4.0, 1.0, 2.0, true, true, true, true, true, false, 2.0, 0.0, 5.0, 1.0, -1));
      this.presets
         .add(new CrosshairPresetsDialog.Preset("Круг и Точка", "Круг", 3.0, 1.0, 1.0, true, true, true, true, true, true, 2.0, 0.0, 4.5, 1.0, -16716050));
      this.presets
         .add(new CrosshairPresetsDialog.Preset("T-образный", "Крест", 4.0, 1.5, 2.0, false, true, true, true, true, false, 2.0, 0.0, 5.0, 1.0, -16711936));
      this.presets
         .add(new CrosshairPresetsDialog.Preset("Красный X", "Крест", 4.0, 1.5, 2.0, true, true, true, true, true, false, 2.0, 45.0, 5.0, 1.0, -56798));
   }

   protected void init() {
      super.init();
      this.openYAnim.force(24.0F);
      this.openYAnim.setEasing(1);
      this.openYAnim.animateTo(0.0F, 350L);
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      this.renderBackground(context, mouseX, mouseY, delta);
      context.getMatrices().pushMatrix();
      context.getMatrices().translate((float) (0.0F), (float) (this.openYAnim.getValue()));
      CustomFont mainFont = FontManager.getMainFont();
      CustomFont titleFont = FontManager.getTitleFont();
      CustomFont subFont = FontManager.getSubFont();
      CustomFont iconFont = FontManager.getIconFont();
      int dialogWidth = 300;
      int dialogHeight = 380;
      int left = (this.width - dialogWidth) / 2;
      int top = (this.height - dialogHeight) / 2;
      RenderUtils.drawRoundedRect(context, left - 1, top - 1, dialogWidth + 2, dialogHeight + 2, 12.0F, -15066593);
      RenderUtils.drawRoundedRect(context, left, top, dialogWidth, dialogHeight, 12.0F, -15461351);
      RenderUtils.drawTargetIcon(context, left + 18, top + 18, 12.0F, ReallyVisualsScreen.accentColor);
      titleFont.drawString(context, "Сохраненные прицелы", left + 36, top + 17, -1);
      subFont.drawString(context, "Больше прицелов на нашем сайте ", left + 18, top + 34, -8750459);
      int w1 = subFont.getStringWidth("Больше прицелов на нашем сайте ");
      subFont.drawString(context, "ABOBUS123", left + 18 + w1, top + 34, ReallyVisualsScreen.accentColor);
      int cols = 4;
      int rows = 5;
      int cellW = 54;
      int cellH = 42;
      int gapX = 10;
      int gapY = 8;
      int startX = left + 18;
      int startY = top + 52;

      for (int r = 0; r < rows; r++) {
         for (int c = 0; c < cols; c++) {
            int index = r * cols + c;
            int cellX = startX + c * (cellW + gapX);
            int cellY = startY + r * (cellH + gapY);
            boolean isSelected = index == this.selectedPresetIndex;
            boolean isHovered = mouseX >= cellX && mouseX <= cellX + cellW && mouseY >= cellY && mouseY <= cellY + cellH;
            RenderUtils.drawRoundedRect(context, cellX, cellY, cellW, cellH, 10.0F, -15000798);
            if (isSelected || isHovered) {
               RenderUtils.drawSingleRoundedRect(context, cellX - 1, cellY - 1, cellW + 2, cellH + 2, 11.0F, ReallyVisualsScreen.accentColor);
               RenderUtils.drawRoundedRect(context, cellX, cellY, cellW, cellH, 10.0F, -15461351);
            }

            if (index < this.presets.size()) {
               CrosshairPresetsDialog.Preset p = this.presets.get(index);
               Crosshair temp = new Crosshair();
               temp.type.value = p.type;
               temp.size.value = p.size;
               temp.thickness.value = p.thickness;
               temp.gap.value = p.gap;
               temp.lineTop = p.lineTop;
               temp.lineBottom = p.lineBottom;
               temp.lineLeft = p.lineLeft;
               temp.lineRight = p.lineRight;
               temp.outline.value = p.outline;
               temp.dot.value = p.dot;
               temp.dotSize.value = p.dotSize;
               temp.rotation.value = p.rotation;
               temp.circleRadius.value = p.circleRadius;
               temp.circleThickness.value = p.circleThickness;
               temp.customColor = p.color;
               CrosshairRenderUtils.renderCrosshair(context, temp, cellX + cellW / 2.0F, cellY + cellH / 2.0F);
            }
         }
      }

      int btnY = top + dialogHeight - 38;
      int btnW = 120;
      int btnH = 24;
      int applyX = left + 18;
      if (this.selectedPresetIndex >= 0) {
         int accent = ReallyVisualsScreen.accentColor;
         RenderUtils.drawGlow(context, applyX, btnY, btnW, btnH, 12.0F, accent, 4, 0.2F);
         RenderUtils.drawRoundedRect(context, applyX, btnY, btnW, btnH, 12.0F, accent);
         mainFont.drawString(context, "Применить", applyX + (btnW - mainFont.getStringWidth("Применить")) / 2.0F, btnY + 7, -1);
      } else {
         RenderUtils.drawRoundedRect(context, applyX, btnY, btnW, btnH, 12.0F, -15198179);
         subFont.drawString(context, "Применить", applyX + (btnW - subFont.getStringWidth("Применить")) / 2.0F, btnY + 7, -10132112);
      }

      int cancelX = left + dialogWidth - btnW - 18;
      boolean cancelHovered = mouseX >= cancelX && mouseX <= cancelX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
      RenderUtils.drawRoundedRect(context, cancelX, btnY, btnW, btnH, 12.0F, cancelHovered ? -14408660 : -15198179);
      mainFont.drawString(context, "Отмена", cancelX + (btnW - mainFont.getStringWidth("Отмена")) / 2.0F, btnY + 7, -1);
      context.getMatrices().popMatrix();

      super.render(context, mouseX, mouseY, delta);
   }

   public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
      double mouseX = click.x(); double mouseY = click.y(); int button = click.button();
      float openSlide = this.openYAnim.getValue();
      if (openSlide > 0.5F) {
         mouseY -= openSlide;
      }

      int dialogWidth = 300;
      int dialogHeight = 380;
      int left = (this.width - dialogWidth) / 2;
      int top = (this.height - dialogHeight) / 2;
      int cols = 4;
      int rows = 5;
      int cellW = 54;
      int cellH = 42;
      int gapX = 10;
      int gapY = 8;
      int startX = left + 18;
      int startY = top + 52;

      for (int r = 0; r < rows; r++) {
         for (int c = 0; c < cols; c++) {
            int index = r * cols + c;
            int cellX = startX + c * (cellW + gapX);
            int cellY = startY + r * (cellH + gapY);
            if (mouseX >= cellX && mouseX <= cellX + cellW && mouseY >= cellY && mouseY <= cellY + cellH) {
               this.selectedPresetIndex = index;
               return true;
            }
         }
      }

      int btnY = top + dialogHeight - 38;
      int btnW = 120;
      int btnH = 24;
      int applyX = left + 18;
      if (this.selectedPresetIndex >= 0
         && this.selectedPresetIndex < this.presets.size()
         && mouseX >= applyX
         && mouseX <= applyX + btnW
         && mouseY >= btnY
         && mouseY <= btnY + btnH) {
         CrosshairPresetsDialog.Preset p = this.presets.get(this.selectedPresetIndex);
         this.module.type.value = p.type;
         this.module.size.value = p.size;
         this.module.thickness.value = p.thickness;
         this.module.gap.value = p.gap;
         this.module.lineTop = p.lineTop;
         this.module.lineBottom = p.lineBottom;
         this.module.lineLeft = p.lineLeft;
         this.module.lineRight = p.lineRight;
         this.module.outline.value = p.outline;
         this.module.dot.value = p.dot;
         this.module.dotSize.value = p.dotSize;
         this.module.rotation.value = p.rotation;
         this.module.circleRadius.value = p.circleRadius;
         this.module.circleThickness.value = p.circleThickness;
         this.module.customColor = p.color;
         this.client.setScreen(this.parentScreen);
         return true;
      } else {
         int cancelX = left + dialogWidth - btnW - 18;
         if (mouseX >= cancelX && mouseX <= cancelX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
            this.client.setScreen(this.parentScreen);
            return true;
         } else {
            return super.mouseClicked(click, doubled);
         }
      }
   }

   public static class Preset {
      public String name;
      public String type;
      public double size;
      public double thickness;
      public double gap;
      public boolean lineTop;
      public boolean lineBottom;
      public boolean lineLeft;
      public boolean lineRight;
      public boolean outline;
      public boolean dot;
      public double dotSize;
      public double rotation;
      public double circleRadius;
      public double circleThickness;
      public int color;

      public Preset(
         String name,
         String type,
         double size,
         double thickness,
         double gap,
         boolean lineTop,
         boolean lineBottom,
         boolean lineLeft,
         boolean lineRight,
         boolean outline,
         boolean dot,
         double dotSize,
         double rotation,
         double circleRadius,
         double circleThickness,
         int color
      ) {
         this.name = name;
         this.type = type;
         this.size = size;
         this.thickness = thickness;
         this.gap = gap;
         this.lineTop = lineTop;
         this.lineBottom = lineBottom;
         this.lineLeft = lineLeft;
         this.lineRight = lineRight;
         this.outline = outline;
         this.dot = dot;
         this.dotSize = dotSize;
         this.rotation = rotation;
         this.circleRadius = circleRadius;
         this.circleThickness = circleThickness;
         this.color = color;
      }
   }
}
