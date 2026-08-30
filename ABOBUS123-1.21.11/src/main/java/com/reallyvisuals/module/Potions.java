package com.reallyvisuals.module;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reallyvisuals.gui.HUDManager;
import com.reallyvisuals.gui.ReallyVisualsScreen;
import com.reallyvisuals.gui.font.CustomFont;
import com.reallyvisuals.gui.font.FontManager;
import com.reallyvisuals.gui.render.RenderUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;

public class Potions extends Module {
   public Potions() {
      super("Potions", "Отображение активных эффектов зелий", Category.HUD, false, false);
   }

   @Override
   public void onRenderHUD(DrawContext context) {
      if (this.isEnabled()) {
         MinecraftClient mc = MinecraftClient.getInstance();
         List<Potions.ActivePotionSnapshot> list = this.getActivePotions(mc);
         if (list.isEmpty()) {
            if (!(mc.currentScreen instanceof ReallyVisualsScreen)) {
               return;
            }

            list.add(new Potions.ActivePotionSnapshot(null, "Strength", "3:12"));
            list.add(new Potions.ActivePotionSnapshot(null, "Fire Resi", "1:20"));
            list.add(new Potions.ActivePotionSnapshot(null, "Speed II", "0:45"));
         }

         CustomFont mainFont = FontManager.getMainFont();
         CustomFont subFont = FontManager.getSubFont();
         int cardWidth = 120;
         int rowHeight = 16;
         int headerHeight = 22;
         int cardHeight = headerHeight + list.size() * rowHeight + 4;
         int x = (int)HUDManager.potions.x;
         int y = (int)HUDManager.potions.y;
         HUDManager.potions.setContentSize(cardWidth, cardHeight);
         HUDManager.potions.beginScale(context);
         RenderUtils.drawRoundedRect(context, x, y, cardWidth, cardHeight, 7.0F, -300871403);
         RenderUtils.drawRoundedRect(context, x + 1, y + 1, cardWidth - 2, cardHeight - 2, 6.5F, -15329764);
         int iconX = x + 8;
         int iconY = y + 6;
         this.drawPotionFlaskIcon(context, iconX, iconY);
         mainFont.drawString(context, "Potions", x + 22, y + 5, -1);
         int rowY = y + headerHeight;

         for (Potions.ActivePotionSnapshot ps : list) {
            this.drawStatusEffectSprite(context, mc, ps.effect, x + 8, rowY + 2, 10);
            subFont.drawString(context, ps.name, x + 22, rowY + 2, -1);
            int pillWidth = subFont.getStringWidth(ps.durationText) + 8;
            int pillX = x + cardWidth - 6 - pillWidth;
            int pillY = rowY + 1;
            RenderUtils.drawRoundedRect(context, pillX, pillY, pillWidth, 12.0F, 4.0F, -14803418);
            subFont.drawString(context, ps.durationText, pillX + 4, pillY + 2, -1);
            rowY += rowHeight;
         }

         HUDManager.potions.endScale(context);
      }
   }

   private void drawPotionFlaskIcon(DrawContext context, int x, int y) {
      CustomFont iconFont = FontManager.getIconFont();
      context.getMatrices().pushMatrix();
      float scale = 0.55F;
      context.getMatrices().scale((float) (scale), (float) (scale));
      float invS = 1.0F / scale;
      iconFont.drawString(context, "\ue917", (x + 2.0F) * invS, (y + 1.0F) * invS, -40942);
      context.getMatrices().popMatrix();
   }

   private void drawStatusEffectSprite(DrawContext context, MinecraftClient mc, net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect, int x, int y, int size) {
      try {
         // 1.21.11: no client-side status effect sprite manager; icons live in the GUI
         // atlas under mob_effect/<id>, so the sprite is addressed by Identifier.
         if (effect != null) {
            net.minecraft.util.Identifier id = effect.getKey().map(k -> k.getValue()).orElse(null);
            if (id != null) {
               context.drawGuiTexture(
                  net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED,
                  net.minecraft.util.Identifier.of(id.getNamespace(), "mob_effect/" + id.getPath()),
                  x, y, size, size);
               return;
            }
         }
      } catch (Throwable var8) {
      }

      RenderUtils.drawRoundedRect(context, x, y + 1, size - 2, size - 2, 2.0F, -40942);
   }

   private List<Potions.ActivePotionSnapshot> getActivePotions(MinecraftClient mc) {
      ArrayList<Potions.ActivePotionSnapshot> list = new ArrayList<>();
      if (mc.player == null) {
         return list;
      }

      for (StatusEffectInstance instance : mc.player.getStatusEffects()) {
         net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect = instance.getEffectType();
         String name = effect.value().getName().getString();
         int amp = instance.getAmplifier();
         if (amp > 0) {
            name = name + " " + this.getRoman(amp + 1);
         }

         int dur;
         boolean isInf = (dur = instance.getDuration()) > 32000 || instance.isInfinite();
         String durStr = isInf ? "Inf" : String.format("%d:%02d", dur / 20 / 60, dur / 20 % 60);
         list.add(new Potions.ActivePotionSnapshot(effect, name, durStr));
      }

      return list;
   }

   private String getRoman(int n) {
      switch (n) {
         case 1:
            return "I";
         case 2:
            return "II";
         case 3:
            return "III";
         case 4:
            return "IV";
         case 5:
            return "V";
         default:
            return String.valueOf(n);
      }
   }

   public static class ActivePotionSnapshot {
      public final net.minecraft.registry.entry.RegistryEntry<StatusEffect> effect;
      public final String name;
      public final String durationText;

      public ActivePotionSnapshot(net.minecraft.registry.entry.RegistryEntry<StatusEffect> effect, String name, String durationText) {
         this.effect = effect;
         this.name = name;
         this.durationText = durationText;
      }
   }
}
