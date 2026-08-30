package com.reallyvisuals.module;

import com.reallyvisuals.gui.font.CustomFont;
import com.reallyvisuals.gui.font.FontManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class ClickPearl extends Module {
   public Module.KeySetting keySetting = new Module.KeySetting("Кнопка", 71);
   private boolean throwing = false;
   private int originalSlot = -1;
   private long throwStartTime = 0L;

   public ClickPearl() {
      super("Click Pearl", "Быстрое использование эндер-перла по бинду", Category.UTILITIES, false, true);
      this.addSetting(this.keySetting);
   }

   @Override
   public void onKeyPressed(int key) {
      if (this.isEnabled()) {
         if (this.keySetting.key != 0 && this.keySetting.key == key) {
            this.useEnderPearl();
         }
      }
   }

   private void useEnderPearl() {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player != null && client.interactionManager != null) {
         int pearlSlot = -1;

         for (int i = 0; i < 9; i++) {
            if (client.player.getInventory().getStack(i).getItem() == Items.ENDER_PEARL) {
               pearlSlot = i;
               break;
            }
         }

         if (pearlSlot != -1 && !this.throwing) {
            this.originalSlot = client.player.getInventory().getSelectedSlot();
            client.player.getInventory().setSelectedSlot(pearlSlot);
            client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
            this.throwing = true;
            this.throwStartTime = System.currentTimeMillis();
         }
      }
   }

   @Override
   public void onTick() {
      if (this.throwing && System.currentTimeMillis() - this.throwStartTime >= 350L) {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client.player != null && this.originalSlot != -1) {
            client.player.getInventory().setSelectedSlot(this.originalSlot);
         }

         this.throwing = false;
         this.originalSlot = -1;
      }
   }

   public void renderHotbarKeybind(DrawContext context, int scaledWidth, int scaledHeight) {
      if (this.keySetting.key > 0) {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client.player != null) {
            int pearlSlot = -1;

            for (int i = 0; i < 9; i++) {
               if (client.player.getInventory().getStack(i).getItem() == Items.ENDER_PEARL) {
                  pearlSlot = i;
                  break;
               }
            }

            if (pearlSlot != -1) {
               float slotX = scaledWidth / 2.0F - 91.0F + pearlSlot * 20;
               float slotY = scaledHeight - 22;
               CustomFont subFont = FontManager.getSubFont();
               if (subFont != null) {
                  subFont.drawString(context, this.keySetting.keyName, slotX + 2.5F, slotY + 2.0F, -1);
               }
            }
         }
      }
   }
}
