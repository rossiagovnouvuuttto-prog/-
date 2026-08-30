package com.reallyvisuals.gui;

import com.reallyvisuals.gui.font.CustomFont;
import com.reallyvisuals.gui.font.FontManager;
import com.reallyvisuals.gui.render.CrosshairRenderUtils;
import com.reallyvisuals.gui.render.RenderUtils;
import com.reallyvisuals.module.Crosshair;
import com.reallyvisuals.module.Module;
import com.reallyvisuals.utils.AnimationUtils;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class CrosshairEditorScreen extends Screen {
   private final Screen parentScreen;
   private final Crosshair module;
   private boolean typeDropdownOpen = false;
   private int typeDropdownX;
   private int typeDropdownY;
   private int typeDropdownW;
   private int linesDropdownX;
   private int linesDropdownY;
   private int linesDropdownW;
    private final AnimationUtils.Animation openYAnim = new AnimationUtils.Animation(24.0F);

   public CrosshairEditorScreen(Screen parentScreen, Crosshair module) {
      super(Text.literal("Crosshair Editor"));
      this.parentScreen = parentScreen;
      this.module = module;
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
      CrosshairRenderUtils.renderCrosshair(context, this.module, this.width / 2.0F, this.height / 2.0F);
      int panelW = 240;
      int panelH = 410;
      int panelLeft = this.width - panelW - 30;
      int panelTop = (this.height - panelH) / 2;
      RenderUtils.drawGlow(context, panelLeft + 20, panelTop + panelH - 20, panelW - 40, 15.0F, 10.0F, ReallyVisualsScreen.accentColor, 10, 0.15F);
      RenderUtils.drawRoundedRect(context, panelLeft - 1, panelTop - 1, panelW + 2, panelH + 2, 12.0F, -15066593);
      RenderUtils.drawRoundedRect(context, panelLeft, panelTop, panelW, panelH, 12.0F, -15461351);
      RenderUtils.drawTargetIcon(context, panelLeft + 16, panelTop + 16, 12.0F, ReallyVisualsScreen.accentColor);
      titleFont.drawString(context, "Crosshair", panelLeft + 34, panelTop + 15, -1);
      subFont.drawString(context, "Настраивай свой кастомный прицел!", panelLeft + 16, panelTop + 32, -8750459);
      int currentY = panelTop + 48;
      int cardWidth = panelW - 24;
      String activeType = this.module.type.value;
      subFont.drawString(context, "Тип", panelLeft + 12, currentY, -1);
      int typeBoxY = currentY + 12;
      RenderUtils.drawRoundedRect(context, panelLeft + 12, typeBoxY, cardWidth, 20.0F, 6.0F, -15198179);
      iconFont.drawString(context, "\ue910", panelLeft + 20, typeBoxY + 6, -10132112);
      subFont.drawString(context, activeType, panelLeft + 36, typeBoxY + 6, -1);
      context.getMatrices().pushMatrix();
      float arrowScaleY = this.typeDropdownOpen ? -1.0F : 1.0F;
      context.getMatrices().translate((float) (panelLeft + cardWidth - 2), (float) (typeBoxY + 10));
      context.getMatrices().scale((float) (1.0F), (float) (arrowScaleY));
      iconFont.drawString(context, "\ue90d", -4.0F, -4.0F, this.typeDropdownOpen ? -1 : -10132112);
      context.getMatrices().popMatrix();
      this.typeDropdownX = panelLeft + 12;
      this.typeDropdownY = typeBoxY;
      this.typeDropdownW = cardWidth;
      currentY += 36;
      if ("Крест".equals(activeType)) {
         currentY = this.renderNumberSetting(context, subFont, panelLeft + 12, currentY, cardWidth, this.module.size);
         currentY = this.renderNumberSetting(context, subFont, panelLeft + 12, currentY, cardWidth, this.module.thickness);
         currentY = this.renderNumberSetting(context, subFont, panelLeft + 12, currentY, cardWidth, this.module.gap);
         subFont.drawString(context, "Линии", panelLeft + 12, currentY, -1);
         int linesBoxY = currentY + 12;
         RenderUtils.drawRoundedRect(context, panelLeft + 12, linesBoxY, cardWidth, 20.0F, 6.0F, -15198179);
         iconFont.drawString(context, "\ue910", panelLeft + 20, linesBoxY + 6, -10132112);
         subFont.drawString(context, this.module.getLinesString(), panelLeft + 36, linesBoxY + 6, -1);
         context.getMatrices().pushMatrix();
         float linesArrowY = this.module.linesDropdownOpen ? -1.0F : 1.0F;
         context.getMatrices().translate((float) (panelLeft + cardWidth - 2), (float) (linesBoxY + 10));
         context.getMatrices().scale((float) (1.0F), (float) (linesArrowY));
         iconFont.drawString(context, "\ue90d", -4.0F, -4.0F, this.module.linesDropdownOpen ? -1 : -10132112);
         context.getMatrices().popMatrix();
         this.linesDropdownX = panelLeft + 12;
         this.linesDropdownY = linesBoxY;
         this.linesDropdownW = cardWidth;
         currentY += 36;
      } else if ("Круг".equals(activeType)) {
         currentY = this.renderNumberSetting(context, subFont, panelLeft + 12, currentY, cardWidth, this.module.circleRadius);
         currentY = this.renderNumberSetting(context, subFont, panelLeft + 12, currentY, cardWidth, this.module.circleThickness);
      }

      currentY = this.renderBooleanSetting(context, subFont, panelLeft + 12, currentY, cardWidth, this.module.outline);
      if (!"Точка".equals(activeType)) {
         currentY = this.renderBooleanSetting(context, subFont, panelLeft + 12, currentY, cardWidth, this.module.dot);
      }

      if (this.module.dot.value || "Точка".equals(activeType)) {
         currentY = this.renderNumberSetting(context, subFont, panelLeft + 12, currentY, cardWidth, this.module.dotSize);
      }

      currentY = this.renderNumberSetting(context, subFont, panelLeft + 12, currentY, cardWidth, this.module.rotation);
      if (!"Точка".equals(activeType)) {
         currentY = this.renderBooleanSetting(context, subFont, panelLeft + 12, currentY, cardWidth, this.module.dynamic);
      }

      currentY = this.renderBooleanSetting(context, subFont, panelLeft + 12, currentY, cardWidth, this.module.targetColor);
      currentY = this.renderBooleanSetting(context, subFont, panelLeft + 12, currentY, cardWidth, this.module.clientColor);
      if (!this.module.clientColor.value) {
         subFont.drawString(context, "Кастомный цвет", panelLeft + 12, currentY + 2, -1);
         int boxX = panelLeft + cardWidth + 12 - 75;
         int boxY = currentY;
         RenderUtils.drawRoundedRect(context, boxX, boxY, 75.0F, 14.0F, 4.0F, -15198179);
         RenderUtils.drawCircle(context, boxX + 10, boxY + 7, 4.0F, this.module.customColor);
         subFont.drawString(context, "#" + Integer.toHexString(this.module.customColor & 16777215).toUpperCase(), boxX + 20, boxY + 3, -1);
      }

      int presetBtnY = panelTop + panelH - 62;
      boolean presetHovered = mouseX >= panelLeft + 12 && mouseX <= panelLeft + 12 + cardWidth && mouseY >= presetBtnY && mouseY <= presetBtnY + 24;
      RenderUtils.drawRoundedRect(context, panelLeft + 12, presetBtnY, cardWidth, 24.0F, 8.0F, presetHovered ? -14408660 : -15198179);
      subFont.drawString(
         context, "Сохраненные прицелы", panelLeft + 12 + (cardWidth - subFont.getStringWidth("Сохраненные прицелы")) / 2.0F, presetBtnY + 7, -1
      );
      int bottomY = panelTop + panelH - 32;
      int btnW = (cardWidth - 8) / 2;
      int saveX = panelLeft + 12;
      boolean saveHovered = mouseX >= saveX && mouseX <= saveX + btnW && mouseY >= bottomY && mouseY <= bottomY + 22;
      RenderUtils.drawRoundedRect(context, saveX, bottomY, btnW, 22.0F, 7.0F, saveHovered ? -14408660 : -15198179);
      subFont.drawString(context, "Сохранить", saveX + (btnW - subFont.getStringWidth("Сохранить")) / 2.0F, bottomY + 6, -1);
      int cancelX = saveX + btnW + 8;
      boolean cancelHovered = mouseX >= cancelX && mouseX <= cancelX + btnW && mouseY >= bottomY && mouseY <= bottomY + 22;
      RenderUtils.drawRoundedRect(context, cancelX, bottomY, btnW, 22.0F, 7.0F, cancelHovered ? -14408660 : -14935006);
      subFont.drawString(context, "Отмена", cancelX + (btnW - subFont.getStringWidth("Отмена")) / 2.0F, bottomY + 6, -1);
      if (this.typeDropdownOpen) {
         String[] modes = this.module.type.modes;
         int dropH = modes.length * 20;
         RenderUtils.drawRoundedRect(context, this.typeDropdownX - 1, this.typeDropdownY + 22, this.typeDropdownW + 2, dropH + 2, 6.0F, -15066593);
         RenderUtils.drawRoundedRect(context, this.typeDropdownX, this.typeDropdownY + 23, this.typeDropdownW, dropH, 6.0F, -15461351);

         for (int k = 0; k < modes.length; k++) {
            String m = modes[k];
            int yOffset = this.typeDropdownY + 23 + k * 20;
            boolean isHovered = mouseX >= this.typeDropdownX && mouseX <= this.typeDropdownX + this.typeDropdownW && mouseY >= yOffset && mouseY < yOffset + 20;
            if (m.equals(this.module.type.value)) {
               RenderUtils.drawRoundedRect(context, this.typeDropdownX, yOffset, this.typeDropdownW, 20.0F, 6.0F, -35072);
               subFont.drawString(context, m, this.typeDropdownX + 10, yOffset + 6, -1);
            } else {
               if (isHovered) {
                  RenderUtils.drawRoundedRect(context, this.typeDropdownX, yOffset, this.typeDropdownW, 20.0F, 6.0F, 553648127);
               }

               subFont.drawString(context, m, this.typeDropdownX + 10, yOffset + 6, -2960681);
            }
         }
      }

      if (this.module.linesDropdownOpen && "Крест".equals(activeType)) {
         String[] lineOptions = new String[]{"Верх", "Низ", "Лево", "Право"};
         boolean[] selected = new boolean[]{this.module.lineTop, this.module.lineBottom, this.module.lineLeft, this.module.lineRight};
         int dropH = lineOptions.length * 24;
         RenderUtils.drawRoundedRect(context, this.linesDropdownX - 1, this.linesDropdownY + 22, this.linesDropdownW + 2, dropH + 2, 6.0F, -15066593);
         RenderUtils.drawRoundedRect(context, this.linesDropdownX, this.linesDropdownY + 23, this.linesDropdownW, dropH, 6.0F, -15461351);

         for (int k = 0; k < lineOptions.length; k++) {
            String option = lineOptions[k];
            boolean isSel = selected[k];
            int yOffset = this.linesDropdownY + 25 + k * 23;
            boolean isHovered = mouseX >= this.linesDropdownX
               && mouseX <= this.linesDropdownX + this.linesDropdownW
               && mouseY >= yOffset
               && mouseY < yOffset + 21;
            if (isSel) {
               RenderUtils.drawRoundedRect(context, this.linesDropdownX + 4, yOffset, this.linesDropdownW - 8, 20.0F, 6.0F, ReallyVisualsScreen.accentColor);
               subFont.drawString(context, option, this.linesDropdownX + 14, yOffset + 6, -1);
            } else {
               if (isHovered) {
                  RenderUtils.drawRoundedRect(context, this.linesDropdownX + 4, yOffset, this.linesDropdownW - 8, 20.0F, 6.0F, -14408660);
               }

               subFont.drawString(context, option, this.linesDropdownX + 14, yOffset + 6, -2960681);
            }
         }
      }

      context.getMatrices().popMatrix();

      super.render(context, mouseX, mouseY, delta);
   }

   private int renderNumberSetting(DrawContext context, CustomFont font, int x, int y, int cardWidth, Module.NumberSetting num) {
      font.drawString(context, num.name, x, y, -1);
      String valStr = String.format("%.1f", num.value).replace(".", ",");
      font.drawString(context, valStr, x + cardWidth - font.getStringWidth(valStr), y, -35072);
      int trackY = y + 14;
      RenderUtils.drawRoundedRect(context, x, trackY, cardWidth, 2.0F, 1.0F, -14935006);
      float fillRatio = (float)((num.value - num.min) / (num.max - num.min));
      RenderUtils.drawRoundedRect(context, x, trackY, (int)(cardWidth * fillRatio), 2.0F, 1.0F, -35072);
      float knobX = x + cardWidth * fillRatio - 2.5F;
      RenderUtils.drawRoundedRect(context, knobX, trackY - 1.5F, 5.0F, 5.0F, 3.0F, -1);
      return y + 26;
   }

   private int renderBooleanSetting(DrawContext context, CustomFont font, int x, int y, int cardWidth, Module.BooleanSetting bool) {
      font.drawString(context, bool.name, x, y + 2, -1);
      int boxSize = 12;
      int boxX = x + cardWidth - boxSize;
      int boxY = y;
      if (bool.value) {
         RenderUtils.drawRoundedRect(context, boxX, boxY, boxSize, boxSize, 4.0F, -35072);
         RenderUtils.drawCheckmark(context, boxX, boxY, boxSize, -1);
      } else {
         RenderUtils.drawRoundedRect(context, boxX, boxY, boxSize, boxSize, 4.0F, -14935006);
      }

      return y + 20;
   }

   public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
      double mouseX = click.x(); double mouseY = click.y(); int button = click.button();
      float openSlide = this.openYAnim.getValue();
      if (openSlide > 0.5F) {
         mouseY -= openSlide;
      }

      if (this.typeDropdownOpen) {
         String[] modes = this.module.type.modes;
         int dropH = modes.length * 20;
         int index;
         if (mouseX >= this.typeDropdownX
            && mouseX <= this.typeDropdownX + this.typeDropdownW
            && mouseY >= this.typeDropdownY + 23
            && mouseY <= this.typeDropdownY + 23 + dropH
            && (index = (int)((mouseY - (this.typeDropdownY + 23)) / 20.0)) >= 0
            && index < modes.length) {
            this.module.type.value = modes[index];
         }

         this.typeDropdownOpen = false;
         return true;
      } else if (this.module.linesDropdownOpen && "Крест".equals(this.module.type.value)) {
         int dropH = 96;
         if (mouseX >= this.linesDropdownX
            && mouseX <= this.linesDropdownX + this.linesDropdownW
            && mouseY >= this.linesDropdownY + 23
            && mouseY <= this.linesDropdownY + 23 + dropH) {
            int index = (int)((mouseY - (this.linesDropdownY + 23)) / 24.0);
            if (index == 0) {
               this.module.lineTop = !this.module.lineTop;
            } else if (index == 1) {
               this.module.lineBottom = !this.module.lineBottom;
            } else if (index == 2) {
               this.module.lineLeft = !this.module.lineLeft;
            } else if (index == 3) {
               this.module.lineRight = !this.module.lineRight;
            }

            return true;
         } else {
            this.module.linesDropdownOpen = false;
            return true;
         }
      } else {
         int panelW = 240;
         int panelH = 410;
         int panelLeft = this.width - panelW - 30;
         int panelTop = (this.height - panelH) / 2;
         int cardWidth = panelW - 24;
         int currentY = panelTop + 48;
         String activeType = this.module.type.value;
         if (mouseY >= currentY + 12 && mouseY <= currentY + 32) {
            this.typeDropdownOpen = !this.typeDropdownOpen;
            return true;
         }

         currentY += 36;
         if ("Крест".equals(activeType)) {
            currentY = this.handleNumberClick(mouseX, mouseY, panelLeft + 12, currentY, cardWidth, this.module.size);
            currentY = this.handleNumberClick(mouseX, mouseY, panelLeft + 12, currentY, cardWidth, this.module.thickness);
            if (mouseY >= (currentY = this.handleNumberClick(mouseX, mouseY, panelLeft + 12, currentY, cardWidth, this.module.gap)) + 12
               && mouseY <= currentY + 32) {
               this.module.linesDropdownOpen = !this.module.linesDropdownOpen;
               return true;
            }

            currentY += 36;
         } else if ("Круг".equals(activeType)) {
            currentY = this.handleNumberClick(mouseX, mouseY, panelLeft + 12, currentY, cardWidth, this.module.circleRadius);
            currentY = this.handleNumberClick(mouseX, mouseY, panelLeft + 12, currentY, cardWidth, this.module.circleThickness);
         }

         currentY = this.handleBooleanClick(mouseX, mouseY, panelLeft + 12, currentY, cardWidth, this.module.outline);
         if (!"Точка".equals(activeType)) {
            currentY = this.handleBooleanClick(mouseX, mouseY, panelLeft + 12, currentY, cardWidth, this.module.dot);
         }

         if (this.module.dot.value || "Точка".equals(activeType)) {
            currentY = this.handleNumberClick(mouseX, mouseY, panelLeft + 12, currentY, cardWidth, this.module.dotSize);
         }

         currentY = this.handleNumberClick(mouseX, mouseY, panelLeft + 12, currentY, cardWidth, this.module.rotation);
         if (!"Точка".equals(activeType)) {
            currentY = this.handleBooleanClick(mouseX, mouseY, panelLeft + 12, currentY, cardWidth, this.module.dynamic);
         }

         currentY = this.handleBooleanClick(mouseX, mouseY, panelLeft + 12, currentY, cardWidth, this.module.targetColor);
         currentY = this.handleBooleanClick(mouseX, mouseY, panelLeft + 12, currentY, cardWidth, this.module.clientColor);
         int presetBtnY = panelTop + panelH - 62;
         if (mouseX >= panelLeft + 12 && mouseX <= panelLeft + 12 + cardWidth && mouseY >= presetBtnY && mouseY <= presetBtnY + 24) {
            this.client.setScreen(new CrosshairPresetsDialog(this, this.module));
            return true;
         } else {
            int bottomY = panelTop + panelH - 32;
            int btnW = (cardWidth - 8) / 2;
            if (mouseX >= panelLeft + 12 && mouseX <= panelLeft + panelW - 12 && mouseY >= bottomY && mouseY <= bottomY + 22) {
               this.client.setScreen(this.parentScreen);
               return true;
            } else {
               return super.mouseClicked(click, doubled);
            }
         }
      }
   }

   private int handleNumberClick(double mouseX, double mouseY, int x, int y, int cardWidth, Module.NumberSetting num) {
      if (mouseY >= y + 8 && mouseY <= y + 24) {
         float ratio = (float)(mouseX - x) / cardWidth;
         ratio = Math.max(0.0F, Math.min(1.0F, ratio));
         double val = num.min + ratio * (num.max - num.min);
         num.value = Math.round(val / num.inc) * num.inc;
      }

      return y + 26;
   }

   private int handleBooleanClick(double mouseX, double mouseY, int x, int y, int cardWidth, Module.BooleanSetting bool) {
      if (mouseY >= y && mouseY <= y + 18) {
         bool.value = !bool.value;
      }

      return y + 20;
   }

   public boolean mouseDragged(net.minecraft.client.gui.Click click, double deltaX, double deltaY) {
      double mouseX = click.x(); double mouseY = click.y(); int button = click.button();
      int panelW = 240;
      int panelH = 410;
      int panelLeft = this.width - panelW - 30;
      int panelTop = (this.height - panelH) / 2;
      int cardWidth = panelW - 24;
      int currentY = panelTop + 48;
      String activeType = this.module.type.value;
      currentY += 36;
      if ("Крест".equals(activeType)) {
         currentY = this.handleNumberClick(mouseX, mouseY, panelLeft + 12, currentY, cardWidth, this.module.size);
         currentY = this.handleNumberClick(mouseX, mouseY, panelLeft + 12, currentY, cardWidth, this.module.thickness);
         currentY = this.handleNumberClick(mouseX, mouseY, panelLeft + 12, currentY, cardWidth, this.module.gap);
         currentY += 36;
      } else if ("Круг".equals(activeType)) {
         currentY = this.handleNumberClick(mouseX, mouseY, panelLeft + 12, currentY, cardWidth, this.module.circleRadius);
         currentY = this.handleNumberClick(mouseX, mouseY, panelLeft + 12, currentY, cardWidth, this.module.circleThickness);
      }

      currentY += 20;
      if (!"Точка".equals(activeType)) {
         currentY += 20;
      }

      if (this.module.dot.value || "Точка".equals(activeType)) {
         currentY = this.handleNumberClick(mouseX, mouseY, panelLeft + 12, currentY, cardWidth, this.module.dotSize);
      }

      currentY = this.handleNumberClick(mouseX, mouseY, panelLeft + 12, currentY, cardWidth, this.module.rotation);
      return super.mouseDragged(click, deltaX, deltaY);
   }
}
