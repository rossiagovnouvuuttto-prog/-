package com.reallyvisuals.module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

public class AutoDuel extends Module {
   public static AutoDuel INSTANCE;
   public Module.ModeSetting kitSetting = new Module.ModeSetting(
      "Набор", new String[]{"Щит", "Шипы 3", "Лук", "Тотемы", "Нодебафф", "Шары", "Классик", "Читерский рай"}, "Щит"
   );
   public Module.BooleanSetting betSetting = new Module.BooleanSetting("Ставить деньги", false);
   public Module.TextSetting betAmountSetting = new Module.TextSetting("Сумма ставки", "$2,000");
   private final Set<String> sentPlayers = new HashSet<>();
   private long lastSendTime = 0L;
   private long lastSlotClickTime = 0L;

   public AutoDuel() {
      super("Auto Duel", "Автоматическая отправка запросов на дуэль", Category.UTILITIES, false, true);
      INSTANCE = this;
      this.addSetting(this.kitSetting);
      this.addSetting(this.betSetting);
      this.addSetting(this.betAmountSetting.visible(() -> this.betSetting.value));
   }

   @Override
   public void onEnable() {
      this.sentPlayers.clear();
      this.lastSendTime = 0L;
      this.lastSlotClickTime = 0L;
   }

   @Override
   public void onDisable() {
      this.sentPlayers.clear();
   }

   public static void onChatMessage(String message) {
      if (INSTANCE != null && INSTANCE.isEnabled()) {
         if (message != null) {
            String lower = message.toLowerCase();
            if (lower.contains("принял ваш запрос") || lower.contains("принял запрос")) {
               INSTANCE.setEnabled(false);
            }
         }
      }
   }

   @Override
   public void onTick() {
      if (this.isEnabled()) {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client.player != null && client.world != null) {
            long now = System.currentTimeMillis();
            if (client.currentScreen instanceof HandledScreen) {
               HandledScreen handledScreen = (HandledScreen)client.currentScreen;
               String title = handledScreen.getTitle().getString();
               if (now - this.lastSlotClickTime >= 350L) {
                  if (title.contains("Выбор набора")) {
                     ScreenHandler handler = handledScreen.getScreenHandler();
                     String targetKit = this.kitSetting.value;

                     for (int slotId = 0; slotId < handler.slots.size(); slotId++) {
                        Slot slot = (Slot)handler.slots.get(slotId);
                        ItemStack stack = slot.getStack();
                        if (!stack.isEmpty() && this.matchesKit(stack.getName().getString(), targetKit)) {
                           client.interactionManager.clickSlot(handler.syncId, slotId, 0, SlotActionType.PICKUP, client.player);
                           this.lastSlotClickTime = now;
                           return;
                        }
                     }
                  } else if (title.contains("Настройка поединка")) {
                     ScreenHandler handler = handledScreen.getScreenHandler();

                     for (int slotId = 0; slotId < handler.slots.size(); slotId++) {
                        Slot slot = (Slot)handler.slots.get(slotId);
                        ItemStack stack = slot.getStack();
                        String itemName;
                        if (!stack.isEmpty()
                           && (
                              (itemName = stack.getName().getString().toLowerCase()).contains("отправить")
                                 || itemName.contains("запрос")
                                 || stack.getItem().getTranslationKey().contains("lime_stained_glass_pane")
                           )) {
                           client.interactionManager.clickSlot(handler.syncId, slotId, 0, SlotActionType.PICKUP, client.player);
                           this.lastSlotClickTime = now;
                           return;
                        }
                     }

                     if (handler.slots.size() > 10) {
                        client.interactionManager.clickSlot(handler.syncId, 10, 0, SlotActionType.PICKUP, client.player);
                        this.lastSlotClickTime = now;
                        return;
                     }
                  }
               }
            } else if (now - this.lastSendTime >= 1400L) {
               if (client.getNetworkHandler() != null) {
                  Collection<PlayerListEntry> players = client.getNetworkHandler().getPlayerList();
                  if (!players.isEmpty()) {
                     String selfName = client.player.getGameProfile().name();
                     ArrayList<String> available = new ArrayList<>();

                     for (PlayerListEntry p : players) {
                        String name = p.getProfile().name();
                        if (name != null && !name.equalsIgnoreCase(selfName) && !this.sentPlayers.contains(name)) {
                           available.add(name);
                        }
                     }

                     if (available.isEmpty()) {
                        this.sentPlayers.clear();

                        for (PlayerListEntry p : players) {
                           String name = p.getProfile().name();
                           if (name != null && !name.equalsIgnoreCase(selfName)) {
                              available.add(name);
                           }
                        }
                     }

                     if (!available.isEmpty()) {
                        String targetNick = available.get(new Random().nextInt(available.size()));
                        this.sentPlayers.add(targetNick);
                        String cmd = "/duel " + targetNick;
                        String cleanBet;
                        if (this.betSetting.value && !(cleanBet = this.betAmountSetting.value.replaceAll("[^0-9]", "")).isEmpty()) {
                           cmd = cmd + " " + cleanBet;
                        }

                        client.player.networkHandler.sendChatMessage(cmd);
                        this.lastSendTime = now;
                     }
                  }
               }
            }
         }
      }
   }

   private boolean matchesKit(String itemName, String targetKit) {
      if (itemName != null && targetKit != null) {
         String lowerItem = itemName.toLowerCase();
         switch (targetKit) {
            case "Щит":
               return lowerItem.contains("щит");
            case "Шипы 3":
               return lowerItem.contains("шипы");
            case "Лук":
               return lowerItem.contains("рук") || lowerItem.contains("лук");
            case "Тотемы":
               return lowerItem.contains("тотем");
            case "Нодебафф":
               return lowerItem.contains("нодебафф") || lowerItem.contains("зелья");
            case "Шары":
               return lowerItem.contains("шар");
            case "Классик":
               return lowerItem.contains("классик");
            case "Читерский рай":
               return lowerItem.contains("читер") || lowerItem.contains("рай");
            default:
               return lowerItem.contains(targetKit.toLowerCase());
         }
      } else {
         return false;
      }
   }
}
