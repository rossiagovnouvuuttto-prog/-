package com.reallyvisuals.gui.render;

import net.minecraft.util.math.RotationAxis;

import com.reallyvisuals.gui.ReallyVisualsScreen;
import com.reallyvisuals.module.Crosshair;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.hit.HitResult.Type;

public class CrosshairRenderUtils {
   public static void renderCrosshair(DrawContext context, Crosshair module, float centerX, float centerY) {
      if (module != null) {
         MinecraftClient mc = MinecraftClient.getInstance();
         int color = module.customColor;
         if (module.clientColor.value) {
            color = ReallyVisualsScreen.clientColor;
         }

         if (module.targetColor.value && mc.crosshairTarget != null && mc.crosshairTarget.getType() == Type.ENTITY) {
            color = -52429;
         }

         float rot = (float)module.rotation.value;
         context.getMatrices().pushMatrix();
         context.getMatrices().translate((float) (centerX), (float) (centerY));
         if (rot != 0.0F) {
            context.getMatrices().rotate((float) Math.toRadians(rot));
         }

         String typeStr = module.type.value;
         if (module.dot.value || "Точка".equals(typeStr)) {
            float dSize = (float)module.dotSize.value;
            if (module.outline.value) {
               RenderUtils.drawRect(context, -dSize / 2.0F - 1.0F, -dSize / 2.0F - 1.0F, dSize + 2.0F, dSize + 2.0F, -16777216);
            }

            RenderUtils.drawRect(context, -dSize / 2.0F, -dSize / 2.0F, dSize, dSize, color);
         }

         if ("Крест".equals(typeStr)) {
            float sz = (float)module.size.value;
            float th = (float)module.thickness.value;
            float gp = (float)module.gap.value;
            if (module.dynamic.value && mc.player != null && (mc.player.forwardSpeed != 0.0F || mc.player.sidewaysSpeed != 0.0F)) {
               gp += 3.0F;
            }

            boolean outline = module.outline.value;
            if (module.lineTop) {
               if (outline) {
                  RenderUtils.drawRect(context, -th / 2.0F - 1.0F, -gp - sz - 1.0F, th + 2.0F, sz + 2.0F, -16777216);
               }

               RenderUtils.drawRect(context, -th / 2.0F, -gp - sz, th, sz, color);
            }

            if (module.lineBottom) {
               if (outline) {
                  RenderUtils.drawRect(context, -th / 2.0F - 1.0F, gp - 1.0F, th + 2.0F, sz + 2.0F, -16777216);
               }

               RenderUtils.drawRect(context, -th / 2.0F, gp, th, sz, color);
            }

            if (module.lineLeft) {
               if (outline) {
                  RenderUtils.drawRect(context, -gp - sz - 1.0F, -th / 2.0F - 1.0F, sz + 2.0F, th + 2.0F, -16777216);
               }

               RenderUtils.drawRect(context, -gp - sz, -th / 2.0F, sz, th, color);
            }

            if (module.lineRight) {
               if (outline) {
                  RenderUtils.drawRect(context, gp - 1.0F, -th / 2.0F - 1.0F, sz + 2.0F, th + 2.0F, -16777216);
               }

               RenderUtils.drawRect(context, gp, -th / 2.0F, sz, th, color);
            }
         } else if ("Круг".equals(typeStr)) {
            float radius = (float)module.circleRadius.value;
            float th = (float)module.circleThickness.value;
            boolean outline = module.outline.value;
            if (module.dynamic.value && mc.player != null && (mc.player.forwardSpeed != 0.0F || mc.player.sidewaysSpeed != 0.0F)) {
               radius += 2.0F;
            }

            if (outline) {
               RenderUtils.drawCircleOutline(context, 0.0F, 0.0F, radius + 1.0F, th + 2.0F, -16777216);
            }

            RenderUtils.drawCircleOutline(context, 0.0F, 0.0F, radius, th, color);
         }

         context.getMatrices().popMatrix();
      }
   }
}
