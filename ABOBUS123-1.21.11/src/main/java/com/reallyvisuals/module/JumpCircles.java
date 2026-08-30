package com.reallyvisuals.module;

import net.minecraft.util.math.RotationAxis;

import com.reallyvisuals.gui.render.Buf;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reallyvisuals.gui.ReallyVisualsScreen;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import net.minecraft.util.math.Vec3d;

public class JumpCircles extends Module {
   public Module.NumberSetting radius = new Module.NumberSetting("Радиус", 1.0, 0.5, 3.0, 0.1);
   public Module.NumberSetting speed = new Module.NumberSetting("Скорость", 2.0, 0.5, 5.0, 0.1);
   public Module.BooleanSetting useClientColor = new Module.BooleanSetting("Цвет клиента", true);
   private static final Identifier CIRCLE_TEX = Identifier.of("really", "textures/particle/circle.png");
   private final List<JumpCircles.Circle> circles = new CopyOnWriteArrayList<>();

   public JumpCircles() {
      super("Jump Circles", "Отображение кругов при прыжке игрока", Category.VISUALS, false, true);
      this.addSetting(this.radius);
      this.addSetting(this.speed);
      this.addSetting(this.useClientColor);
      this.customColor = -16718337;
   }

   public void onJump(Vec3d pos) {
      if (this.isEnabled()) {
         this.circles.add(new JumpCircles.Circle(pos, this.radius.value, this.speed.value));
         int max = PerformanceBoost.throttleHeavyVisuals() ? 3 : 8;
         while (this.circles.size() > max) this.circles.remove(0);
      }
   }

   public void renderWorld(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Camera camera) {
      if (this.isEnabled() && !this.circles.isEmpty()) {
         this.circles.removeIf(c -> !c.isAlive());
         if (!this.circles.isEmpty()) {
            Vec3d camPos = camera.getCameraPos();
            int color = this.useClientColor.value ? ReallyVisualsScreen.clientColor : this.customColor;
            float r = (color >> 16 & 0xFF) / 255.0F;
            float g = (color >> 8 & 0xFF) / 255.0F;
            float b = (color & 0xFF) / 255.0F;
            MinecraftClient mc = MinecraftClient.getInstance();
            for (JumpCircles.Circle circle : this.circles) {
               float alpha = circle.getAlpha();
               if (!(alpha <= 0.01F)) {
                  float rad = (float)circle.getCurrentRadius();
                  matrices.push();
                  matrices.translate(circle.pos.x - camPos.x, circle.pos.y - camPos.y + 0.02, circle.pos.z - camPos.z);
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
                  Matrix4f model = matrices.peek().getPositionMatrix();
                  Buf buffer = Buf.begin(matrices, vertexConsumers, Buf.QUADS);
                  buffer.vertex(model, -rad, -rad, 0.0F).color(r, g, b, alpha).texture(0.0F, 0.0F).next();
                  buffer.vertex(model, -rad, rad, 0.0F).color(r, g, b, alpha).texture(0.0F, 1.0F).next();
                  buffer.vertex(model, rad, rad, 0.0F).color(r, g, b, alpha).texture(1.0F, 1.0F).next();
                  buffer.vertex(model, rad, -rad, 0.0F).color(r, g, b, alpha).texture(1.0F, 0.0F).next();
                  buffer.draw();
                  matrices.pop();
               }
            }
         }
      }
   }

   public static class Circle {
      public final Vec3d pos;
      public final long start = System.currentTimeMillis();
      public final double maxRadius;
      public final double speedFactor;

      public Circle(Vec3d pos, double maxRadius, double speedFactor) {
         this.pos = pos;
         this.maxRadius = maxRadius;
         this.speedFactor = speedFactor;
      }

      public double getProgress() {
         double duration = 1200.0 / this.speedFactor;
         return Math.min(1.0, (System.currentTimeMillis() - this.start) / duration);
      }

      public boolean isAlive() {
         return this.getProgress() < 1.0;
      }

      public double getCurrentRadius() {
         double p = this.getProgress();
         double ease = 1.0 - Math.pow(1.0 - p, 3.0);
         return 0.2 + (this.maxRadius - 0.2) * ease;
      }

      public float getAlpha() {
         double p = this.getProgress();
         return (float)(1.0 - p * p);
      }
   }
}
