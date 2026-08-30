package com.reallyvisuals.module;

import net.minecraft.block.Block;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

public class ShulkerPreview extends Module {
   public Module.BooleanSetting onlyShift = new Module.BooleanSetting("Только с Shift", true);
   public Module.BooleanSetting showInWorld = new Module.BooleanSetting("Показывать в мире", true);

   public ShulkerPreview() {
      super("Shulker Preview", "Показывает содержимое шалкер-бокса при наведении", Category.UTILITIES, true, true);
      this.addSetting(this.onlyShift);
      this.addSetting(this.showInWorld);
   }

   public static boolean isShulkerBox(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return false;
      } else if (stack.getItem() instanceof BlockItem) {
         Block block = ((BlockItem)stack.getItem()).getBlock();
         return block instanceof ShulkerBoxBlock;
      } else {
         return false;
      }
   }

   public static boolean hasItems(ItemStack stack) {
      if (!isShulkerBox(stack)) {
         return false;
      }

      net.minecraft.component.type.ContainerComponent container =
         stack.get(net.minecraft.component.DataComponentTypes.CONTAINER);
      return container != null && !container.copyFirstStack().isEmpty();
   }

   public static DefaultedList<ItemStack> getShulkerItems(ItemStack stack) {
      DefaultedList items = DefaultedList.ofSize(27, ItemStack.EMPTY);
      if (!isShulkerBox(stack)) {
         return items;
      }

      net.minecraft.component.type.ContainerComponent container =
         stack.get(net.minecraft.component.DataComponentTypes.CONTAINER);
      if (container != null) {
         // 1.20.5+ stores shulker contents as a component, already slot-ordered.
         container.copyTo(items);
      }

      return items;
   }
}
