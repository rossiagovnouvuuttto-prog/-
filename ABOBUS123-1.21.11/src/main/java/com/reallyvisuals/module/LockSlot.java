package com.reallyvisuals.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class LockSlot extends Module {
   public Module.BooleanSetting blockAllHotbar = new Module.BooleanSetting("Блокировать весь хотбар", false);
   public Module.MultiSelectSetting lockedSlots = new Module.MultiSelectSetting(
      "Заблокированные слоты", new String[]{"Слот 1", "Слот 2", "Слот 3", "Слот 4", "Слот 5", "Слот 6", "Слот 7", "Слот 8", "Слот 9"}, new String[0]
   );
   public Module.BooleanSetting blockSpheresTotems = new Module.BooleanSetting("Блокировать шары/тотемы", false);

   public LockSlot() {
      super("Lock Slot", "Блокировка выбрасывания предметов из выбранных слотов", Category.UTILITIES, false, true);
      this.addSetting(this.blockAllHotbar);
      this.addSetting(this.lockedSlots);
      this.addSetting(this.blockSpheresTotems);
   }

   public boolean isSlotLocked(int slotIndex) {
      if (!this.isEnabled()) {
         return false;
      }

      if (this.blockAllHotbar.value && slotIndex >= 0 && slotIndex < 9) {
         return true;
      }

      String slotName = "Слот " + (slotIndex + 1);
      if (this.lockedSlots.isSelected(slotName)) {
         return true;
      }

      if (this.blockSpheresTotems.value) {
         MinecraftClient client = MinecraftClient.getInstance();
         ItemStack stack;
         if (client.player != null
            && slotIndex >= 0
            && slotIndex < 9
            && !(stack = client.player.getInventory().getStack(slotIndex)).isEmpty()
            && (stack.getItem() == Items.TOTEM_OF_UNDYING || stack.getName().getString().toLowerCase().contains("шар"))) {
            return true;
         }
      }

      return false;
   }
}
