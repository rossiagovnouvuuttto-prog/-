package com.reallyvisuals.module;

import com.reallyvisuals.gui.render.Buf;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reallyvisuals.gui.ReallyVisualsScreen;
import com.reallyvisuals.gui.render.RenderUtils3D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

public class TargetESP extends Module {
   public Module.ModeSetting mode = new Module.ModeSetting("Режим", new String[]{"Свэня", "Кольцо", "Кубики", "Кристаллы", "Глитч", "Кометы", "Печать"}, "Свэня");
   public Module.NumberSetting cometCount = (Module.NumberSetting)new Module.NumberSetting("Кол-во комет", 3.0, 1.0, 10.0, 1.0)
      .visible(() -> this.mode.value.equals("Кометы"));
   public Module.NumberSetting speed = new Module.NumberSetting("Скорость", 1.0, 0.5, 5.0, 0.1);
   public Module.NumberSetting cometTailLength = (Module.NumberSetting)new Module.NumberSetting("Длина хвоста", 120.0, 20.0, 300.0, 10.0)
      .visible(() -> this.mode.value.equals("Кометы"));
   public Module.BooleanSetting clientColor = new Module.BooleanSetting("Цвет клиента", true);
   public Module.BooleanSetting damageReaction = new Module.BooleanSetting("Реакция на урон", true);
   public Module.ColorSetting damageColor = (Module.ColorSetting)new Module.ColorSetting("Цвет урона", -52686).visible(() -> this.damageReaction.value);
   private PigEntity swenyaPig;
   private ClientWorld swenyaPigWorld;
   private LivingEntity lastTarget;
   private long lastTargetSeenAt;

   public TargetESP() {
      super("Target ESP", "Свэня TargetESP: свинки вокруг текущей цели", Category.VISUALS, false, true);
      this.addSetting(this.mode);
      this.addSetting(this.cometCount);
      this.addSetting(this.speed);
      this.addSetting(this.cometTailLength);
      this.addSetting(this.clientColor);
      this.addSetting(this.damageReaction);
      this.addSetting(this.damageColor);
      this.customColor = -16718337;
   }

   public void onRender3D(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Camera camera, float tickDelta) {
      if (this.isEnabled()) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.world != null && mc.player != null) {
            LivingEntity target = this.getTargetEntity(mc);
            if (target != null && target.isAlive() && target != mc.player) {
               Vec3d camPos = camera.getCameraPos();
               net.minecraft.util.math.Vec3d lastPos = target.getLastRenderPos();
               double x = MathHelper.lerp(tickDelta, lastPos.x, target.getX());
               double y = MathHelper.lerp(tickDelta, lastPos.y, target.getY());
               double z = MathHelper.lerp(tickDelta, lastPos.z, target.getZ());
               int mainColor = this.clientColor.value ? ReallyVisualsScreen.clientColor : this.customColor;
               if (this.damageReaction.value && target.hurtTime > 0) {
                  mainColor = this.damageColor.color;
               }

               float r = (mainColor >> 16 & 0xFF) / 255.0F;
               float g = (mainColor >> 8 & 0xFF) / 255.0F;
               float b = (mainColor & 0xFF) / 255.0F;
               double animTime = System.currentTimeMillis() % 100000L / 1000.0 * this.speed.value;
               String m = this.mode.value;
               switch (this.mode.value) {
                  case "Свэня":
                     this.renderSwenya(matrices, vertexConsumers, camera, target, x, y, z, tickDelta);
                     break;
                  case "Кольцо":
                     this.renderRing(matrices, vertexConsumers, camPos, x, y, z, target.getWidth(), target.getHeight(), r, g, b, animTime);
                     break;
                  case "Кубики":
                     this.renderCubes(matrices, vertexConsumers, camPos, x, y, z, target.getWidth(), target.getHeight(), r, g, b, animTime);
                     break;
                  case "Кристаллы":
                     this.renderCrystals(matrices, vertexConsumers, camPos, x, y, z, target.getWidth(), target.getHeight(), r, g, b, animTime);
                     break;
                  case "Глитч":
                     this.renderGlitch(matrices, vertexConsumers, camPos, x, y, z, target.getWidth(), target.getHeight(), r, g, b, animTime);
                     break;
                  case "Кометы":
                     this.renderComets(matrices, vertexConsumers, camPos, x, y, z, target.getWidth(), target.getHeight(), r, g, b, animTime);
                     break;
                  case "Печать":
                     this.renderSeal(matrices, vertexConsumers, camPos, x, y, z, target.getWidth(), target.getHeight(), r, g, b, animTime);
               }
            }
         }
      }
   }

   private LivingEntity getTargetEntity(MinecraftClient mc) {
      Entity entity = null;
      if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == Type.ENTITY) {
         entity = ((EntityHitResult)mc.crosshairTarget).getEntity();
      } else if (mc.targetedEntity != null) {
         entity = mc.targetedEntity;
      }

      if (entity instanceof LivingEntity && entity != mc.player) {
         LivingEntity living = (LivingEntity)entity;
         if (living.isAlive()) {
            this.lastTarget = living;
            this.lastTargetSeenAt = System.currentTimeMillis();
            return living;
         }
      }

      if (this.lastTarget != null
         && this.lastTarget.isAlive()
         && this.lastTarget.getEntityWorld() == mc.world
         && mc.player.squaredDistanceTo(this.lastTarget) <= 144.0
         && System.currentTimeMillis() - this.lastTargetSeenAt <= 1500L) {
         return this.lastTarget;
      }

      this.lastTarget = null;
      return null;
   }

   private void renderSwenya(
      MatrixStack matrices, VertexConsumerProvider vertexConsumers, Camera camera, LivingEntity target, double x, double y, double z, float tickDelta
   ) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.world == null) {
         return;
      }

      if (this.swenyaPig == null || this.swenyaPigWorld != mc.world) {
         this.swenyaPigWorld = mc.world;
         this.swenyaPig = new PigEntity(EntityType.PIG, mc.world);
         this.swenyaPig.setSilent(true);
         this.swenyaPig.setInvulnerable(true);
      }

      PigEntity pig = this.swenyaPig;
      EntityRenderManager dispatcher = mc.getEntityRenderDispatcher();
      Vec3d camPos = camera.getCameraPos();
      double radius = Math.max(0.72, target.getWidth() * 0.5 + 0.42);
      double centerY = y + Math.max(0.65, target.getHeight() * 0.48);
      double time = (System.currentTimeMillis() % 1000000L) * 0.00025 * this.speed.value;
      boolean throttled = PerformanceBoost.throttleHeavyVisuals();
      int orbitCount = throttled ? 4 : 8;

      for (int i = 0; i < orbitCount; i++) {
         double angle = time * Math.PI * 2.0 + i * Math.PI * 2.0 / orbitCount;
         double px = x + Math.cos(angle) * radius;
         double pz = z + Math.sin(angle) * radius;
         double py = centerY + ((i & 1) == 0 ? 0.10 : -0.10);
         float yaw = (float)Math.toDegrees(-angle) + 90.0F;
         pig.setYaw(yaw);
         pig.setBodyYaw(yaw);
         pig.setHeadYaw(yaw);

         matrices.push();
         matrices.translate(px - camPos.x, py - camPos.y, pz - camPos.z);
         matrices.scale(0.30F, 0.30F, 0.30F);
         // 1.21.11: EntityRenderManager has no external render(); mode unavailable.
         matrices.pop();
      }

      // The ninth pig spins above the target, matching the original "Свеня" effect.
      if (!throttled) {
         float spin = (float)(time * 180.0);
         matrices.push();
         matrices.translate(x - camPos.x, y + target.getHeight() + 0.45 - camPos.y, z - camPos.z);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(spin));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float)Math.sin(time * 1.5) * 120.0F));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)Math.cos(time * 1.2) * 90.0F));
         matrices.scale(0.40F, 0.40F, 0.40F);
         // 1.21.11: EntityRenderManager has no external render(); mode unavailable.
         matrices.pop();
      }

      
   }

   private void renderRing(
      MatrixStack matrices, VertexConsumerProvider vertexConsumers, Vec3d camPos, double x, double y, double z, float width, float height, float r, float g, float b, double animTime
   ) {
      float rad = width / 2.0F + 0.25F;
      double progress = (Math.sin(animTime * 3.0) + 1.0) / 2.0;
      double ringY = y + height * progress;
      double ringHeight = 0.25;
      matrices.push();
      matrices.translate(x - camPos.x, ringY - camPos.y, z - camPos.z);
      Matrix4f matrix = matrices.peek().getPositionMatrix();
            Buf bufferBuilder = Buf.begin(matrices, vertexConsumers, Buf.QUADS);
      int segments = 36;

      for (int i = 0; i < segments; i++) {
         double a1 = Math.toRadians(i * 360.0 / segments);
         double a2 = Math.toRadians((i + 1) * 360.0 / segments);
         float x1 = (float)(Math.cos(a1) * rad);
         float z1 = (float)(Math.sin(a1) * rad);
         float x2 = (float)(Math.cos(a2) * rad);
         float z2 = (float)(Math.sin(a2) * rad);
         bufferBuilder.vertex(matrix, x1, 0.0F, z1).color(r, g, b, 0.45F).next();
         bufferBuilder.vertex(matrix, x2, 0.0F, z2).color(r, g, b, 0.45F).next();
         bufferBuilder.vertex(matrix, x2, (float)(-ringHeight), z2).color(r, g, b, 0.0F).next();
         bufferBuilder.vertex(matrix, x1, (float)(-ringHeight), z1).color(r, g, b, 0.0F).next();
      }

      bufferBuilder.draw();
      Buf bufferBuilder2 = Buf.begin(matrices, vertexConsumers, Buf.LINE_STRIP);

      for (int var35 = 0; var35 <= segments; var35++) {
         double a = Math.toRadians(var35 * 360.0 / segments);
         float vx = (float)(Math.cos(a) * rad);
         float vz = (float)(Math.sin(a) * rad);
         bufferBuilder2.vertex(matrix, vx, 0.0F, vz).color(r, g, b, 0.95F).next();
      }

      bufferBuilder2.draw();
      matrices.pop();
   }

   private void renderCubes(
      MatrixStack matrices, VertexConsumerProvider vertexConsumers, Vec3d camPos, double x, double y, double z, float width, float height, float r, float g, float b, double animTime
   ) {
      int itemsPerLayer = 4;
      int layers = 3;
      float rad = width / 2.0F + 0.35F;
      Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();

      for (int l = 0; l < layers; l++) {
         double layerY = y + height * 0.15 + l * (height * 0.35);
         double layerOffset = l * (Math.PI / 4);

         for (int i = 0; i < itemsPerLayer; i++) {
            double angle = animTime * 1.8 + layerOffset + i * Math.PI * 2.0 / itemsPerLayer;
            double px = x + Math.cos(angle) * rad;
            double pz = z + Math.sin(angle) * rad;
            matrices.push();
            matrices.translate(px - camPos.x, layerY - camPos.y, pz - camPos.z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(animTime * 45.0 + i * 30)));
            Matrix4f matrix = matrices.peek().getPositionMatrix();
                        Buf bufferBuilder = Buf.begin(matrices, vertexConsumers, Buf.QUADS);
            float sOuter = 0.22F;
            bufferBuilder.vertex(matrix, -sOuter, -sOuter, 0.0F).color(r, g, b, 0.25F).next();
            bufferBuilder.vertex(matrix, sOuter, -sOuter, 0.0F).color(r, g, b, 0.25F).next();
            bufferBuilder.vertex(matrix, sOuter, sOuter, 0.0F).color(r, g, b, 0.25F).next();
            bufferBuilder.vertex(matrix, -sOuter, sOuter, 0.0F).color(r, g, b, 0.25F).next();
            bufferBuilder.draw();
            Buf bufferBuilder2 = Buf.begin(matrices, vertexConsumers, Buf.QUADS);
            float sInner = 0.11F;
            bufferBuilder2.vertex(matrix, -sInner, -sInner, 0.0F).color(r, g, b, 0.95F).next();
            bufferBuilder2.vertex(matrix, sInner, -sInner, 0.0F).color(r, g, b, 0.95F).next();
            bufferBuilder2.vertex(matrix, sInner, sInner, 0.0F).color(r, g, b, 0.95F).next();
            bufferBuilder2.vertex(matrix, -sInner, sInner, 0.0F).color(r, g, b, 0.95F).next();
            bufferBuilder2.draw();
            matrices.pop();
         }
      }
   }

   private void renderCrystals(
      MatrixStack matrices, VertexConsumerProvider vertexConsumers, Vec3d camPos, double x, double y, double z, float width, float height, float r, float g, float b, double animTime
   ) {
      int itemsPerLayer = 5;
      int layers = 4;
      float rad = width / 2.0F + 0.3F;
      Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();

      for (int l = 0; l < layers; l++) {
         double layerY = y + height * 0.1 + l * (height * 0.25);
         double layerOffset = l * (Math.PI / 3);

         for (int i = 0; i < itemsPerLayer; i++) {
            double angle = animTime * 2.2 + layerOffset + i * Math.PI * 2.0 / itemsPerLayer;
            double px = x + Math.cos(angle) * rad;
            double pz = z + Math.sin(angle) * rad;
            matrices.push();
            matrices.translate(px - camPos.x, layerY - camPos.y, pz - camPos.z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(45.0F));
            Matrix4f matrix = matrices.peek().getPositionMatrix();
                        Buf bufferBuilder = Buf.begin(matrices, vertexConsumers, Buf.QUADS);
            float sOuter = 0.09F;
            bufferBuilder.vertex(matrix, -sOuter, -sOuter, 0.0F).color(r, g, b, 0.25F).next();
            bufferBuilder.vertex(matrix, sOuter, -sOuter, 0.0F).color(r, g, b, 0.25F).next();
            bufferBuilder.vertex(matrix, sOuter, sOuter, 0.0F).color(r, g, b, 0.25F).next();
            bufferBuilder.vertex(matrix, -sOuter, sOuter, 0.0F).color(r, g, b, 0.25F).next();
            bufferBuilder.draw();
            Buf bufferBuilder2 = Buf.begin(matrices, vertexConsumers, Buf.QUADS);
            float sInner = 0.045F;
            bufferBuilder2.vertex(matrix, -sInner, -sInner, 0.0F).color(r, g, b, 0.95F).next();
            bufferBuilder2.vertex(matrix, sInner, -sInner, 0.0F).color(r, g, b, 0.95F).next();
            bufferBuilder2.vertex(matrix, sInner, sInner, 0.0F).color(r, g, b, 0.95F).next();
            bufferBuilder2.vertex(matrix, -sInner, sInner, 0.0F).color(r, g, b, 0.95F).next();
            bufferBuilder2.draw();
            matrices.pop();
         }
      }
   }

   private void renderGlitch(
      MatrixStack matrices, VertexConsumerProvider vertexConsumers, Vec3d camPos, double x, double y, double z, float width, float height, float r, float g, float b, double animTime
   ) {
      matrices.push();
      matrices.translate(x - camPos.x, y - camPos.y, z - camPos.z);
      Box targetBox = new Box(-width / 2.0, 0.0, -width / 2.0, width / 2.0, height, width / 2.0);
      long seed = (long)(animTime * 18.0);
      double jitterX = (seed * 17L % 7L - 3L) * 0.015;
      double jitterY = (seed * 31L % 7L - 3L) * 0.015;
      double jitterZ = (seed * 43L % 7L - 3L) * 0.015;
      Box glitchedBox = targetBox.offset(jitterX, jitterY, jitterZ);
      RenderUtils3D.drawOutlineBox(matrices, vertexConsumers, glitchedBox, 0.05F, 0.05F, 0.05F, 0.95F);
      if (seed % 3L == 0L) {
         Box subBox = glitchedBox.expand(0.02, -height * 0.2, 0.02);
         RenderUtils3D.drawOutlineBox(matrices, vertexConsumers, subBox, 0.05F, 0.05F, 0.05F, 0.85F);
      }

      matrices.pop();
   }

   private void renderComets(
      MatrixStack matrices, VertexConsumerProvider vertexConsumers, Vec3d camPos, double x, double y, double z, float width, float height, float r, float g, float b, double animTime
   ) {
      int count = Math.max(1, (int)this.cometCount.value);
      int tailSegments = Math.max(10, (int)(this.cometTailLength.value / 6.0));
      float rad = width / 2.0F + 0.45F;
      Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();

      for (int c = 0; c < count; c++) {
         matrices.push();
         matrices.translate(x - camPos.x, y + height * 0.5 - camPos.y, z - camPos.z);
         float tiltX = 35.0F * (c == 0 ? 1.0F : (c == 1 ? -1.0F : 0.5F));
         float tiltZ = 45.0F * (c == 1 ? 1.0F : (c == 2 ? -1.0F : 0.3F));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(tiltX));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(tiltZ));
         Matrix4f matrix = matrices.peek().getPositionMatrix();
                  Buf bufferBuilder = Buf.begin(matrices, vertexConsumers, Buf.LINE_STRIP);
         int ringSegments = 40;

         for (int s = 0; s <= ringSegments; s++) {
            double a = Math.toRadians(s * 360.0 / ringSegments);
            float vx = (float)(Math.cos(a) * rad);
            float vz = (float)(Math.sin(a) * rad);
            bufferBuilder.vertex(matrix, vx, 0.0F, vz).color(r, g, b, 0.3F).next();
         }

         bufferBuilder.draw();
         Buf bufferBuilder2 = Buf.begin(matrices, vertexConsumers, Buf.LINE_STRIP);
         Vec3d headPos = null;

         for (int i = 0; i <= tailSegments; i++) {
            double t = animTime * 2.5 + c * Math.PI * 2.0 / count - i * 0.04;
            float vx = (float)(Math.cos(t) * rad);
            float vz = (float)(Math.sin(t) * rad);
            float alpha = (1.0F - (float)i / tailSegments) * 0.95F;
            if (i == 0) {
               headPos = new Vec3d(vx, 0.0, vz);
            }

            bufferBuilder2.vertex(matrix, vx, 0.0F, vz).color(r, g, b, alpha).next();
         }

         bufferBuilder2.draw();
         if (headPos != null) {
            matrices.push();
            matrices.translate(headPos.x, 0.0, headPos.z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            Matrix4f headMatrix = matrices.peek().getPositionMatrix();
            Buf bufferBuilder3 = Buf.begin(matrices, vertexConsumers, Buf.QUADS);
            float hsOuter = 0.16F;
            bufferBuilder3.vertex(headMatrix, -hsOuter, -hsOuter, 0.0F).color(r, g, b, 0.45F).next();
            bufferBuilder3.vertex(headMatrix, hsOuter, -hsOuter, 0.0F).color(r, g, b, 0.45F).next();
            bufferBuilder3.vertex(headMatrix, hsOuter, hsOuter, 0.0F).color(r, g, b, 0.45F).next();
            bufferBuilder3.vertex(headMatrix, -hsOuter, hsOuter, 0.0F).color(r, g, b, 0.45F).next();
            bufferBuilder3.draw();
            Buf bufferBuilder4 = Buf.begin(matrices, vertexConsumers, Buf.QUADS);
            float hsInner = 0.08F;
            bufferBuilder4.vertex(headMatrix, -hsInner, -hsInner, 0.0F).color(1.0F, 0.95F, 0.7F, 1.0F).next();
            bufferBuilder4.vertex(headMatrix, hsInner, -hsInner, 0.0F).color(1.0F, 0.95F, 0.7F, 1.0F).next();
            bufferBuilder4.vertex(headMatrix, hsInner, hsInner, 0.0F).color(1.0F, 0.95F, 0.7F, 1.0F).next();
            bufferBuilder4.vertex(headMatrix, -hsInner, hsInner, 0.0F).color(1.0F, 0.95F, 0.7F, 1.0F).next();
            bufferBuilder4.draw();
            matrices.pop();
         }

         matrices.pop();
      }
   }

   private void renderSeal(
      MatrixStack matrices, VertexConsumerProvider vertexConsumers, Vec3d camPos, double x, double y, double z, float width, float height, float r, float g, float b, double animTime
   ) {
      float rad = width / 2.0F + 0.5F;
      matrices.push();
      matrices.translate(x - camPos.x, y + 0.02 - camPos.y, z - camPos.z);
      Matrix4f matrix = matrices.peek().getPositionMatrix();
            Buf bufferBuilder = Buf.begin(matrices, vertexConsumers, Buf.QUADS);
      int segments = 36;
      float wallHeight = 0.35F;

      for (int i = 0; i < segments; i++) {
         double a1 = Math.toRadians(i * 360.0 / segments);
         double a2 = Math.toRadians((i + 1) * 360.0 / segments);
         float x1 = (float)(Math.cos(a1) * rad);
         float z1 = (float)(Math.sin(a1) * rad);
         float x2 = (float)(Math.cos(a2) * rad);
         float z2 = (float)(Math.sin(a2) * rad);
         bufferBuilder.vertex(matrix, x1, 0.0F, z1).color(r, g, b, 0.25F).next();
         bufferBuilder.vertex(matrix, x2, 0.0F, z2).color(r, g, b, 0.25F).next();
         bufferBuilder.vertex(matrix, x2, wallHeight, z2).color(r, g, b, 0.0F).next();
         bufferBuilder.vertex(matrix, x1, wallHeight, z1).color(r, g, b, 0.0F).next();
      }

      bufferBuilder.draw();
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(animTime * 35.0)));
      matrix = matrices.peek().getPositionMatrix();
      Buf bufferBuilder2 = Buf.begin(matrices, vertexConsumers, Buf.LINE_STRIP);

      for (int var34 = 0; var34 <= segments; var34++) {
         double a = Math.toRadians(var34 * 360.0 / segments);
         float vx = (float)(Math.cos(a) * rad);
         float vz = (float)(Math.sin(a) * rad);
         bufferBuilder2.vertex(matrix, vx, 0.0F, vz).color(r, g, b, 0.9F).next();
      }

      bufferBuilder2.draw();
      float innerRad = rad * 0.75F;
      Buf bufferBuilder3 = Buf.begin(matrices, vertexConsumers, Buf.LINES);

      for (int i2 = 0; i2 < segments; i2 += 2) {
         double a1 = Math.toRadians(i2 * 360.0 / segments);
         double a2 = Math.toRadians((i2 + 1) * 360.0 / segments);
         float vx1 = (float)(Math.cos(a1) * innerRad);
         float vz1 = (float)(Math.sin(a1) * innerRad);
         float vx2 = (float)(Math.cos(a2) * innerRad);
         float vz2 = (float)(Math.sin(a2) * innerRad);
         bufferBuilder3.vertex(matrix, vx1, 0.0F, vz1).color(r, g, b, 0.85F).next();
         bufferBuilder3.vertex(matrix, vx2, 0.0F, vz2).color(r, g, b, 0.85F).next();
      }

      bufferBuilder3.draw();
      int starPoints = 5;
      float[] starX = new float[starPoints];
      float[] starZ = new float[starPoints];

      for (int k = 0; k < starPoints; k++) {
         double angle = Math.toRadians(k * (360.0 / starPoints));
         starX[k] = (float)(Math.cos(angle) * innerRad);
         starZ[k] = (float)(Math.sin(angle) * innerRad);
      }
      Buf bufferBuilder4 = Buf.begin(matrices, vertexConsumers, Buf.LINES);

      for (int var33 = 0; var33 < starPoints; var33++) {
         int nextK = (var33 + 2) % starPoints;
         bufferBuilder4.vertex(matrix, starX[var33], 0.0F, starZ[var33]).color(r, g, b, 0.9F).next();
         bufferBuilder4.vertex(matrix, starX[nextK], 0.0F, starZ[nextK]).color(r, g, b, 0.9F).next();
      }

      bufferBuilder4.draw();
      matrices.pop();
   }
}
