package com.reallyvisuals.module;

import com.reallyvisuals.gui.ReallyVisualsScreen;
import com.reallyvisuals.gui.render.RenderUtils3D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

public class Slipstream extends Module {
   public Module.BooleanSetting hideFirstPerson = new Module.BooleanSetting("Скрывать от 1-го лица", false);
   public Module.BooleanSetting clientColor = new Module.BooleanSetting("Цвет клиента", true);
   private final List<Slipstream.SpeedLine> speedLines = new ArrayList<>();
   private final List<Slipstream.WindRing> windRings = new ArrayList<>();
   private final List<Vec3d> leftWingTrail = new ArrayList<>();
   private final List<Vec3d> rightWingTrail = new ArrayList<>();
   private final Random random = new Random();
   private long lastRingTime = 0L;

   public Slipstream() {
      super("Slipstream", "Эффекты ветра при полёте на элитрах", Category.VISUALS, false, true);
      this.addSetting(this.hideFirstPerson);
      this.addSetting(this.clientColor);
   }

   public void onRender3D(MatrixStack matrices, VertexConsumerProvider vertexConsumers, float tickDelta) {
      if (this.isEnabled()) {
         MinecraftClient mc = MinecraftClient.getInstance();
         ClientPlayerEntity player = mc.player;
         if (player != null && mc.world != null) {
            boolean isFirstPerson = mc.options.getPerspective().isFirstPerson();
            if (!this.hideFirstPerson.value || !isFirstPerson) {
               boolean isFlying = player.isGliding();
               Vec3d velocity = player.getVelocity();
               double speed = velocity.length();
               int accentColor = this.clientColor.value ? ReallyVisualsScreen.clientColor : this.customColor;
               if (!isFlying && !(speed > 0.4)) {
                  if (!this.leftWingTrail.isEmpty()) {
                     this.leftWingTrail.remove(this.leftWingTrail.size() - 1);
                  }

                  if (!this.rightWingTrail.isEmpty()) {
                     this.rightWingTrail.remove(this.rightWingTrail.size() - 1);
                  }
               } else {
                  int lineCap = PerformanceBoost.throttleHeavyVisuals() ? 14 : 40;
                  if (this.speedLines.size() < lineCap) {
                     Vec3d pPos = player.getEntityPos().add(0.0, player.getStandingEyeHeight(), 0.0);
                     Vec3d look = player.getRotationVec(tickDelta);
                     Vec3d spawnPos = pPos.add(
                        look.x * (5.0 + this.random.nextDouble() * 10.0) + (this.random.nextDouble() - 0.5) * 6.0,
                        look.y * (5.0 + this.random.nextDouble() * 10.0) + (this.random.nextDouble() - 0.5) * 4.0,
                        look.z * (5.0 + this.random.nextDouble() * 10.0) + (this.random.nextDouble() - 0.5) * 6.0
                     );
                     this.speedLines.add(new Slipstream.SpeedLine(spawnPos, look.multiply(-1.5 - this.random.nextDouble() * 1.5)));
                  }

                  double yawRad = Math.toRadians(player.getYaw());
                  double pitchRad = Math.toRadians(player.getPitch());
                  Vec3d backVec = new Vec3d(-Math.sin(yawRad), 0.0, Math.cos(yawRad)).multiply(0.8);
                  Vec3d rightVec = new Vec3d(-Math.cos(yawRad), 0.0, -Math.sin(yawRad));
                  Vec3d leftWing = player.getEntityPos().add(0.0, 1.0, 0.0).add(rightVec.multiply(-0.9)).add(backVec);
                  Vec3d rightWing = player.getEntityPos().add(0.0, 1.0, 0.0).add(rightVec.multiply(0.9)).add(backVec);
                  this.leftWingTrail.add(0, leftWing);
                  this.rightWingTrail.add(0, rightWing);
                  int trailCap = PerformanceBoost.throttleHeavyVisuals() ? 12 : 25;
                  while (this.leftWingTrail.size() > trailCap) this.leftWingTrail.remove(this.leftWingTrail.size() - 1);
                  while (this.rightWingTrail.size() > trailCap) this.rightWingTrail.remove(this.rightWingTrail.size() - 1);

                  long now = System.currentTimeMillis();
                  long ringDelay = PerformanceBoost.throttleHeavyVisuals() ? 700L : 350L;
                  if (isFlying && now - this.lastRingTime > ringDelay) {
                     this.windRings.add(new Slipstream.WindRing(player.getEntityPos().add(0.0, 1.0, 0.0), player.getRotationVec(1.0F)));
                     this.lastRingTime = now;
                  }
               }

               RenderUtils3D.drawRibbonStream(matrices, vertexConsumers, this.leftWingTrail, accentColor, 0.35F);
               RenderUtils3D.drawRibbonStream(matrices, vertexConsumers, this.rightWingTrail, accentColor, 0.35F);
               Iterator<Slipstream.SpeedLine> lineIt = this.speedLines.iterator();

               while (lineIt.hasNext()) {
                  Slipstream.SpeedLine line = lineIt.next();
                  line.update();
                  if (line.alpha <= 0.0F) {
                     lineIt.remove();
                  } else {
                     RenderUtils3D.drawSpeedLine(matrices, vertexConsumers, line.pos, line.velocity, accentColor, line.alpha);
                  }
               }

               Iterator<Slipstream.WindRing> ringIt = this.windRings.iterator();

               while (ringIt.hasNext()) {
                  Slipstream.WindRing ring = ringIt.next();
                  ring.update();
                  if (ring.alpha <= 0.0F) {
                     ringIt.remove();
                  } else {
                     RenderUtils3D.drawWindRing(matrices, vertexConsumers, ring.pos, ring.direction, ring.radius, accentColor, ring.alpha);
                  }
               }
            }
         }
      }
   }

   private static class SpeedLine {
      Vec3d pos;
      Vec3d velocity;
      float alpha = 1.0F;

      SpeedLine(Vec3d pos, Vec3d velocity) {
         this.pos = pos;
         this.velocity = velocity;
      }

      void update() {
         this.pos = this.pos.add(this.velocity);
         this.alpha -= 0.05F;
      }
   }

   private static class WindRing {
      Vec3d pos;
      Vec3d direction;
      float radius = 0.5F;
      float alpha = 1.0F;

      WindRing(Vec3d pos, Vec3d direction) {
         this.pos = pos;
         this.direction = direction;
      }

      void update() {
         this.radius += 0.15F;
         this.pos = this.pos.add(this.direction.multiply(0.3));
         this.alpha -= 0.04F;
      }
   }
}
