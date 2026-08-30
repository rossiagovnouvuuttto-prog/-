package com.reallyvisuals.mixin.accessor;

import java.util.Map;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemCooldownManager.class)
public interface ItemCooldownManagerAccessor {
   @Accessor("entries")
   Map<Item, Object> getEntries();

   @Accessor("tick")
   int getTick();
}
