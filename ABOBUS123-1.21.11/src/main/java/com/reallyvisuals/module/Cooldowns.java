package com.reallyvisuals.module;

import com.reallyvisuals.gui.HUDManager;
import com.reallyvisuals.gui.ReallyVisualsScreen;
import com.reallyvisuals.gui.font.CustomFont;
import com.reallyvisuals.gui.font.FontManager;
import com.reallyvisuals.gui.render.RenderUtils;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;

public class Cooldowns extends Module {
   private static final Map<Item, String> ITEM_NAMES = new HashMap<>();
   private static Field tickField = null;
   private static Field entriesField = null;
   private static Field startTickField = null;
   private static Field endTickField = null;

   public Cooldowns() {
      super("Cooldowns", "Отображение кулдаунов предметов", Category.HUD, false, false);
   }

   @Override
   public void onRenderHUD(DrawContext context) {
      if (this.isEnabled()) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.player != null && mc.world != null) {
            List<Cooldowns.ActiveCooldown> list = this.getActiveCooldowns(mc);
            if (list.isEmpty()) {
               if (!(mc.currentScreen instanceof ReallyVisualsScreen)) {
                  return;
               }

               list.add(new Cooldowns.ActiveCooldown(new ItemStack(Items.ENDER_PEARL), "Эндер-шар", "0,0s"));
            }

            CustomFont mainFont = FontManager.getMainFont();
            CustomFont subFont = FontManager.getSubFont();
            int x = (int)HUDManager.cooldowns.x;
            int y = (int)HUDManager.cooldowns.y;
            int cardWidth = 135;
            int rowHeight = 18;
            int headerHeight = 22;
            int cardHeight = headerHeight + list.size() * rowHeight + 4;
            HUDManager.cooldowns.setContentSize(cardWidth, cardHeight);
            HUDManager.cooldowns.beginScale(context);
            RenderUtils.drawRoundedRect(context, x, y, cardWidth, cardHeight, 8.0F, -300871403);
            RenderUtils.drawRoundedRect(context, x + 1, y + 1, cardWidth - 2, cardHeight - 2, 7.5F, -15461351);
            int clockX = x + 8;
            int clockY = y + 6;
            RenderUtils.drawRoundedRect(context, clockX, clockY, 9.0F, 9.0F, 4.5F, ReallyVisualsScreen.clientColor);
            RenderUtils.drawRoundedRect(context, clockX + 1.5F, clockY + 1.5F, 6.0F, 6.0F, 3.0F, -15461351);
            RenderUtils.drawRect(context, clockX + 4, clockY + 2.5F, 1.0F, 2.5F, ReallyVisualsScreen.clientColor);
            RenderUtils.drawRect(context, clockX + 4, clockY + 4.5F, 2.0F, 1.0F, ReallyVisualsScreen.clientColor);
            mainFont.drawString(context, "Cooldowns", x + 23, y + 5, -1);
            int rowY = y + headerHeight;

            for (Cooldowns.ActiveCooldown cd : list) {
               try {
                  context.drawItem(cd.stack, x + 6, rowY + 1);
               } catch (Exception var20) {
               }

               mainFont.drawString(context, cd.name, x + 26, rowY + 4, -1);
               int pillWidth = subFont.getStringWidth(cd.timeText) + 8;
               int pillX = x + cardWidth - 6 - pillWidth;
               int pillY = rowY + 2;
               RenderUtils.drawRoundedRect(context, pillX, pillY, pillWidth, 13.0F, 4.0F, -14737626);
               subFont.drawString(context, cd.timeText, pillX + 4, pillY + 3, -1);
               rowY += rowHeight;
            }

            HUDManager.cooldowns.endScale(context);
         }
      }
   }

   private List<Cooldowns.ActiveCooldown> getActiveCooldowns(MinecraftClient mc) {
      ArrayList<Cooldowns.ActiveCooldown> list = new ArrayList<>();
      if (mc.player == null) {
         return list;
      }

      ItemCooldownManager manager = mc.player.getItemCooldownManager();

      try {
         int currentTick = tickField != null ? tickField.getInt(manager) : 0;
         Map entries = entriesField != null ? (Map)entriesField.get(manager) : null;
         if (entries != null && !entries.isEmpty()) {
            for (Object entryObj : entries.entrySet()) {
               Entry entry = (Entry)entryObj;
               Object key = entry.getKey();
               Object val = entry.getValue();
               Item item = null;
               if (key instanceof Item) {
                  item = (Item)key;
               } else if (key instanceof Identifier) {
                  item = (Item)Registries.ITEM.get((Identifier)key);
               }

               int remainingTicks;
               if (item != null
                  && item != Items.AIR
                  && val != null
                  && (remainingTicks = (endTickField != null ? endTickField.getInt(val) : 0) - currentTick) > 0) {
                  float secs = remainingTicks / 20.0F;
                  String timeStr;
                  if (secs < 10.0F) {
                     timeStr = String.format("%.1fs", secs).replace(".", ",");
                  } else {
                     int totalSecs = (int)secs;
                     timeStr = String.format("%d:%02d", totalSecs / 60, totalSecs % 60);
                  }

                  String name = ITEM_NAMES.getOrDefault(item, new ItemStack(item).getName().getString());
                  list.add(new Cooldowns.ActiveCooldown(new ItemStack(item), name, timeStr));
               }
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

      return list;
   }

   static {
      ITEM_NAMES.put(Items.GOLDEN_APPLE, "Золотое яблоко");
      ITEM_NAMES.put(Items.ENCHANTED_GOLDEN_APPLE, "Золотое яблоко");
      ITEM_NAMES.put(Items.FIREWORK_ROCKET, "Фейерверк");
      ITEM_NAMES.put(Items.ENDER_PEARL, "Эндер-шар");
      ITEM_NAMES.put(Items.CHORUS_FRUIT, "Хорус");
      ITEM_NAMES.put(Items.POPPED_CHORUS_FRUIT, "Хорус");
      ITEM_NAMES.put(Items.SHIELD, "Щит");
      ITEM_NAMES.put(Items.TOTEM_OF_UNDYING, "Тотем");
      ITEM_NAMES.put(Items.ENDER_EYE, "Глаз эндера");

      try {
         for (Field f : ItemCooldownManager.class.getDeclaredFields()) {
            if (f.getType() == int.class && tickField == null) {
               f.setAccessible(true);
               tickField = f;
            } else if (Map.class.isAssignableFrom(f.getType()) && entriesField == null) {
               f.setAccessible(true);
               entriesField = f;
            }
         }

         for (Class<?> cls : ItemCooldownManager.class.getDeclaredClasses()) {
            Field f1 = null;
            Field f2 = null;

            for (Field f : cls.getDeclaredFields()) {
               if (f.getType() == int.class) {
                  f.setAccessible(true);
                  if (f1 == null) {
                     f1 = f;
                  } else if (f2 == null) {
                     f2 = f;
                  }
               }
            }

            if (f1 != null && f2 != null) {
               startTickField = f1;
               endTickField = f2;
               break;
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   public static class ActiveCooldown {
      public final ItemStack stack;
      public final String name;
      public final String timeText;

      public ActiveCooldown(ItemStack stack, String name, String timeText) {
         this.stack = stack;
         this.name = name;
         this.timeText = timeText;
      }
   }
}
