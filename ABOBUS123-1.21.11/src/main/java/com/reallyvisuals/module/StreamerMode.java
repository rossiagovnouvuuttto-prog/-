package com.reallyvisuals.module;

import net.minecraft.client.MinecraftClient;

public class StreamerMode extends Module {
   public Module.BooleanSetting hideName = new Module.BooleanSetting("Скрыть ник", false);
   public Module.BooleanSetting hideGrief = new Module.BooleanSetting("Скрыть номер грифа", false);
   public Module.BooleanSetting hideCoords = new Module.BooleanSetting("Скрыть координаты", false);
   public Module.BooleanSetting blurPassword = new Module.BooleanSetting("Блюр пароля", true);

   public StreamerMode() {
      super("Streamer Mode", "Скрытие имени игрока и личной информации", Category.UTILITIES, false, true);
      this.addSetting(this.hideName);
      this.addSetting(this.hideGrief);
      this.addSetting(this.hideCoords);
      this.addSetting(this.blurPassword);
   }

   public static String processText(String text) {
      if (text != null && !text.isEmpty()) {
         StreamerMode mode = (StreamerMode)ModuleManager.getInstance().getModule("Streamer Mode");
         if (mode != null && mode.isEnabled()) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mode.hideName.value) {
               String u1;
               if (mc.getSession() != null && mc.getSession().getUsername() != null && !(u1 = mc.getSession().getUsername()).isEmpty()) {
                  text = text.replace(u1, "ABOBUS123");
               }

               if (mc.player != null) {
                  String u2 = mc.player.getNameForScoreboard();
                  if (u2 != null && !u2.isEmpty()) {
                     text = text.replace(u2, "ABOBUS123");
                  }

                  String u3;
                  if (mc.player.getGameProfile() != null
                     && mc.player.getGameProfile().name() != null
                     && !(u3 = mc.player.getGameProfile().name()).isEmpty()) {
                     text = text.replace(u3, "ABOBUS123");
                  }
               }

               text = text.replaceAll("(?i)(Ник:\\s*§?[0-9a-fk-or]?)[A-Za-z0-9_]+", "$1ABOBUS123");
               text = text.replaceAll("(?i)(Nick:\\s*§?[0-9a-fk-or]?)[A-Za-z0-9_]+", "$1ABOBUS123");
            }

            if (mode.hideGrief.value) {
               text = text.replaceAll("(?i)гриф\\s*#?\\s*\\d+", "ABOBUS123");
               text = text.replaceAll("(?i)гриф[-#\\s]*\\d+", "ABOBUS123");
               text = text.replaceAll("(?i)анархия[-#\\s]*\\d+", "ABOBUS123");
            }

            return text;
         } else {
            return text;
         }
      } else {
         return text;
      }
   }
}
