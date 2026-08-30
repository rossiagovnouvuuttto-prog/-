package com.reallyvisuals.module;

import com.reallyvisuals.gui.font.CustomFont;
import com.reallyvisuals.gui.font.FontManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

public class ArmorHud extends Module {
   public ArmorHud() {
      super("Armor HUD", "Отображение прочности брони около хотбара", Category.HUD, false, false);
   }

   @Override
   public void onRenderHUD(DrawContext context) {
      if (this.isEnabled()) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.player != null && mc.world != null) {
            int scaledWidth = mc.getWindow().getScaledWidth();
            int scaledHeight = mc.getWindow().getScaledHeight();
            int hotbarLeft = scaledWidth / 2 - 91;
            int hotbarRight = scaledWidth / 2 + 91;
            int hotbarTop = scaledHeight - 22;
            CustomFont mainFont = FontManager.getMainFont();
            ItemStack boots = mc.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.FEET);
            ItemStack leggings = mc.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.LEGS);
            ItemStack chestplate = mc.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST);
            ItemStack helmet = mc.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD);
            if (!helmet.isEmpty()) {
               this.drawArmorItemLeft(context, mc, mainFont, helmet, hotbarLeft - 50, hotbarTop - 14);
            }

            if (!chestplate.isEmpty()) {
               this.drawArmorItemLeft(context, mc, mainFont, chestplate, hotbarLeft - 50, hotbarTop + 4);
            }

            if (!leggings.isEmpty()) {
               this.drawArmorItemRight(context, mc, mainFont, leggings, hotbarRight + 8, hotbarTop - 14);
            }

            if (!boots.isEmpty()) {
               this.drawArmorItemRight(context, mc, mainFont, boots, hotbarRight + 8, hotbarTop + 4);
            }
         }
      }
   }

   private void drawArmorItemLeft(DrawContext context, MinecraftClient mc, CustomFont font, ItemStack stack, int x, int y) {
      int maxDur = stack.getMaxDamage();
      int dur = maxDur - stack.getDamage();
      int pct = maxDur > 0 ? (int)((float)dur / maxDur * 100.0F) : 100;
      String text = pct + "%";
      int textW = font.getStringWidth(text);
      font.drawString(context, text, x + 24 - textW, y + 4, -1);
      context.drawItem(stack, x + 28, y);
   }

   private void drawArmorItemRight(DrawContext context, MinecraftClient mc, CustomFont font, ItemStack stack, int x, int y) {
      int maxDur = stack.getMaxDamage();
      int dur = maxDur - stack.getDamage();
      int pct = maxDur > 0 ? (int)((float)dur / maxDur * 100.0F) : 100;
      String text = pct + "%";
      context.drawItem(stack, x, y);
      font.drawString(context, text, x + 20, y + 4, -1);
   }
}
