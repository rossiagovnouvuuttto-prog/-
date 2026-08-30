package com.reallyvisuals.gui.render;

import com.reallyvisuals.module.WorldParticles;
import java.util.List;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * 1.21.11 rewrite.
 *
 * World rendering still uses MatrixStack, so geometry is unchanged. What changed is how
 * it reaches the GPU: Tessellator + RenderSystem.setShader is gone, replaced by
 * VertexConsumerProvider + RenderLayer. The layer now owns blend/depth/cull state, so all
 * the RenderSystem enable and disable bookkeeping is deleted rather than translated - doing
 * it by hand would fight the batching.
 *
 * Every method therefore takes a VertexConsumerProvider. Callers get one from the Fabric
 * world render events (WorldRenderContext.consumers()), reintroduced in 1.21.10.
 *
 * Note on line layers: RenderLayers.LINES uses POSITION_COLOR_NORMAL and will render
 * nothing if the normal is omitted, so line() computes the segment direction per vertex.
 */
public class RenderUtils3D {

   // ---------------------------------------------------------------- helpers

   private static float red(int color) {
      return (color >> 16 & 0xFF) / 255.0F;
   }

   private static float green(int color) {
      return (color >> 8 & 0xFF) / 255.0F;
   }

   private static float blue(int color) {
      return (color & 0xFF) / 255.0F;
   }

   /** One line segment on RenderLayers.LINES; normals are mandatory on that layer. */
   private static void line(MatrixStack matrices, VertexConsumer vc,
                            float x1, float y1, float z1, float x2, float y2, float z2,
                            float r, float g, float b, float a) {
      MatrixStack.Entry entry = matrices.peek();
      float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
      float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
      if (len < 1.0E-5F) {
         return;
      }
      dx /= len;
      dy /= len;
      dz /= len;
      vc.vertex(entry, x1, y1, z1).color(r, g, b, a).normal(entry, dx, dy, dz);
      vc.vertex(entry, x2, y2, z2).color(r, g, b, a).normal(entry, dx, dy, dz);
   }

   // ---------------------------------------------------------------- boxes

   public static void drawFilledBox(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Box box, float r, float g, float b, float alpha) {
      Matrix4f m = matrices.peek().getPositionMatrix();
      VertexConsumer vc = vertexConsumers.getBuffer(RenderLayers.debugQuads());
      float x1 = (float) box.minX, y1 = (float) box.minY, z1 = (float) box.minZ;
      float x2 = (float) box.maxX, y2 = (float) box.maxY, z2 = (float) box.maxZ;
      float[][] faces = {
         {x1,y1,z1, x2,y1,z1, x2,y1,z2, x1,y1,z2},
         {x1,y2,z2, x2,y2,z2, x2,y2,z1, x1,y2,z1},
         {x1,y1,z1, x1,y2,z1, x2,y2,z1, x2,y1,z1},
         {x2,y1,z2, x2,y2,z2, x1,y2,z2, x1,y1,z2},
         {x1,y1,z2, x1,y2,z2, x1,y2,z1, x1,y1,z1},
         {x2,y1,z1, x2,y2,z1, x2,y2,z2, x2,y1,z2}
      };
      for (float[] f : faces) {
         for (int v = 0; v < 4; v++) {
            vc.vertex(m, f[v*3], f[v*3+1], f[v*3+2]).color(r, g, b, alpha);
         }
      }
   }

   public static void drawOutlineBox(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Box box, float r, float g, float b, float alpha) {
      VertexConsumer vc = vertexConsumers.getBuffer(RenderLayers.LINES);
      float x1 = (float) box.minX, y1 = (float) box.minY, z1 = (float) box.minZ;
      float x2 = (float) box.maxX, y2 = (float) box.maxY, z2 = (float) box.maxZ;
      for (int i = 0; i < 2; i++) {
         float y = i == 0 ? y1 : y2;
         line(matrices, vc, x1, y, z1, x2, y, z1, r, g, b, alpha);
         line(matrices, vc, x2, y, z1, x2, y, z2, r, g, b, alpha);
         line(matrices, vc, x2, y, z2, x1, y, z2, r, g, b, alpha);
         line(matrices, vc, x1, y, z2, x1, y, z1, r, g, b, alpha);
      }
      line(matrices, vc, x1, y1, z1, x1, y2, z1, r, g, b, alpha);
      line(matrices, vc, x2, y1, z1, x2, y2, z1, r, g, b, alpha);
      line(matrices, vc, x2, y1, z2, x2, y2, z2, r, g, b, alpha);
      line(matrices, vc, x1, y1, z2, x1, y2, z2, r, g, b, alpha);
   }

   public static void drawBlockOverlay(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Box box, boolean drawOutline, boolean drawFill, int color) {
      float r = red(color), g = green(color), b = blue(color);
      if (drawFill) {
         drawFilledBox(matrices, vertexConsumers, box, r, g, b, 0.25F);
      }
      if (drawOutline) {
         drawOutlineBox(matrices, vertexConsumers, box, r, g, b, 0.9F);
      }
   }

   /** Only the corners of each edge, lenRatio of the way along. */
   public static void drawCornerBox(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Box box, float r, float g, float b, float alpha, float lenRatio) {
      VertexConsumer vc = vertexConsumers.getBuffer(RenderLayers.LINES);

      float x1 = (float) box.minX, y1 = (float) box.minY, z1 = (float) box.minZ;
      float x2 = (float) box.maxX, y2 = (float) box.maxY, z2 = (float) box.maxZ;
      float dx = (x2 - x1) * lenRatio;
      float dy = (y2 - y1) * lenRatio;
      float dz = (z2 - z1) * lenRatio;

      for (int cx = 0; cx < 2; cx++) {
         for (int cy = 0; cy < 2; cy++) {
            for (int cz = 0; cz < 2; cz++) {
               float px = cx == 0 ? x1 : x2;
               float py = cy == 0 ? y1 : y2;
               float pz = cz == 0 ? z1 : z2;
               float sx = cx == 0 ? dx : -dx;
               float sy = cy == 0 ? dy : -dy;
               float sz = cz == 0 ? dz : -dz;
               line(matrices, vc, px, py, pz, px + sx, py, pz, r, g, b, alpha);
               line(matrices, vc, px, py, pz, px, py + sy, pz, r, g, b, alpha);
               line(matrices, vc, px, py, pz, px, py, pz + sz, r, g, b, alpha);
            }
         }
      }
   }

   // ---------------------------------------------------------------- lines

   public static void draw3DLine(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Vec3d start, Vec3d end, float r, float g, float b, float alpha) {
      line(
         matrices, vertexConsumers.getBuffer(RenderLayers.LINES),
         (float) start.x, (float) start.y, (float) start.z,
         (float) end.x, (float) end.y, (float) end.z,
         r, g, b, alpha
      );
   }

   public static void drawSpeedLine(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Vec3d pos, Vec3d velocity, int color, float alpha) {
      draw3DLine(matrices, vertexConsumers, pos, pos.add(velocity), red(color), green(color), blue(color), alpha);
   }

   public static void drawWindRing(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Vec3d pos, Vec3d dir, float radius, int color, float alpha) {
      float r = red(color), g = green(color), b = blue(color);
      int segments = 24;

      matrices.push();
      matrices.translate(pos.x, pos.y, pos.z);
      VertexConsumer vc = vertexConsumers.getBuffer(RenderLayers.LINES);

      for (int i = 0; i < segments; i++) {
         double a0 = Math.toRadians(i * (360.0 / segments));
         double a1 = Math.toRadians((i + 1) * (360.0 / segments));
         line(
            matrices, vc,
            (float) Math.cos(a0) * radius, (float) Math.sin(a0) * radius, 0.0F,
            (float) Math.cos(a1) * radius, (float) Math.sin(a1) * radius, 0.0F,
            r, g, b, alpha
         );
      }
      matrices.pop();
   }

   // ---------------------------------------------------------------- surfaces

   public static void drawChinaHat(MatrixStack matrices, VertexConsumerProvider vertexConsumers, float hatHeight, float opacity, boolean drawOutline, int color) {
      float r = red(color), g = green(color), b = blue(color);
      float radius = 0.65F;
      int segments = 36;
      Matrix4f matrix = matrices.peek().getPositionMatrix();

      // TRIANGLE_FAN is unavailable as a batched layer, so the cone is emitted as
      // independent triangles on the quad layer (apex, edge, edge, edge-repeat).
      VertexConsumer vc = vertexConsumers.getBuffer(RenderLayers.debugQuads());
      for (int i = 0; i < segments; i++) {
         double a0 = Math.toRadians(i * (360.0 / segments));
         double a1 = Math.toRadians((i + 1) * (360.0 / segments));
         float x0 = (float) Math.cos(a0) * radius, z0 = (float) Math.sin(a0) * radius;
         float x1 = (float) Math.cos(a1) * radius, z1 = (float) Math.sin(a1) * radius;

         vc.vertex(matrix, 0.0F, hatHeight, 0.0F).color(r, g, b, opacity);
         vc.vertex(matrix, x0, 0.0F, z0).color(r, g, b, opacity);
         vc.vertex(matrix, x1, 0.0F, z1).color(r, g, b, opacity);
         vc.vertex(matrix, x1, 0.0F, z1).color(r, g, b, opacity);
      }

      if (drawOutline) {
         VertexConsumer lines = vertexConsumers.getBuffer(RenderLayers.LINES);
         for (int i = 0; i < segments; i++) {
            double a0 = Math.toRadians(i * (360.0 / segments));
            double a1 = Math.toRadians((i + 1) * (360.0 / segments));
            line(
               matrices, lines,
               (float) Math.cos(a0) * radius, 0.0F, (float) Math.sin(a0) * radius,
               (float) Math.cos(a1) * radius, 0.0F, (float) Math.sin(a1) * radius,
               r, g, b, 0.95F
            );
         }
      }
   }

   public static void drawParticle3D(MatrixStack matrices, VertexConsumerProvider vertexConsumers, WorldParticles.Particle3D p, int color, boolean glow) {
      float r = red(color), g = green(color), b = blue(color);
      float a = p.alpha;

      matrices.push();
      matrices.translate(p.pos.x, p.pos.y, p.pos.z);
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(p.rotX));
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(p.rotY));
      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(p.rotZ));

      if (p.type.equals("\u041a\u0443\u0431\u0438\u043a\u0438")) {
         Box box = new Box(-p.scale / 2.0F, -p.scale / 2.0F, -p.scale / 2.0F, p.scale / 2.0F, p.scale / 2.0F, p.scale / 2.0F);
         drawFilledBox(matrices, vertexConsumers, box, r, g, b, a * 0.25F);
         drawOutlineBox(matrices, vertexConsumers, box, r, g, b, a * 0.9F);
      } else {
         Matrix4f matrix = matrices.peek().getPositionMatrix();
         VertexConsumer vc = vertexConsumers.getBuffer(RenderLayers.debugQuads());
         float s = p.scale;
         vc.vertex(matrix, -s, -s, 0.0F).color(r, g, b, a);
         vc.vertex(matrix, s, -s, 0.0F).color(r, g, b, a);
         vc.vertex(matrix, s, s, 0.0F).color(r, g, b, a);
         vc.vertex(matrix, -s, s, 0.0F).color(r, g, b, a);
      }
      matrices.pop();
   }

   /** Trail ribbon: consecutive point pairs become quads, alpha fading along the tail. */
   public static void drawRibbonStream(MatrixStack matrices, VertexConsumerProvider vertexConsumers, List<Vec3d> points, int color, float alpha) {
      if (points.size() < 2) {
         return;
      }
      float r = red(color), g = green(color), b = blue(color);
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      VertexConsumer vc = vertexConsumers.getBuffer(RenderLayers.debugQuads());
      float width = 0.12F;

      for (int i = 0; i < points.size() - 1; i++) {
         Vec3d p0 = points.get(i);
         Vec3d p1 = points.get(i + 1);
         float a0 = alpha * (1.0F - (float) i / points.size());
         float a1 = alpha * (1.0F - (float) (i + 1) / points.size());

         vc.vertex(matrix, (float) p0.x, (float) (p0.y - width), (float) p0.z).color(r, g, b, a0);
         vc.vertex(matrix, (float) p1.x, (float) (p1.y - width), (float) p1.z).color(r, g, b, a1);
         vc.vertex(matrix, (float) p1.x, (float) (p1.y + width), (float) p1.z).color(r, g, b, a1);
         vc.vertex(matrix, (float) p0.x, (float) (p0.y + width), (float) p0.z).color(r, g, b, a0);
      }
   }
}
