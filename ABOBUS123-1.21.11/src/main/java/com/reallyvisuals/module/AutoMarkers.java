package com.reallyvisuals.module;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

public class AutoMarkers extends Module {
   public Module.BooleanSetting airdrops = new Module.BooleanSetting("Аирдропы", true);
   public Module.BooleanSetting mythics = new Module.BooleanSetting("Мифические сундуки", true);
   public Module.BooleanSetting bosses = new Module.BooleanSetting("Боссы", true);
   private static final Pattern XYZ_LABELLED = Pattern.compile("(?i)x\\s*[:=]?\\s*(-?\\d+)\\D{1,12}y\\s*[:=]?\\s*(-?\\d+)\\D{1,12}z\\s*[:=]?\\s*(-?\\d+)");
   private static final Pattern XYZ_PLAIN = Pattern.compile("(?<!\\d)(-?\\d{1,7})\\s*[,;/ ]\\s*(-?\\d{1,4})\\s*[,;/ ]\\s*(-?\\d{1,7})(?!\\d)");
   private String lastSignature = "";
   private long lastAddedAt;

   public AutoMarkers() {
      super("Авто метки", "Автоматические метки на ивенты сервера ReallyWorld", Category.MARKERS, true, true);
      this.addSetting(this.airdrops);
      this.addSetting(this.mythics);
      this.addSetting(this.bosses);
   }

   public static void onChatMessage(String message) {
      Module m = ModuleManager.getInstance().getModule("Авто метки");
      if (m instanceof AutoMarkers) ((AutoMarkers)m).process(message);
   }

   private void process(String message) {
      if (!this.isEnabled() || message == null || message.isEmpty()) return;
      String lower = message.toLowerCase(Locale.ROOT);
      String name = null;
      int color = 0xFFFFAA00;
      int icon = 6;
      if (this.airdrops.value && (lower.contains("аирдроп") || lower.contains("airdrop") || lower.contains("эйрдроп"))) {
         name = "Аирдроп"; color = 0xFFFFAA00; icon = 6;
      } else if (this.mythics.value && (lower.contains("мифичес") || lower.contains("mythic"))) {
         name = "Мифический сундук"; color = 0xFFAA55FF; icon = 1;
      } else if (this.bosses.value && (lower.contains("босс") || lower.contains("boss"))) {
         name = "Босс"; color = 0xFFFF5555; icon = 4;
      }
      if (name == null) return;

      int[] xyz = extractCoordinates(message);
      if (xyz == null) return;
      if (xyz[1] < -128 || xyz[1] > 512) return;
      String sig = name + ':' + xyz[0] + ':' + xyz[1] + ':' + xyz[2];
      long now = System.currentTimeMillis();
      if (sig.equals(this.lastSignature) && now - this.lastAddedAt < 15000L) return;

      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.world == null) return;
      WaypointManager.addWaypointLimited(name, new Vec3d(xyz[0] + 0.5, xyz[1], xyz[2] + 0.5), color, icon, "Автоматическая метка");
      this.lastSignature = sig;
      this.lastAddedAt = now;
   }

   private static int[] extractCoordinates(String message) {
      Matcher m = XYZ_LABELLED.matcher(message);
      if (!m.find()) {
         m = XYZ_PLAIN.matcher(message);
         if (!m.find()) return null;
      }
      try {
         return new int[]{Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3))};
      } catch (Exception ignored) {
         return null;
      }
   }
}
