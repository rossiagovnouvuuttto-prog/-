package com.reallyvisuals.module;

import net.minecraft.text.Text;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;

public class FriendSystem extends Module {
   public Module.BooleanSetting visualsOnFriends = new Module.BooleanSetting("Визуалы на друзьях", true);
   public Module.MultiSelectSetting visuals = new Module.MultiSelectSetting(
      "Визуалы", new String[]{"China Hat", "Trails", "Jump Circles"}, new String[]{"China Hat", "Trails", "Jump Circles"}
   );
   public Module.BooleanSetting tabHighlight = new Module.BooleanSetting("Подсветка в табе и нике", true);
   public Module.BooleanSetting noHitFriends = new Module.BooleanSetting("Не бить друзей", true);
   public Module.BooleanSetting joinNotify = new Module.BooleanSetting("Уведомления о входе", true);
   private int ticks;
   private boolean initialized;

   public FriendSystem() {
      super("Настройки друзей", "Поведение клиента по отношению к друзьям", Category.FRIENDS, false, true);
      this.addSetting(this.tabHighlight);
      this.addSetting(this.noHitFriends);
      this.addSetting(this.joinNotify);
   }

   @Override
   public void onTick() {
      if (++this.ticks < 20) return;
      this.ticks = 0;
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.getNetworkHandler() == null || mc.world == null) {
         this.initialized = false;
         for (FriendManager.Friend f : FriendManager.getFriends()) f.online = false;
         return;
      }
      Set<String> online = new HashSet<>();
      for (PlayerListEntry e : mc.getNetworkHandler().getPlayerList()) {
         if (e != null && e.getProfile() != null && e.getProfile().name() != null) online.add(e.getProfile().name().toLowerCase(Locale.ROOT));
      }
      for (FriendManager.Friend f : FriendManager.getFriends()) {
         boolean nowOnline = online.contains(f.name.toLowerCase(Locale.ROOT));
         if (this.initialized && this.isEnabled() && this.joinNotify.value && nowOnline && !f.online && mc.inGameHud != null) {
            mc.inGameHud.getChatHud().addMessage(Text.literal("§6[ABOBUS123] §aДруг вошёл: §f" + f.name));
         }
         f.online = nowOnline;
      }
      this.initialized = true;
   }
}
