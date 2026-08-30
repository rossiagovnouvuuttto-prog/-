package com.reallyvisuals.gui.render;

import java.awt.Color;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/**
 * 1.21.11 rewrite.
 *
 * In 1.21.11 the GUI layer is state-based: DrawContext collects typed elements into a
 * GuiRenderState and there is no public way to submit custom vertices. Every shape that
 * used to be built from a triangle fan is therefore composed from two primitives only:
 *
 *   - context.fill(...)        for axis-aligned solid areas
 *   - context.drawTexture(...) for anything curved, sampled from an anti-aliased disc
 *
 * DISC is a 256x256 white circle with a soft edge. Its four quadrants are the four
 * rounded corners; scaling a quadrant to radius x radius reproduces the old SDF look
 * because the alpha falloff is baked into the texture instead of computed per fragment.
 *
 * All coordinates arrive as float (call sites are unchanged in that respect) and are
 * rounded at the boundary, since DrawContext is integer-only.
 */
public class RenderUtils {
   public static final Identifier DISC = Identifier.of("really", "textures/gui/disc.png");

   private static final int DISC_SIZE = 256;
   private static final int DISC_HALF = DISC_SIZE / 2;

   // ---------------------------------------------------------------- helpers

   private static int i(float v) {
      return Math.round(v);
   }

   /** Old code passed 0x00RRGGBB to mean "opaque"; preserve that behaviour. */
   private static int opaque(int color) {
      return (color >>> 24) == 0 ? color | 0xFF000000 : color;
   }

   private static int lerpColor(int a, int b, float t) {
      int aa = a >>> 24 & 0xFF, ar = a >> 16 & 0xFF, ag = a >> 8 & 0xFF, ab = a & 0xFF;
      int ba = b >>> 24 & 0xFF, br = b >> 16 & 0xFF, bg = b >> 8 & 0xFF, bb = b & 0xFF;
      int ra = (int) (aa + (ba - aa) * t);
      int rr = (int) (ar + (br - ar) * t);
      int rg = (int) (ag + (bg - ag) * t);
      int rb = (int) (ab + (bb - ab) * t);
      return ra << 24 | rr << 16 | rg << 8 | rb;
   }

   private static int withAlpha(int color, float mul) {
      int a = (int) ((color >>> 24 & 0xFF) * Math.max(0.0F, Math.min(1.0F, mul)));
      return a << 24 | color & 0x00FFFFFF;
   }

   /**
    * Draws one quadrant of DISC scaled into a radius x radius box.
    * quadX/quadY select the quadrant: 0 = left/top, 1 = right/bottom.
    */
   private static void corner(DrawContext ctx, int x, int y, int r, int quadX, int quadY, int color) {
      if (r <= 0) {
         return;
      }
      ctx.drawTexture(
         RenderPipelines.GUI_TEXTURED,
         DISC,
         x, y,
         quadX * (float) DISC_HALF, quadY * (float) DISC_HALF,
         r, r,
         DISC_HALF, DISC_HALF,
         DISC_SIZE, DISC_SIZE,
         color
      );
   }

   // ---------------------------------------------------------------- rects

   public static void drawRect(DrawContext ctx, float x, float y, float width, float height, int color) {
      ctx.fill(i(x), i(y), i(x + width), i(y + height), opaque(color));
   }

   public static void drawGradientRect(DrawContext ctx, float x, float y, float width, float height, int startColor, int endColor) {
      ctx.fillGradient(i(x), i(y), i(x + width), i(y + height), opaque(startColor), opaque(endColor));
   }

   public static void drawGradientRectAlpha(DrawContext ctx, float x, float y, float width, float height, int startColor, int endColor) {
      ctx.fillGradient(i(x), i(y), i(x + width), i(y + height), startColor, endColor);
   }

   /**
    * fillGradient is vertical only, so a horizontal ramp is drawn as one-pixel columns.
    * Widths here are GUI-scaled, so this stays in the low hundreds of fills.
    */
   public static void drawGradientRectHorizontal(DrawContext ctx, float x, float y, float width, float height, int startColor, int endColor) {
      int x0 = i(x), y0 = i(y), y1 = i(y + height), w = i(width);
      if (w <= 0) {
         return;
      }
      int s = opaque(startColor), e = opaque(endColor);
      for (int c = 0; c < w; c++) {
         float t = w == 1 ? 0.0F : (float) c / (w - 1);
         ctx.fill(x0 + c, y0, x0 + c + 1, y1, lerpColor(s, e, t));
      }
   }

   // ---------------------------------------------------------------- rounded rects

   public static void drawSingleRoundedRect(DrawContext ctx, float x, float y, float width, float height, float radius, int color) {
      int x0 = i(x), y0 = i(y), x1 = i(x + width), y1 = i(y + height);
      int c = opaque(color);
      int r = Math.max(0, Math.min(i(radius), Math.min(x1 - x0, y1 - y0) / 2));

      if (r == 0) {
         ctx.fill(x0, y0, x1, y1, c);
         return;
      }

      corner(ctx, x0, y0, r, 0, 0, c);
      corner(ctx, x1 - r, y0, r, 1, 0, c);
      corner(ctx, x0, y1 - r, r, 0, 1, c);
      corner(ctx, x1 - r, y1 - r, r, 1, 1, c);

      ctx.fill(x0 + r, y0, x1 - r, y1, c);   // middle band, full height
      ctx.fill(x0, y0 + r, x0 + r, y1 - r, c); // left edge
      ctx.fill(x1 - r, y0 + r, x1, y1 - r, c); // right edge
   }

   public static void drawRoundedRect(DrawContext ctx, float x, float y, float width, float height, float radius, int color) {
      drawSingleRoundedRect(ctx, x, y, width, height, radius, color);
   }

   public static void drawGradientRoundedRect(DrawContext ctx, float x, float y, float width, float height, float radius, int startColor, int endColor) {
      int y0 = i(y), h = i(height);
      if (h <= 0) {
         return;
      }
      // Rounded silhouette in the start colour, then horizontal slices blended over it.
      drawSingleRoundedRect(ctx, x, y, width, height, radius, startColor);

      int x0 = i(x), x1 = i(x + width);
      int r = Math.max(0, Math.min(i(radius), Math.min(x1 - x0, h) / 2));
      int s = opaque(startColor), e = opaque(endColor);
      for (int row = r; row < h - r; row++) {
         float t = h == 1 ? 0.0F : (float) row / (h - 1);
         ctx.fill(x0, y0 + row, x1, y0 + row + 1, lerpColor(s, e, t));
      }
   }

   /** Concentric rounded outlines with falling alpha - same look as the old additive glow. */
   public static void drawGlow(DrawContext ctx, float x, float y, float width, float height, float radius, int color, int glowRadius, float maxAlpha) {
      if (glowRadius <= 0) {
         return;
      }
      for (int step = glowRadius; step >= 1; step--) {
         float t = (float) step / glowRadius;
         float a = maxAlpha * (1.0F - t) * (1.0F - t);
         drawSingleRoundedRect(ctx, x - step, y - step, width + step * 2.0F, height + step * 2.0F, radius + step, withAlpha(opaque(color), a));
      }
   }

   // ---------------------------------------------------------------- circles

   public static void drawCircle(DrawContext ctx, float cx, float cy, float radius, int color) {
      int d = i(radius * 2.0F);
      if (d <= 0) {
         return;
      }
      ctx.drawTexture(
         RenderPipelines.GUI_TEXTURED, DISC,
         i(cx - radius), i(cy - radius),
         0.0F, 0.0F,
         d, d,
         DISC_SIZE, DISC_SIZE,
         DISC_SIZE, DISC_SIZE,
         opaque(color)
      );
   }

   /** No stencil available, so the ring is stepped around the circumference. */
   public static void drawCircleOutline(DrawContext ctx, float cx, float cy, float radius, float thickness, int color) {
      int c = opaque(color);
      int t = Math.max(1, i(thickness));
      int segments = Math.max(16, i(radius * 4.0F));
      for (int s = 0; s < segments; s++) {
         double ang = 2.0 * Math.PI * s / segments;
         int px = i(cx + (float) Math.cos(ang) * radius) - t / 2;
         int py = i(cy + (float) Math.sin(ang) * radius) - t / 2;
         ctx.fill(px, py, px + t, py + t, c);
      }
   }

   // ---------------------------------------------------------------- textures

   public static void drawTexture(DrawContext ctx, Identifier texture, float x, float y, float width, float height) {
      int w = i(width), h = i(height);
      ctx.drawTexture(RenderPipelines.GUI_TEXTURED, texture, i(x), i(y), 0.0F, 0.0F, w, h, w, h);
   }

   public static void drawTextureUV(DrawContext ctx, Identifier texture, float x, float y, float width, float height, float u1, float v1, float u2, float v2) {
      int w = i(width), h = i(height);
      // Normalised UVs are expressed against a virtual texture the size of the drawn region.
      int texW = Math.max(1, i(w / Math.max(1.0E-4F, u2 - u1)));
      int texH = Math.max(1, i(h / Math.max(1.0E-4F, v2 - v1)));
      ctx.drawTexture(
         RenderPipelines.GUI_TEXTURED, texture,
         i(x), i(y),
         u1 * texW, v1 * texH,
         w, h,
         texW, texH
      );
   }

   // ---------------------------------------------------------------- colour picker widgets

   public static void drawColorBox(DrawContext ctx, float x, float y, float width, float height, float hue, float radius) {
      int x0 = i(x), y0 = i(y), w = i(width), h = i(height);
      if (w <= 0 || h <= 0) {
         return;
      }
      // saturation across, value down
      for (int col = 0; col < w; col++) {
         float sat = w == 1 ? 1.0F : (float) col / (w - 1);
         int top = Color.HSBtoRGB(hue, sat, 1.0F) | 0xFF000000;
         ctx.fillGradient(x0 + col, y0, x0 + col + 1, y0 + h, top, 0xFF000000);
      }
      if (radius > 0.0F) {
         // knock the square corners back with the surrounding shape's radius
         drawSingleRoundedRect(ctx, x, y, width, height, radius, 0x00000000);
      }
   }

   public static void drawHueBar(DrawContext ctx, float x, float y, float width, float height, float radius) {
      int x0 = i(x), y0 = i(y), w = i(width), h = i(height);
      if (w <= 0 || h <= 0) {
         return;
      }
      boolean vertical = h >= w;
      int steps = vertical ? h : w;
      for (int s = 0; s < steps; s++) {
         float hue = steps == 1 ? 0.0F : (float) s / (steps - 1);
         int c = Color.HSBtoRGB(hue, 1.0F, 1.0F) | 0xFF000000;
         if (vertical) {
            ctx.fill(x0, y0 + s, x0 + w, y0 + s + 1, c);
         } else {
            ctx.fill(x0 + s, y0, x0 + s + 1, y0 + h, c);
         }
      }
   }

   // ---------------------------------------------------------------- icons

   public static void drawHamburgerIcon(DrawContext ctx, float x, float y, float width, int color) {
      float bar = Math.max(1.0F, width / 8.0F);
      float gap = bar * 2.0F;
      for (int line = 0; line < 3; line++) {
         drawSingleRoundedRect(ctx, x, y + line * gap, width, bar, bar / 2.0F, color);
      }
   }

   public static void drawGearIcon(DrawContext ctx, float cx, float cy, float radius, int color) {
      int c = opaque(color);
      drawCircle(ctx, cx, cy, radius, c);
      float toothLen = radius * 0.45F;
      float toothW = Math.max(1.0F, radius * 0.30F);
      for (int tooth = 0; tooth < 8; tooth++) {
         double ang = Math.PI * tooth / 4.0;
         float tx = cx + (float) Math.cos(ang) * (radius + toothLen * 0.5F);
         float ty = cy + (float) Math.sin(ang) * (radius + toothLen * 0.5F);
         drawSingleRoundedRect(ctx, tx - toothW / 2.0F, ty - toothW / 2.0F, toothW, toothW, toothW / 3.0F, c);
      }
      // hub
      drawCircle(ctx, cx, cy, radius * 0.38F, 0xFF000000);
   }

   public static void drawCheckmark(DrawContext ctx, float boxX, float boxY, float size, int color) {
      int c = opaque(color);
      float t = Math.max(1.0F, size / 8.0F);
      // short leg, down-right
      int steps = Math.max(2, i(size * 0.30F));
      for (int s = 0; s < steps; s++) {
         float p = (float) s / steps;
         float px = boxX + size * 0.22F + size * 0.20F * p;
         float py = boxY + size * 0.48F + size * 0.22F * p;
         ctx.fill(i(px), i(py), i(px + t), i(py + t), c);
      }
      // long leg, up-right
      steps = Math.max(2, i(size * 0.50F));
      for (int s = 0; s < steps; s++) {
         float p = (float) s / steps;
         float px = boxX + size * 0.42F + size * 0.36F * p;
         float py = boxY + size * 0.70F - size * 0.40F * p;
         ctx.fill(i(px), i(py), i(px + t), i(py + t), c);
      }
   }

   public static void drawTargetIcon(DrawContext ctx, float x, float y, float size, int color) {
      int c = opaque(color);
      float cx = x + size / 2.0F;
      float cy = y + size / 2.0F;
      float ring = Math.max(1.0F, size / 10.0F);
      drawCircleOutline(ctx, cx, cy, size * 0.45F, ring, c);
      drawCircleOutline(ctx, cx, cy, size * 0.26F, ring, c);
      drawCircle(ctx, cx, cy, size * 0.08F, c);
   }
}
