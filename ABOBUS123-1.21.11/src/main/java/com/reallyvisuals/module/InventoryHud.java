package com.reallyvisuals.module;

import com.reallyvisuals.gui.HUDManager;
import com.reallyvisuals.gui.render.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

public class InventoryHud extends Module {
   public Module.BooleanSetting background = new Module.BooleanSetting("Фон", true);
   public Module.BooleanSetting slotBackground = new Module.BooleanSetting("Подложка слотов", true);

   public InventoryHud() {
      super("Inventory HUD", "Отображение инвентаря на экране", Category.HUD, false, false);
      this.addSetting(this.background);
      this.addSetting(this.slotBackground);
   }

   @Override
   public void onRenderHUD(DrawContext context) {
      if (this.isEnabled()) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.player != null && mc.world != null) {
            int slotSize = 18;
            int gap = 2;
            int cols = 9;
            int rows = 3;
            int padding = 4;
            int gridW = cols * (slotSize + gap) - gap;
            int gridH = rows * (slotSize + gap) - gap;
            int cardW = gridW + padding * 2;
            int cardH = gridH + padding * 2;
            HUDManager.inventoryHud.setContentSize(cardW, cardH);
            HUDManager.inventoryHud.beginScale(context);
            int x = (int)HUDManager.inventoryHud.x;
            int y = (int)HUDManager.inventoryHud.y;
            if (this.background.value) {
               RenderUtils.drawRoundedRect(context, x, y, cardW, cardH, 8.0F, -300871403);
               RenderUtils.drawRoundedRect(context, x + 1, y + 1, cardW - 2, cardH - 2, 7.5F, -15461351);
            }

            for (int row = 0; row < rows; row++) {
               for (int col = 0; col < cols; col++) {
                  int slotIndex = 9 + row * cols + col;
                  int slotX = x + padding + col * (slotSize + gap);
                  int slotY = y + padding + row * (slotSize + gap);
                  if (this.slotBackground.value) {
                     RenderUtils.drawRoundedRect(context, slotX, slotY, slotSize, slotSize, 4.0F, -15198179);
                  }

                  ItemStack stack;
                  if (slotIndex < mc.player.getInventory().getMainStacks().size() && !(stack = (ItemStack)mc.player.getInventory().getMainStacks().get(slotIndex)).isEmpty()) {
                     try {
                        int itemX = slotX + 1;
                        int itemY = slotY + 1;
                        context.drawItem(stack, itemX, itemY);
                        context.drawStackOverlay(mc.textRenderer, stack, itemX, itemY);
                     } catch (Exception var22) {
                     }
                  }
               }
            }

            HUDManager.inventoryHud.endScale(context);
         }
      }
   }
}
