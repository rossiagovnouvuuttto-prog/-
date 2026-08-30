package com.reallyvisuals.module;

import com.reallyvisuals.gui.HUDManager;
import com.reallyvisuals.gui.ReallyVisualsScreen;
import com.reallyvisuals.gui.font.CustomFont;
import com.reallyvisuals.gui.font.FontManager;
import com.reallyvisuals.gui.render.RenderUtils;
import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

public class BetterNear extends Module {
   public Module.NumberSetting playerLimit = new Module.NumberSetting("Лимит игроков", 6.0, 1.0, 20.0, 1.0);
   public Module.NumberSetting maxDistance = new Module.NumberSetting("Максимум блоков", 100.0, 5.0, 100.0, 1.0);

   public BetterNear() {
      super("Better Near", "Список прогруженных игроков и направление до них", Category.HUD, false, true);
      this.addSetting(this.playerLimit);
      this.addSetting(this.maxDistance);
   }

   @Override
   public void onRenderHUD(DrawContext context) {
      if (this.isEnabled()) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.player != null && mc.world != null) {
            ArrayList<AbstractClientPlayerEntity> players = new ArrayList<>(mc.world.getPlayers());
            double maxDist = this.maxDistance.value;
            players.removeIf(p -> p == mc.player || !p.isAlive() || p.distanceTo(mc.player) > maxDist);
            if (!players.isEmpty()) {
               players.sort(Comparator.comparingDouble(p -> p.distanceTo(mc.player)));
               int limit = Math.min((int)this.playerLimit.value, players.size());
               if (limit != 0) {
                  CustomFont mainFont = FontManager.getMainFont();
                  CustomFont subFont = FontManager.getSubFont();
                  int x = (int)HUDManager.betterNear.x;
                  int y = (int)HUDManager.betterNear.y;
                  int cardWidth = 175;
                  int rowHeight = 18;
                  int headerHeight = 24;
                  int cardHeight = headerHeight + limit * rowHeight + 4;
                  HUDManager.betterNear.setContentSize(cardWidth, cardHeight);
                  HUDManager.betterNear.beginScale(context);
                  RenderUtils.drawRoundedRect(context, x, y, cardWidth, cardHeight, 8.0F, -300871403);
                  RenderUtils.drawRoundedRect(context, x + 1, y + 1, cardWidth - 2, cardHeight - 2, 7.5F, -15461351);
                  mainFont.drawString(context, "Better Near", x + 10, y + 6, ReallyVisualsScreen.clientColor);
                  subFont.drawString(context, "(" + players.size() + ")", x + 10 + mainFont.getStringWidth("Better Near") + 5, y + 7, -10132112);
                  RenderUtils.drawRect(context, x + 8, y + 21, cardWidth - 16, 1.0F, -14540246);
                  int rowY = y + 25;

                  for (int i = 0; i < limit; i++) {
                     AbstractClientPlayerEntity player = players.get(i);
                     double dist = player.distanceTo(mc.player);
                     String arrow = this.getDirectionArrow(mc.player.getYaw(), mc.player.getX(), mc.player.getZ(), player.getX(), player.getZ());
                     String name = this.resolveName(mc, player);
                     String distStr = String.format("%.0fm", dist);
                     int maxNameW = cardWidth - 20 - 14 - subFont.getStringWidth(distStr) - 6;

                     while (name.length() > 1 && mainFont.getStringWidth(name) > maxNameW) {
                        name = name.substring(0, name.length() - 1);
                     }

                     mainFont.drawString(context, name, x + 10, rowY + 2, -1);
                     subFont.drawString(context, distStr, x + cardWidth - 30 - subFont.getStringWidth(distStr), rowY + 3, -6250326);
                     mainFont.drawString(context, arrow, x + cardWidth - 20, rowY + 2, ReallyVisualsScreen.clientColor);
                     rowY += rowHeight;
                  }

                  HUDManager.betterNear.endScale(context);
               }
            }
         }
      }
   }

   private String resolveName(MinecraftClient mc, AbstractClientPlayerEntity player) {
      boolean isNpc = false;

      try {
         isNpc = mc.getNetworkHandler() == null || mc.getNetworkHandler().getPlayerListEntry(player.getUuid()) == null;
      } catch (Throwable var7) {
      }

      String label = null;

      try {
         if (player.getCustomName() != null) {
            label = player.getCustomName().getString();
         }
      } catch (Throwable var6) {
      }

      if (label == null || label.trim().isEmpty()) {
         try {
            label = player.getDisplayName().getString();
         } catch (Throwable var5) {
            label = player.getName().getString();
         }
      }

      label = CustomFont.stripFormatting(label);
      return isNpc ? "[NPC] " + label : label;
   }

   private String getDirectionArrow(float yaw, double playerX, double playerZ, double targetX, double targetZ) {
      double dz = targetZ - playerZ;
      double dx = targetX - playerX;
      double angle = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
      double diff = MathHelper.wrapDegrees(angle - yaw);
      if (diff >= -22.5 && diff < 22.5) {
         return "↑";
      } else if (diff >= 22.5 && diff < 67.5) {
         return "↗";
      } else if (diff >= 67.5 && diff < 112.5) {
         return "→";
      } else if (diff >= 112.5 && diff < 157.5) {
         return "↘";
      } else if (diff >= 157.5 || diff < -157.5) {
         return "↓";
      } else if (diff >= -157.5 && diff < -112.5) {
         return "↙";
      } else if (diff >= -112.5 && diff < -67.5) {
         return "←";
      } else {
         return diff >= -67.5 && diff < -22.5 ? "↖" : "↑";
      }
   }
}
