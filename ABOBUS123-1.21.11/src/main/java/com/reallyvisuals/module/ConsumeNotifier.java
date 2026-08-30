package com.reallyvisuals.module;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;
import net.minecraft.item.ThrowablePotionItem;
import net.minecraft.text.Text;

public class ConsumeNotifier extends Module {
   private final Map<UUID, String> activeConsuming = new HashMap<>();

   public ConsumeNotifier() {
      super("Consume Notifier", "Отслеживание поедания и питья других игроков", Category.UTILITIES, false, false);
   }

   @Override
   public void onTick() {
      if (this.isEnabled()) {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client.player != null && client.world != null) {
            for (PlayerEntity p : client.world.getPlayers()) {
               if (p != client.player && !(client.player.distanceTo(p) > 40.0F)) {
                  UUID uuid = p.getUuid();
                  if (p.isUsingItem()) {
                     ItemStack stack = p.getActiveItem();
                     if (!stack.isEmpty() && !this.activeConsuming.containsKey(uuid)) {
                        boolean isGapple = stack.getItem() == Items.GOLDEN_APPLE
                           || stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE
                           || stack.getItem() == Items.GOLDEN_CARROT;
                        boolean isPotion = stack.getItem() instanceof PotionItem || stack.getItem() instanceof ThrowablePotionItem;
                        if (isGapple) {
                           this.activeConsuming.put(uuid, stack.getName().getString());
                           client.player
                              .sendMessage(
                                 Text.of(
                                    "§7[§6Consume Notifier§7] §fИгрок §e" + p.getGameProfile().name() + " §fест §6" + stack.getName().getString() + "§f!"
                                 ),
                                 false
                              );
                        } else if (isPotion) {
                           this.activeConsuming.put(uuid, stack.getName().getString());
                           client.player
                              .sendMessage(
                                 Text.of(
                                    "§7[§6Consume Notifier§7] §fИгрок §e" + p.getGameProfile().name() + " §fпьет §b" + stack.getName().getString() + "§f!"
                                 ),
                                 false
                              );
                        }
                     }
                  } else {
                     this.activeConsuming.remove(uuid);
                  }
               }
            }
         }
      }
   }
}
