package com.reallyvisuals.module;

import com.reallyvisuals.gui.render.Buf;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reallyvisuals.gui.ReallyVisualsScreen;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import net.minecraft.util.math.Vec3d;

public class Trails extends Module {
   public Module.BooleanSetting showFirstPerson = new Module.BooleanSetting("Показывать от первого лица", false);
   public Module.ModeSetting trailType = new Module.ModeSetting("Тип следа", new String[]{"Сплошной", "Пунктир", "Затухающий"}, "Сплошной");
   public Module.NumberSetting trailLength = new Module.NumberSetting("Длина следа", 2.5, 0.5, 10.0, 0.5);
   public Module.NumberSetting trailWidth = new Module.NumberSetting("Ширина следа", 0.15, 0.05, 0.5, 0.01);
   public Module.BooleanSetting clientColor = new Module.BooleanSetting("Цвет клиента", true);
   private final List<Trails.TrailPoint> points = new ArrayList<>();
   private Vec3d lastPlayerPos = null;

   public Trails() {
      super("Trails", "Отображение следа игрока", Category.VISUALS, false, true);
      this.addSetting(this.showFirstPerson);
      this.addSetting(this.trailType);
      this.addSetting(this.trailLength);
      this.addSetting(this.trailWidth);
      this.addSetting(this.clientColor);
      this.customColor = -16718337;
   }

   public void onRender3D(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Camera camera, float tickDelta) {
      if (this.isEnabled()) {
         MinecraftClient mc = MinecraftClient.getInstance();
         ClientPlayerEntity player = mc.player;
         if (player != null && mc.world != null) {
            boolean isFirstPerson = mc.options.getPerspective().isFirstPerson();
            if (!isFirstPerson || this.showFirstPerson.value) {
               long now = System.currentTimeMillis();
               double maxAge = this.trailLength.value * 1000.0;
               Vec3d lerpedPos = new Vec3d(
                  MathHelper.lerp(tickDelta, (float) player.getLastRenderPos().x, (float) player.getX()),
                  MathHelper.lerp(tickDelta, (float) player.getLastRenderPos().y, (float) player.getY()),
                  MathHelper.lerp(tickDelta, (float) player.getLastRenderPos().z, (float) player.getZ())
               );
               if (this.lastPlayerPos == null || this.lastPlayerPos.distanceTo(lerpedPos) > 0.02) {
                  this.points.add(new Trails.TrailPoint(lerpedPos.add(0.0, 0.35, 0.0), now));
                  int maxPoints = PerformanceBoost.throttleHeavyVisuals() ? 70 : 180;
                  if (this.points.size() > maxPoints) {
                     this.points.subList(0, this.points.size() - maxPoints).clear();
                  }
                  this.lastPlayerPos = lerpedPos;
               }

               this.points.removeIf(p -> now - p.time > maxAge);
               if (this.points.size() >= 2) {
                  Vec3d camPos = camera.getCameraPos();
                  String tType = this.trailType.value;
                   int color = this.clientColor.value ? ReallyVisualsScreen.clientColor : this.customColor;
                  float r = (color >> 16 & 0xFF) / 255.0F;
                  float g = (color >> 8 & 0xFF) / 255.0F;
                  float b = (color & 0xFF) / 255.0F;
                  float baseHeight = (float)(this.trailWidth.value * 5.0);
                  matrices.push();
                  matrices.translate(-camPos.x, -camPos.y, -camPos.z);
                  Matrix4f matrix = matrices.peek().getPositionMatrix();
                                    boolean isDotted = "Пунктир".equals(tType);
                  boolean isFaded = "Затухающий".equals(tType);
                  Buf bufferBuilder = Buf.begin(matrices, vertexConsumers, Buf.QUADS);

                  for (int i = 0; i + 1 < this.points.size(); i++) {
                     if (!isDotted || i % 3 != 2) {
                        Trails.TrailPoint p1 = this.points.get(i);
                        Trails.TrailPoint p2 = this.points.get(i + 1);
                        float age1 = (float)MathHelper.clamp((now - p1.time) / maxAge, 0.0, 1.0);
                        float age2 = (float)MathHelper.clamp((now - p2.time) / maxAge, 0.0, 1.0);
                        float sinAlpha1 = (float)Math.sin(age1 * Math.PI);
                        float sinAlpha2 = (float)Math.sin(age2 * Math.PI);
                        float fade1 = isFaded ? 1.0F - age1 : 1.0F;
                        float fade2 = isFaded ? 1.0F - age2 : 1.0F;
                        float alpha1 = Math.max(0.08F, sinAlpha1 * 0.38F * fade1);
                        float alpha2 = Math.max(0.08F, sinAlpha2 * 0.38F * fade2);
                        if (i >= this.points.size() - 3) {
                           float nearFactor = (this.points.size() - 1 - i) / 3.0F;
                           alpha1 *= Math.max(0.3F, nearFactor);
                           alpha2 *= Math.max(0.3F, nearFactor);
                        }

                        float h1 = baseHeight * fade1;
                        float h2 = baseHeight * fade2;
                        float x1 = (float)p1.pos.x;
                        float y1 = (float)p1.pos.y;
                        float z1 = (float)p1.pos.z;
                        float x2 = (float)p2.pos.x;
                        float y2 = (float)p2.pos.y;
                        float z2 = (float)p2.pos.z;
                        bufferBuilder.vertex(matrix, x1, y1, z1).color(r, g, b, alpha1).next();
                        bufferBuilder.vertex(matrix, x2, y2, z2).color(r, g, b, alpha2).next();
                        bufferBuilder.vertex(matrix, x2, y2 + h2, z2).color(r, g, b, alpha2).next();
                        bufferBuilder.vertex(matrix, x1, y1 + h1, z1).color(r, g, b, alpha1).next();
                        bufferBuilder.vertex(matrix, x1, y1 + h1, z1).color(r, g, b, alpha1).next();
                        bufferBuilder.vertex(matrix, x2, y2 + h2, z2).color(r, g, b, alpha2).next();
                        bufferBuilder.vertex(matrix, x2, y2, z2).color(r, g, b, alpha2).next();
                        bufferBuilder.vertex(matrix, x1, y1, z1).color(r, g, b, alpha1).next();
                     }
                  }

                  bufferBuilder.draw();
                  Buf bufferBuilder2 = Buf.begin(matrices, vertexConsumers, Buf.LINES);

                  for (int var61 = 0; var61 + 1 < this.points.size(); var61++) {
                     if (!isDotted || var61 % 3 != 2) {
                        Trails.TrailPoint p1 = this.points.get(var61);
                        Trails.TrailPoint p2 = this.points.get(var61 + 1);
                        float age1 = (float)MathHelper.clamp((now - p1.time) / maxAge, 0.0, 1.0);
                        float age2 = (float)MathHelper.clamp((now - p2.time) / maxAge, 0.0, 1.0);
                        float sinAlpha1 = (float)Math.sin(age1 * Math.PI);
                        float sinAlpha2 = (float)Math.sin(age2 * Math.PI);
                        float fade1 = isFaded ? 1.0F - age1 : 1.0F;
                        float fade2 = isFaded ? 1.0F - age2 : 1.0F;
                        float alpha1 = Math.max(0.12F, sinAlpha1 * 0.7F * fade1);
                        float alpha2 = Math.max(0.12F, sinAlpha2 * 0.7F * fade2);
                        float h1 = baseHeight * fade1;
                        float h2 = baseHeight * fade2;
                        float x1 = (float)p1.pos.x;
                        float y1 = (float)p1.pos.y;
                        float z1 = (float)p1.pos.z;
                        float x2 = (float)p2.pos.x;
                        float y2 = (float)p2.pos.y;
                        float z2 = (float)p2.pos.z;
                        bufferBuilder2.vertex(matrix, x1, y1, z1).color(r, g, b, alpha1).next();
                        bufferBuilder2.vertex(matrix, x2, y2, z2).color(r, g, b, alpha2).next();
                        bufferBuilder2.vertex(matrix, x1, y1 + h1, z1).color(r, g, b, alpha1).next();
                        bufferBuilder2.vertex(matrix, x2, y2 + h2, z2).color(r, g, b, alpha2).next();
                     }
                  }

                  bufferBuilder2.draw();
                  matrices.pop();
               }
            }
         }
      }
   }

   @Override
   public void onDisable() {
      this.points.clear();
      this.lastPlayerPos = null;
   }

   public static class TrailPoint {
      public final Vec3d pos;
      public final long time;

      public TrailPoint(Vec3d pos, long time) {
         this.pos = pos;
         this.time = time;
      }
   }
}
