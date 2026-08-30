package com.reallyvisuals.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

public class ItemScroller extends Module {
   public Module.NumberSetting delaySetting = new Module.NumberSetting("Задержка (мс)", 50.0, 1.0, 500.0, 1.0);
   private long lastMoveTime = 0L;
   private Slot lastMovedSlot = null;

   public ItemScroller() {
      super("Item Scroller", "Быстрое перемещение предметов при удерживании Shift + ЛКМ", Category.UTILITIES, true, true);
      this.addSetting(this.delaySetting);
   }

   public void handleSlotHover(HandledScreen<?> screen, Slot slot, boolean leftDown, boolean shiftDown) {
      if (this.isEnabled() && leftDown && shiftDown) {
         if (slot != null && slot.hasStack() && !slot.getStack().isEmpty() && slot.getStack().getItem() != Items.AIR) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null && client.interactionManager != null && screen.getScreenHandler() != null) {
               long now = System.currentTimeMillis();
               long delayMs = (long)this.delaySetting.value;
               if (slot != this.lastMovedSlot && now - this.lastMoveTime >= delayMs) {
                  client.interactionManager.clickSlot(screen.getScreenHandler().syncId, slot.id, 0, SlotActionType.QUICK_MOVE, client.player);
                  this.lastMovedSlot = slot;
                  this.lastMoveTime = now;
               }
            } else {
               this.lastMovedSlot = null;
            }
         }
      } else {
         this.lastMovedSlot = null;
      }
   }

   public void resetLastSlot() {
      this.lastMovedSlot = null;
   }
}
