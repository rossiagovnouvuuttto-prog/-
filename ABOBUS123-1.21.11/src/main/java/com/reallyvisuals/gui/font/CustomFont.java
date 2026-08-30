package com.reallyvisuals.gui.font;


import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

public class CustomFont {
   private final Font font;
   private final float fontSize;
   private final Map<Character, CustomFont.Glyph> glyphs = new HashMap<>();
   private Identifier textureId;
   private final int texWidth = 1024;
   private final int texHeight = 1024;
   private final float scale = 2.0F;
   private float lineHeight;
   private float ascent;
   private float descent;

   public CustomFont(InputStream fontStream, float size) {
      this.fontSize = size;

      Font loadedFont;
      try {
         loadedFont = Font.createFont(0, fontStream).deriveFont(size * 2.0F);
         fontStream.close();
      } catch (Exception e) {
         loadedFont = new Font("SansSerif", 0, (int)(size * 2.0F));
      }

      this.font = loadedFont;
      this.generateAtlas();
   }

   public CustomFont(float size) {
      this.fontSize = size;
      this.font = new Font("SansSerif", 0, (int)(size * 2.0F));
      this.generateAtlas();
   }

   private void generateAtlas() {
      BufferedImage img = new BufferedImage(this.texWidth, this.texHeight, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g2d = img.createGraphics();
      g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
      g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      g2d.setFont(this.font);
      FontMetrics metrics = g2d.getFontMetrics();
      this.lineHeight = metrics.getHeight() / 2.0F;
      this.ascent = metrics.getAscent() / 2.0F;
      this.descent = metrics.getDescent() / 2.0F;
      int curX = 6;
      int curY = 6;
      int maxRowHeight = 0;
      StringBuilder charsToRender = new StringBuilder();

      for (int i = 32; i < 127; i++) {
         charsToRender.append((char)i);
      }

      for (int var17 = 1024; var17 <= 1119; var17++) {
         charsToRender.append((char)var17);
      }

      for (int var18 = 59648; var18 <= 59744; var18++) {
         charsToRender.append((char)var18);
      }

      charsToRender.append((char)0x2605);
      charsToRender.append((char)0x2713);
      charsToRender.append((char)0x00A7);
      charsToRender.append((char)0x00BB); // »
      charsToRender.append((char)0x2014); // —
      charsToRender.append((char)0x2022); // •

      for (int arrow = 8592; arrow <= 8601; arrow++) {
         charsToRender.append((char)arrow);
      }

      for (int var19 = 0; var19 < charsToRender.length(); var19++) {
         char c = charsToRender.charAt(var19);
         Rectangle2D bounds = metrics.getStringBounds(String.valueOf(c), g2d);
         float charW = (float)bounds.getWidth();
         float charH = metrics.getHeight();
         if (charW <= 0.0F) {
            charW = 20.0F;
         }

         if (charH <= 0.0F) {
            charH = this.fontSize * 2.0F;
         }

         int iCharW = (int)Math.ceil(charW);
         int iCharH = (int)Math.ceil(charH);
         if (curX + iCharW + 8 >= this.texWidth) {
            curX = 6;
            curY += maxRowHeight + 8;
            maxRowHeight = 0;
         }

         if (curY + iCharH + 8 >= this.texHeight) {
            break;
         }

         g2d.setColor(Color.WHITE);
         g2d.drawString(String.valueOf(c), curX, curY + metrics.getAscent());
         CustomFont.Glyph glyph = new CustomFont.Glyph();
         glyph.width = charW / 2.0F;
         glyph.height = charH / 2.0F;
         glyph.u1 = curX / (float)this.texWidth;
         glyph.v1 = curY / (float)this.texHeight;
         glyph.u2 = (curX + charW) / (float)this.texWidth;
         glyph.v2 = (curY + charH) / (float)this.texHeight;
         this.glyphs.put(c, glyph);
         curX += iCharW + 8;
         if (iCharH > maxRowHeight) {
            maxRowHeight = iCharH;
         }
      }

      g2d.dispose();

      try {
         NativeImage nativeImage = new NativeImage(this.texWidth, this.texHeight, true);

         for (int y = 0; y < this.texHeight; y++) {
            for (int x = 0; x < this.texWidth; x++) {
               int argb = img.getRGB(x, y);
               int alpha = argb >>> 24 & 0xFF;
               int abgr = alpha << 24 | 16777215;
               nativeImage.setColor(x, y, abgr);
            }
         }

         String safeFontName = this.font.getName().toLowerCase().replaceAll("[^a-z0-9_.-]", "_");
         NativeImageBackedTexture tex = new NativeImageBackedTexture(() -> "custom_font_" + safeFontName, nativeImage);
         net.minecraft.util.Identifier fontId = net.minecraft.util.Identifier.of(
            "really", "font/" + safeFontName + "_" + (int) (this.fontSize * 10.0F));
         MinecraftClient.getInstance().getTextureManager().registerTexture(fontId, tex);
         this.textureId = fontId;
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   private static final char SECTION = '\u00a7';
   private static final int[] COLOR_CODES = new int[]{
      0,
      170,
      43520,
      43690,
      11141120,
      11141290,
      16755200,
      11184810,
      5592405,
      5592575,
      5635925,
      5636095,
      16733525,
      16733695,
      16777045,
      16777215
   };

   private static int colorFromCode(char code) {
      String order = "0123456789abcdef";
      int idx = order.indexOf(Character.toLowerCase(code));
      return idx < 0 ? -1 : COLOR_CODES[idx];
   }

   public static String stripFormatting(String text) {
      if (text == null) {
         return null;
      } else {
         StringBuilder sb = new StringBuilder(text.length());

         for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == SECTION && i + 1 < text.length()) {
               i++;
            } else {
               sb.append(c);
            }
         }

         return sb.toString();
      }
   }

   public void drawString(DrawContext context, String text, float x, float y, int color) {
      if (text != null && !text.isEmpty() && this.textureId != null) {
         float a = (color >> 24 & 0xFF) / 255.0F;
         float r = (color >> 16 & 0xFF) / 255.0F;
         float g = (color >> 8 & 0xFF) / 255.0F;
         float b = (color & 0xFF) / 255.0F;
         if (a == 0.0F) {
            a = 1.0F;
         }

         float baseR = r;
         float baseG = g;
         float baseB = b;
         float curX = x;

         for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == SECTION && i + 1 < text.length()) {
               char code = text.charAt(++i);
               int rgb = colorFromCode(code);
               if (rgb >= 0) {
                  r = (rgb >> 16 & 0xFF) / 255.0F;
                  g = (rgb >> 8 & 0xFF) / 255.0F;
                  b = (rgb & 0xFF) / 255.0F;
               } else if (Character.toLowerCase(code) == 'r') {
                  r = baseR;
                  g = baseG;
                  b = baseB;
               }
            } else {
               CustomFont.Glyph glyph = this.glyphs.get(c);
               if (glyph == null) {
                  glyph = this.glyphs.get('?');
               }
               if (glyph != null) {
                  int argb = ((int) (a * 255.0F) << 24) | ((int) (r * 255.0F) << 16)
                           | ((int) (g * 255.0F) << 8) | (int) (b * 255.0F);
                  context.drawTexture(
                     net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, this.textureId,
                     Math.round(curX), Math.round(y),
                     glyph.u1 * this.texWidth, glyph.v1 * this.texHeight,
                     Math.round(glyph.width), Math.round(glyph.height),
                     Math.round((glyph.u2 - glyph.u1) * this.texWidth),
                     Math.round((glyph.v2 - glyph.v1) * this.texHeight),
                     this.texWidth, this.texHeight, argb);
                  curX += glyph.width;
               }
            }
         }
      }
   }

   public int getStringWidth(String text) {
      if (text == null) {
         return 0;
      }

      float w = 0.0F;

      for (int i = 0; i < text.length(); i++) {
         char c = text.charAt(i);
         if (c == SECTION && i + 1 < text.length()) {
            i++;
         } else {
            CustomFont.Glyph g = this.glyphs.get(c);
            if (g == null) {
               g = this.glyphs.get('?');
            }
            if (g != null) {
               w += g.width;
            }
         }
      }

      return (int)Math.ceil(w);
   }

   public float getStringWidthF(String text) {
      if (text == null) {
         return 0.0F;
      }

      float w = 0.0F;

      for (int i = 0; i < text.length(); i++) {
         char c = text.charAt(i);
         if (c == SECTION && i + 1 < text.length()) {
            i++;
         } else {
            CustomFont.Glyph g = this.glyphs.get(c);
            if (g == null) {
               g = this.glyphs.get('?');
            }
            if (g != null) {
               w += g.width;
            }
         }
      }

      return w;
   }

   public float getHeight() {
      return this.lineHeight;
   }

   public float getAscent() {
      return this.ascent;
   }

   public float getDescent() {
      return this.descent;
   }

   public float getCenteredTextY(float boxY, float boxHeight) {
      return boxY + (boxHeight - this.lineHeight) / 2.0F - this.ascent / 8.0F;
   }

   public float getGlyphHeight(char c) {
      CustomFont.Glyph glyph = this.glyphs.get(c);
      return glyph != null ? glyph.height : this.fontSize;
   }

   public static class Glyph {
      public float width;
      public float height;
      public float u1;
      public float v1;
      public float u2;
      public float v2;
   }
}
