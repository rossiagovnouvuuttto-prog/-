package com.reallyvisuals.gui.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Immediate-mode shim for 1.21.11.
 *
 * Tessellator and RenderSystem.setShader are gone, but the call shape of the old
 * BufferBuilder is kept here so existing draw code converts by renaming rather than
 * rewriting. Vertices are buffered and flushed on draw() through a RenderLayer, which
 * owns blend, depth and cull state.
 *
 * LINE_STRIP has no batched layer on this version, so it is expanded into LINES pairs
 * and the per-vertex normals that layer requires are derived from segment direction.
 */
public final class Buf {
   public static final int QUADS = 0;
   public static final int TRIANGLE_FAN = 1;
   public static final int LINE_STRIP = 2;
   public static final int LINES = 3;
   public static final int TEXTURED = 4;

   private final MatrixStack matrices;
   private final VertexConsumerProvider provider;
   private final int mode;
   private final List<float[]> verts = new ArrayList<>();
   private float[] pending;
   private Identifier texture;

   private Buf(MatrixStack matrices, VertexConsumerProvider provider, int mode) {
      this.matrices = matrices;
      this.provider = provider;
      this.mode = mode;
   }

   public static Buf begin(MatrixStack matrices, VertexConsumerProvider provider, int mode) {
      return new Buf(matrices, provider, mode);
   }

   /** Textured quads: pass the atlas the following vertices sample from. */
   public static Buf begin(MatrixStack matrices, VertexConsumerProvider provider, int mode, Identifier texture) {
      Buf b = new Buf(matrices, provider, mode);
      b.texture = texture;
      return b;
   }

   public Buf vertex(Matrix4f matrix, float x, float y, float z) {
      Vector4f v = matrix.transform(new Vector4f(x, y, z, 1.0F));
      pending = new float[] {v.x, v.y, v.z, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F};
      return this;
   }

   public Buf color(float r, float g, float b, float a) {
      if (pending != null) { pending[3] = r; pending[4] = g; pending[5] = b; pending[6] = a; }
      return this;
   }

   public Buf color(int r, int g, int b, int a) {
      return color(r / 255.0F, g / 255.0F, b / 255.0F, a / 255.0F);
   }

   public Buf texture(float u, float v) {
      if (pending != null) { pending[7] = u; pending[8] = v; }
      return this;
   }

   public void next() {
      if (pending != null) { verts.add(pending); pending = null; }
   }

   public void draw() {
      if (pending != null) next();
      if (verts.isEmpty()) return;
      MatrixStack.Entry e = matrices.peek();

      if (mode == LINE_STRIP || mode == LINES) {
         VertexConsumer vc = provider.getBuffer(RenderLayers.LINES);
         int step = mode == LINES ? 2 : 1;
         for (int i = 0; i + 1 < verts.size(); i += step) {
            float[] p = verts.get(i), q = verts.get(i + 1);
            float dx = q[0] - p[0], dy = q[1] - p[1], dz = q[2] - p[2];
            float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 1.0E-5F) continue;
            dx /= len; dy /= len; dz /= len;
            vc.vertex(p[0], p[1], p[2]).color(p[3], p[4], p[5], p[6]).normal(e, dx, dy, dz);
            vc.vertex(q[0], q[1], q[2]).color(q[3], q[4], q[5], q[6]).normal(e, dx, dy, dz);
         }
      } else if (texture != null) {
         VertexConsumer vc = provider.getBuffer(RenderLayers.text(texture));
         for (float[] p : verts) {
            vc.vertex(p[0], p[1], p[2]).color(p[3], p[4], p[5], p[6])
              .texture(p[7], p[8]).light(15728880).normal(e, 0.0F, 0.0F, 1.0F);
         }
      } else {
         VertexConsumer vc = provider.getBuffer(
            mode == TRIANGLE_FAN ? RenderLayers.debugTriangleFan() : RenderLayers.debugQuads());
         for (float[] p : verts) {
            vc.vertex(p[0], p[1], p[2]).color(p[3], p[4], p[5], p[6]);
         }
      }
      verts.clear();
   }
}
