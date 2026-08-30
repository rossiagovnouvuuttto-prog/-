package com.reallyvisuals.module;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

public class ShulkerPreviewRenderer {
   public static void renderPreview(DrawContext context, ItemStack shulkerStack, int x, int y) {
      if (ShulkerPreview.isShulkerBox(shulkerStack)) {
         MinecraftClient mc = MinecraftClient.getInstance();
         DefaultedList<ItemStack> items = ShulkerPreview.getShulkerItems(shulkerStack);
         int width = 176;
         int height = 76;
         if (x + width > mc.getWindow().getScaledWidth() - 5) {
            x = mc.getWindow().getScaledWidth() - width - 5;
         }

         if (y + height > mc.getWindow().getScaledHeight() - 5) {
            y = mc.getWindow().getScaledHeight() - height - 5;
         }

         if (x < 5) {
            x = 5;
         }

         if (y < 5) {
            y = 5;
         }

         context.getMatrices().pushMatrix();
         context.getMatrices().translate((float) (0.0), (float) (0.0));
         context.fill( x, y, x + width, y + height, -267255270);
         int borderColor = -11921302;
         context.fill( x, y, x + width, y + 1, borderColor);
         context.fill( x, y + height - 1, x + width, y + height, borderColor);
         context.fill( x, y, x + 1, y + height, borderColor);
         context.fill( x + width - 1, y, x + width, y + height, borderColor);
         String title = shulkerStack.getCustomName() != null ? shulkerStack.getName().getString() : shulkerStack.getItem().getName(shulkerStack).getString();
         context.drawTextWithShadow(mc.textRenderer, title, x + 7, y + 5, 16777215);
         int startX = x + 7;
         int startY = y + 17;

         for (int i = 0; i < 27; i++) {
            int col = i % 9;
            int row = i / 9;
            int slotX = startX + col * 18;
            int slotY = startY + row * 18;
            context.fill( slotX, slotY, slotX + 18, slotY + 18, -12829636);
            context.fill( slotX + 1, slotY + 1, slotX + 17, slotY + 17, -7829368);
         }
         for (int i = 0; i < 27; i++) {
            int col = i % 9;
            int row = i / 9;
            int slotX = startX + col * 18;
            int slotY = startY + row * 18;
            ItemStack item = (ItemStack)items.get(i);
            if (!item.isEmpty()) {
               context.drawItem(item, slotX + 1, slotY + 1);
               context.drawStackOverlay(mc.textRenderer, item, slotX + 1, slotY + 1);
            }
         }
         context.getMatrices().popMatrix();
      }
   }
}
