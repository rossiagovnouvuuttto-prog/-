package com.reallyvisuals.module;

import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public class PvpSafeModule extends Module {
   private static final Pattern DANGEROUS_CMD_PATTERN = Pattern.compile(
      "^/(leave|limbo|hub|lobby|spawn|home|rtp|warp|back|logout|server|suicide|kill|auction|trade|duel|pay|msg|tell|w)(\\s+.*)?$", 2
   );
   public Module.ModeSetting mode = new Module.ModeSetting("Режим", new String[]{"Только в бою", "Всегда"}, "Только в бою");
   public Module.NumberSetting combatRange = new Module.NumberSetting("Радиус боя", 16.0, 4.0, 64.0, 1.0);
   public Module.NumberSetting combatTime = new Module.NumberSetting("Время боя (сек)", 10.0, 1.0, 60.0, 1.0);
   private final Map<String, Long> confirmationMap = new HashMap<>();
   private long lastCombatTime = 0L;

   public PvpSafeModule() {
      super("PVP Safe", "Подтверждение опасных команд в бою", Category.UTILITIES, false, true);
      this.addSetting(this.mode);
      this.addSetting(this.combatRange);
      this.addSetting(this.combatTime);
   }

   @Override
   public void onTick() {
      if (this.isEnabled()) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.player != null && mc.world != null) {
            double range = this.combatRange.value;

            for (Entity entity : mc.world.getEntities()) {
               if (entity instanceof PlayerEntity && entity != mc.player && entity.isAlive() && entity.distanceTo(mc.player) <= range) {
                  this.lastCombatTime = System.currentTimeMillis();
                  break;
               }
            }
         }
      }
   }

   public boolean isInCombat() {
      return System.currentTimeMillis() - this.lastCombatTime <= (long)(this.combatTime.value * 1000.0);
   }

   public boolean processCommand(String message) {
      if (!this.isEnabled() || message == null || !message.startsWith("/")) {
         return false;
      } else if ("Только в бою".equals(this.mode.value) && !this.isInCombat()) {
         return false;
      } else {
         Matcher matcher = DANGEROUS_CMD_PATTERN.matcher(message.trim());
         if (!matcher.matches()) {
            return false;
         } else {
            String cmd = matcher.group(1).toLowerCase();
            long now = System.currentTimeMillis();
            Long lastTime = this.confirmationMap.get(cmd);
            if (lastTime != null && now - lastTime <= 5000L) {
               this.confirmationMap.remove(cmd);
               return false;
            } else {
               this.confirmationMap.put(cmd, now);
               MinecraftClient mc = MinecraftClient.getInstance();
               if (mc.inGameHud != null) {
                  mc.inGameHud
                     .getChatHud()
                     .addMessage(Text.literal("§6[ABOBUS123] §8» §fРядом игрок! Для подтверждения отправьте команду ещё раз."));
               }

               return true;
            }
         }
      }
   }
}
