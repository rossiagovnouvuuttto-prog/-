package com.reallyvisuals.gui.font;

import java.io.InputStream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

public class FontManager {
   private static CustomFont mainFont;
   private static CustomFont titleFont;
   private static CustomFont subFont;
   private static CustomFont iconFont;
   private static CustomFont largeIconFont;

   public static void init() {
      if (mainFont == null) {
         MinecraftClient client = MinecraftClient.getInstance();

         try {
            InputStream sfStream = client.getResourceManager().getResource(Identifier.of("really", "font/sfprodisplay_medium.ttf")).orElseThrow().getInputStream();
            mainFont = new CustomFont(sfStream, 8.0F);
         } catch (Exception e) {
            try {
               InputStream sfAltStream = client.getResourceManager().getResource(Identifier.of("really", "font/onest_semibold.ttf")).orElseThrow().getInputStream();
               mainFont = new CustomFont(sfAltStream, 8.0F);
            } catch (Exception ignored) {
               mainFont = new CustomFont(8.0F);
            }
         }

         try {
            InputStream sfSubStream = client.getResourceManager().getResource(Identifier.of("really", "font/onest_medium.ttf")).orElseThrow().getInputStream();
            subFont = new CustomFont(sfSubStream, 7.8F);
         } catch (Exception e) {
            subFont = new CustomFont(8.0F);
         }

         try {
            InputStream onestStream = client.getResourceManager().getResource(Identifier.of("really", "font/onest_semibold.ttf")).orElseThrow().getInputStream();
            titleFont = new CustomFont(onestStream, 11.5F);
         } catch (Exception e) {
            titleFont = new CustomFont(11.5F);
         }

         try {
            InputStream iconStream = client.getResourceManager().getResource(Identifier.of("really", "font/really.ttf")).orElseThrow().getInputStream();
            iconFont = new CustomFont(iconStream, 13.0F);
         } catch (Exception e) {
            iconFont = new CustomFont(13.0F);
         }

         try {
            InputStream largeIconStream = client.getResourceManager().getResource(Identifier.of("really", "font/really.ttf")).orElseThrow().getInputStream();
            largeIconFont = new CustomFont(largeIconStream, 15.0F);
         } catch (Exception e) {
            largeIconFont = new CustomFont(15.0F);
         }

         System.out.println("[ABOBUS123] Loaded TTF Fonts and Icons successfully!");
      }
   }

   public static CustomFont getMainFont() {
      if (mainFont == null) {
         init();
      }

      if (mainFont == null) {
         mainFont = new CustomFont(9.5F);
      }

      return mainFont;
   }

   public static CustomFont getTitleFont() {
      if (titleFont == null) {
         init();
      }

      if (titleFont == null) {
         titleFont = new CustomFont(11.5F);
      }

      return titleFont;
   }

   public static CustomFont getSubFont() {
      if (subFont == null) {
         init();
      }

      if (subFont == null) {
         subFont = new CustomFont(8.0F);
      }

      return subFont;
   }

   public static CustomFont getIconFont() {
      if (iconFont == null) {
         init();
      }

      if (iconFont == null) {
         iconFont = new CustomFont(13.0F);
      }

      return iconFont;
   }

   public static CustomFont getLargeIconFont() {
      if (largeIconFont == null) {
         init();
      }

      if (largeIconFont == null) {
         largeIconFont = new CustomFont(15.0F);
      }

      return largeIconFont;
   }
}
