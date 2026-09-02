package com.reallyvisuals.gui;

import com.reallyvisuals.gui.font.CustomFont;
import com.reallyvisuals.gui.font.FontManager;
import com.reallyvisuals.gui.render.RenderUtils;
import com.reallyvisuals.module.CustomHand;
import com.reallyvisuals.module.Module;
import com.reallyvisuals.utils.AnimationUtils;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class CustomHandEditorScreen extends Screen {
   private final Screen parentScreen;
   private final CustomHand module;
   private boolean animDropdownOpen = false;
   private int animDropdownX;
   private int animDropdownY;
   private int animDropdownW;
   private float animDropdownScroll = 0.0F;
   private boolean isDraggingMain = false;
   private boolean isDraggingOff = false;
   private boolean selectedOffHand = false;
   private double lastMouseX;
   private double lastMouseY;
   private final ItemStack swordStack = new ItemStack(Items.DIAMOND_SWORD);
   private final ItemStack totemStack = new ItemStack(Items.TOTEM_OF_UNDYING);
    private final AnimationUtils.Animation openYAnim = new AnimationUtils.Animation(24.0F);

   public CustomHandEditorScreen(Screen parentScreen, CustomHand module) {
      super(Text.literal("Custom Hand Editor"));
      this.parentScreen = parentScreen;
      this.module = module;
   }

   protected void init() {
      super.init();
      this.openYAnim.force(24.0F);
      this.openYAnim.setEasing(1);
      this.openYAnim.animateTo(0.0F, 350L);
   }

   public void tick() {
      super.tick();
      this.module.tickPreview();
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      // 1.21.11 allows exactly one blur per frame and vanilla already spends it
      // before render() is called; renderBackground() would blur a second time and
      // throw "Can only blur once per frame". renderInGameBackground is the
      // darkening half without the blur.
      this.renderInGameBackground(context);
      context.getMatrices().pushMatrix();
      context.getMatrices().translate((float) (0.0F), (float) (this.openYAnim.getValue()));
      CustomFont mainFont = FontManager.getMainFont();
      CustomFont titleFont = FontManager.getTitleFont();
      CustomFont subFont = FontManager.getSubFont();
      CustomFont iconFont = FontManager.getIconFont();
      int screenW = this.width;
      int screenH = this.height;
      int offBoxW = 180;
      int offBoxH = 260;
      int offBoxX = 0;
      int offBoxY = screenH - offBoxH;
      subFont.drawString(
         context, "Доп.", offBoxX + offBoxW / 2.0F - subFont.getStringWidth("Доп.") / 2.0F, offBoxY - 14, this.selectedOffHand ? ReallyVisualsScreen.accentColor : -7303014
      );
      RenderUtils.drawRoundedRect(context, offBoxX, offBoxY, offBoxW, offBoxH, 0.0F, 1073741824);
      RenderUtils.drawSingleRoundedRect(context, offBoxX - 1, offBoxY - 1, offBoxW + 2, offBoxH + 2, 0.0F, this.selectedOffHand ? ReallyVisualsScreen.accentColor : 822083583);
      float offScale = (float)this.module.offHandScale.value * 7.0F;
      float offX = offBoxX + offBoxW / 2.0F + (float)this.module.offHandX.value * 30.0F;
      float offY = offBoxY + offBoxH / 2.0F - (float)this.module.offHandY.value * 30.0F;
      context.getMatrices().pushMatrix();
      context.getMatrices().translate((float) (offX - 8.0F * offScale), (float) (offY - 8.0F * offScale));
      context.getMatrices().scale((float) (offScale), (float) (offScale));
      context.drawItem(this.totemStack, 0, 0);
      context.getMatrices().popMatrix();
      int mainBoxW = 230;
      int mainBoxH = 310;
      int mainBoxX = screenW - mainBoxW;
      int mainBoxY = screenH - mainBoxH;
      subFont.drawString(
         context, "Основная", mainBoxX + mainBoxW / 2.0F - subFont.getStringWidth("Основная") / 2.0F, mainBoxY - 14, !this.selectedOffHand ? ReallyVisualsScreen.accentColor : -7303014
      );
      RenderUtils.drawRoundedRect(context, mainBoxX, mainBoxY, mainBoxW, mainBoxH, 0.0F, 1073741824);
      RenderUtils.drawSingleRoundedRect(context, mainBoxX - 1, mainBoxY - 1, mainBoxW + 2, mainBoxH + 2, 0.0F, !this.selectedOffHand ? ReallyVisualsScreen.accentColor : 822083583);
      float mainScale = (float)this.module.mainHandScale.value * 9.0F;
      float mainX = mainBoxX + mainBoxW / 2.0F + (float)this.module.mainHandX.value * 30.0F;
      float mainY = mainBoxY + mainBoxH / 2.0F - (float)this.module.mainHandY.value * 30.0F;
      context.getMatrices().pushMatrix();
      context.getMatrices().translate((float) (mainX - 8.0F * mainScale), (float) (mainY - 8.0F * mainScale));
      context.getMatrices().scale((float) (mainScale), (float) (mainScale));
      context.drawItem(this.swordStack, 0, 0);
      context.getMatrices().popMatrix();
      int panelW = 220;
      int panelH = 220;
      int panelLeft = (screenW - panelW) / 2;
      int panelTop = 40;
      int instY = panelTop + panelH + 18;
      String line1 = "• Руки можно двигать — просто перетащи их мышкой";
      String line2 = "Колесо мыши — размер • Shift + колесо — глубина • СКМ — сброс";
      subFont.drawString(context, line1, screenW / 2.0F - subFont.getStringWidth(line1) / 2.0F, instY, -1);
      subFont.drawString(context, line2, screenW / 2.0F - subFont.getStringWidth(line2) / 2.0F, instY + 16, -8750459);
      String bottomLine = "Колесо — Масштаб | Shift+Колесо — Глубина | СКМ — Сброс";
      subFont.drawString(context, bottomLine, screenW / 2.0F - subFont.getStringWidth(bottomLine) / 2.0F, screenH - 24, -10132112);
      RenderUtils.drawRoundedRect(context, panelLeft - 1, panelTop - 1, panelW + 2, panelH + 2, 12.0F, -15066593);
      RenderUtils.drawRoundedRect(context, panelLeft, panelTop, panelW, panelH, 12.0F, -15461351);
      iconFont.drawString(context, "\ue917", panelLeft + 16, panelTop + 16, ReallyVisualsScreen.accentColor);
      titleFont.drawString(context, "Custom Hand", panelLeft + 34, panelTop + 15, -1);
      subFont.drawString(context, "Настраивай анимацию руки!", panelLeft + 16, panelTop + 32, -8750459);
      int currentY = panelTop + 48;
      int cardWidth = panelW - 24;
      subFont.drawString(context, "Анимация", panelLeft + 12, currentY, -1);
      int animBoxY = currentY + 12;
      RenderUtils.drawRoundedRect(context, panelLeft + 12, animBoxY, cardWidth, 20.0F, 6.0F, -15198179);
      iconFont.drawString(context, "\ue910", panelLeft + 20, animBoxY + 6, -10132112);
      subFont.drawString(context, this.module.animationMode.value, panelLeft + 36, animBoxY + 6, -1);
      context.getMatrices().pushMatrix();
      float arrowScaleY = this.animDropdownOpen ? -1.0F : 1.0F;
      context.getMatrices().translate((float) (panelLeft + cardWidth - 2), (float) (animBoxY + 10));
      context.getMatrices().scale((float) (1.0F), (float) (arrowScaleY));
      iconFont.drawString(context, "\ue90d", -4.0F, -4.0F, this.animDropdownOpen ? -1 : -10132112);
      context.getMatrices().popMatrix();
      this.animDropdownX = panelLeft + 12;
      this.animDropdownY = animBoxY;
      this.animDropdownW = cardWidth;
      currentY += 36;
      currentY = this.renderNumberSetting(context, subFont, panelLeft + 12, currentY, cardWidth, this.module.animationSpeed);
      currentY = this.renderNumberSetting(context, subFont, panelLeft + 12, currentY, cardWidth, this.module.swingStrength);
      currentY = this.renderBooleanSetting(context, subFont, panelLeft + 12, currentY, cardWidth, this.module.preview);
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
      if (this.animDropdownOpen) {
         String[] modes = this.module.animationMode.modes;
         int maxVisible = 4;
         int itemH = 20;
         int dropH = maxVisible * itemH;
         RenderUtils.drawRoundedRect(context, this.animDropdownX - 1, this.animDropdownY + 22, this.animDropdownW + 2, dropH + 2, 6.0F, -15066593);
         RenderUtils.drawRoundedRect(context, this.animDropdownX, this.animDropdownY + 23, this.animDropdownW, dropH, 6.0F, -15461351);
         int maxScroll = Math.max(0, modes.length - maxVisible);
         this.animDropdownScroll = Math.max(0.0F, Math.min(maxScroll, this.animDropdownScroll));
         int startIndex = (int)this.animDropdownScroll;

         for (int k = 0; k < maxVisible && startIndex + k < modes.length; k++) {
            int modeIdx = startIndex + k;
            String m = modes[modeIdx];
            int yOffset = this.animDropdownY + 23 + k * itemH;
            boolean isHovered = mouseX >= this.animDropdownX
               && mouseX <= this.animDropdownX + this.animDropdownW
               && mouseY >= yOffset
               && mouseY < yOffset + itemH;
            if (m.equals(this.module.animationMode.value)) {
               RenderUtils.drawRoundedRect(context, this.animDropdownX + 2, yOffset + 1, this.animDropdownW - 4, itemH - 2, 5.0F, ReallyVisualsScreen.accentColor);
               subFont.drawString(context, m, this.animDropdownX + 10, yOffset + 6, -1);
            } else {
               if (isHovered) {
                  RenderUtils.drawRoundedRect(context, this.animDropdownX + 2, yOffset + 1, this.animDropdownW - 4, itemH - 2, 5.0F, 553648127);
               }

               subFont.drawString(context, m, this.animDropdownX + 10, yOffset + 6, -2960681);
            }
         }

         if (maxScroll > 0) {
            int scrollbarX = this.animDropdownX + this.animDropdownW - 5;
            int trackH = dropH - 4;
            int thumbH = Math.max(12, trackH * maxVisible / modes.length);
            int thumbY = this.animDropdownY + 25 + (int)(this.animDropdownScroll / maxScroll * (trackH - thumbH));
            RenderUtils.drawRoundedRect(context, scrollbarX, thumbY, 3.0F, thumbH, 1.5F, ReallyVisualsScreen.accentColor);
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

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double amount) {
      if (this.animDropdownOpen) {
         this.animDropdownScroll = (float)(this.animDropdownScroll - amount);
         return true;
      }

      Module.NumberSetting targetScale = this.selectedOffHand ? this.module.offHandScale : this.module.mainHandScale;
      Module.NumberSetting targetZ = this.selectedOffHand ? this.module.offHandZ : this.module.mainHandZ;
      if (com.reallyvisuals.utils.KeyUtils.isKeyPressedSafe(net.minecraft.client.MinecraftClient.getInstance().getWindow().getHandle(), 340)) {
         double deltaZ = amount * 0.05;
         targetZ.value = Math.max(-2.0, Math.min(2.0, targetZ.value + deltaZ));
      } else {
         double deltaScale = amount * 0.05;
         targetScale.value = Math.max(0.5, Math.min(2.0, targetScale.value + deltaScale));
      }

      return super.mouseScrolled(mouseX, mouseY, horizontal, amount);
   }

   public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
      double mouseX = click.x(); double mouseY = click.y(); int button = click.button();
      float openSlide = this.openYAnim.getValue();
      if (openSlide > 0.5F) {
         mouseY -= openSlide;
      }

      if (this.animDropdownOpen) {
         String[] modes = this.module.animationMode.modes;
         int maxVisible = 4;
         int itemH = 20;
         int dropH = maxVisible * itemH;
         int clickedIndex;
         if (mouseX >= this.animDropdownX
            && mouseX <= this.animDropdownX + this.animDropdownW
            && mouseY >= this.animDropdownY + 23
            && mouseY <= this.animDropdownY + 23 + dropH
            && (clickedIndex = (int)((mouseY - (this.animDropdownY + 23)) / itemH) + (int)this.animDropdownScroll) >= 0
            && clickedIndex < modes.length) {
            this.module.animationMode.value = modes[clickedIndex];
         }

         this.animDropdownOpen = false;
         return true;
      } else if (button == 2) {
         this.module.mainHandScale.value = 1.0;
         this.module.mainHandX.value = 0.0;
         this.module.mainHandY.value = 0.0;
         this.module.mainHandZ.value = 0.0;
         this.module.offHandScale.value = 1.0;
         this.module.offHandX.value = 0.0;
         this.module.offHandY.value = 0.0;
         this.module.offHandZ.value = 0.0;
         return true;
      } else {
         int panelW = 220;
         int panelH = 220;
         int panelLeft = (this.width - panelW) / 2;
         int panelTop = 40;
         int cardWidth = panelW - 24;
         int currentY = panelTop + 48;
         if (mouseY >= currentY + 12 && mouseY <= currentY + 32) {
            this.animDropdownOpen = !this.animDropdownOpen;
            return true;
         } else {
            currentY += 36;
            currentY = this.handleNumberClick(mouseX, mouseY, panelLeft + 12, currentY, cardWidth, this.module.animationSpeed);
            currentY = this.handleNumberClick(mouseX, mouseY, panelLeft + 12, currentY, cardWidth, this.module.swingStrength);
            currentY = this.handleBooleanClick(mouseX, mouseY, panelLeft + 12, currentY, cardWidth, this.module.preview);
            int bottomY = panelTop + panelH - 32;
            int btnW = (cardWidth - 8) / 2;
            if (mouseX >= panelLeft + 12 && mouseX <= panelLeft + panelW - 12 && mouseY >= bottomY && mouseY <= bottomY + 22) {
               this.client.setScreen(this.parentScreen);
               return true;
            } else {
               int offBoxW = 180;
               int offBoxH = 260;
               if (mouseX >= 0.0 && mouseX <= offBoxW && mouseY >= this.height - offBoxH) {
                  this.isDraggingOff = true;
                  this.selectedOffHand = true;
                  this.lastMouseX = mouseX;
                  this.lastMouseY = mouseY;
                  return true;
               } else {
                  int mainBoxW = 230;
                  int mainBoxH = 310;
                  if (mouseX >= this.width - mainBoxW && mouseX <= this.width && mouseY >= this.height - mainBoxH) {
                     this.isDraggingMain = true;
                     this.selectedOffHand = false;
                     this.lastMouseX = mouseX;
                     this.lastMouseY = mouseY;
                     return true;
                  } else {
                     return super.mouseClicked(click, doubled);
                  }
               }
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
      if (this.isDraggingMain) {
         double dx = (mouseX - this.lastMouseX) * 0.01;
         double dy = (mouseY - this.lastMouseY) * 0.01;
         this.module.mainHandX.value = Math.max(-2.0, Math.min(2.0, this.module.mainHandX.value + dx));
         this.module.mainHandY.value = Math.max(-2.0, Math.min(2.0, this.module.mainHandY.value - dy));
         this.lastMouseX = mouseX;
         this.lastMouseY = mouseY;
         return true;
      } else if (this.isDraggingOff) {
         double dx = (mouseX - this.lastMouseX) * 0.01;
         double dy = (mouseY - this.lastMouseY) * 0.01;
         this.module.offHandX.value = Math.max(-2.0, Math.min(2.0, this.module.offHandX.value + dx));
         this.module.offHandY.value = Math.max(-2.0, Math.min(2.0, this.module.offHandY.value - dy));
         this.lastMouseX = mouseX;
         this.lastMouseY = mouseY;
         return true;
      } else {
         return super.mouseDragged(click, deltaX, deltaY);
      }
   }

   public boolean mouseReleased(net.minecraft.client.gui.Click click) {
      double mouseX = click.x(); double mouseY = click.y(); int button = click.button();
      this.isDraggingMain = false;
      this.isDraggingOff = false;
      return super.mouseReleased(click);
   }
}
