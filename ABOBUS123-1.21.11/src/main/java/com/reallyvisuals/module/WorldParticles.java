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

public class WorldParticles extends Module {
   public static final String[] PARTICLE_TYPES = new String[]{"Кубики", "Светлячки", "Метеоры", "Звёзды", "Лепестки", "Бабочки"};
   public Module.MultiSelectSetting mode = new Module.MultiSelectSetting("Режим", PARTICLE_TYPES, new String[]{"Кубики"});
   public Module.NumberSetting amount = new Module.NumberSetting("Количество", 100.0, 10.0, 300.0, 10.0);
   public Module.NumberSetting spawnRadius = new Module.NumberSetting("Радиус спавна", 20.0, 5.0, 50.0, 1.0);
   public Module.NumberSetting spawnHeight = new Module.NumberSetting("Высота спавна", 5.0, 1.0, 20.0, 1.0);
   public Module.BooleanSetting glow = new Module.BooleanSetting("Свечение", true);
   public Module.BooleanSetting clientColor = new Module.BooleanSetting("Цвет клиента", true);
   private final List<WorldParticles.Particle3D> particles = new ArrayList<>();
   private final Random random = new Random();

   public WorldParticles() {
      super("World Particles", "Объёмные частицы вокруг игрока", Category.VISUALS, false, true);
      this.addSetting(this.mode);
      this.addSetting(this.amount);
      this.addSetting(this.spawnRadius);
      this.addSetting(this.spawnHeight);
      this.addSetting(this.glow);
      this.addSetting(this.clientColor);
   }

   public void onRender3D(MatrixStack matrices, VertexConsumerProvider vertexConsumers, float tickDelta) {
      if (this.isEnabled()) {
         MinecraftClient mc = MinecraftClient.getInstance();
         ClientPlayerEntity player = mc.player;
         if (player != null && mc.world != null) {
            if (!this.mode.selected.isEmpty()) {
               int targetAmount = (int)this.amount.value;
               PerformanceBoost boost = PerformanceBoost.getInstanceSafe();
               if (boost != null && boost.isEnabled()) {
                  targetAmount = Math.min(targetAmount, PerformanceBoost.throttleHeavyVisuals() ? 40 : 120);
               }
               while (this.particles.size() > targetAmount) this.particles.remove(0);
               Vec3d pPos = player.getEntityPos();

               String[] selectedTypes = this.mode.selected.toArray(new String[0]);
               while (this.particles.size() < targetAmount && selectedTypes.length > 0) {
                  String pType = selectedTypes[this.random.nextInt(selectedTypes.length)];
                  this.spawnParticle(pPos, pType);
               }

               int accentColor = this.clientColor.value ? ReallyVisualsScreen.clientColor : this.customColor;
               boolean enableGlow = this.glow.value;
               Iterator<WorldParticles.Particle3D> iterator = this.particles.iterator();

               while (iterator.hasNext()) {
                  WorldParticles.Particle3D p = iterator.next();
                  p.update(pPos, this.spawnRadius.value, this.spawnHeight.value);
                  if (p.dead) {
                     iterator.remove();
                  } else {
                     RenderUtils3D.drawParticle3D(matrices, vertexConsumers, p, accentColor, enableGlow);
                  }
               }
            }
         }
      }
   }

   private void spawnParticle(Vec3d pPos, String type) {
      double r = this.spawnRadius.value;
      double h = this.spawnHeight.value;
      double px = pPos.x + (this.random.nextDouble() - 0.5) * r * 2.0;
      double py = pPos.y + this.random.nextDouble() * h;
      double pz = pPos.z + (this.random.nextDouble() - 0.5) * r * 2.0;
      double vx = (this.random.nextDouble() - 0.5) * 0.04;
      double vy = !type.equals("Метеоры") && !type.equals("Лепестки") ? (this.random.nextDouble() - 0.5) * 0.02 : -(0.03 + this.random.nextDouble() * 0.05);
      double vz = (this.random.nextDouble() - 0.5) * 0.04;
      float scale = 0.15F + this.random.nextFloat() * 0.25F;
      if (type.equals("Кубики")) {
         scale *= 2.0F;
      }

      this.particles.add(new WorldParticles.Particle3D(new Vec3d(px, py, pz), new Vec3d(vx, vy, vz), type, scale, 150 + this.random.nextInt(200)));
   }

   public static class Particle3D {
      public Vec3d pos;
      public Vec3d velocity;
      public String type;
      public float scale;
      public float rotX;
      public float rotY;
      public float rotZ;
      public float rotSpeedX;
      public float rotSpeedY;
      public float rotSpeedZ;
      public float alpha = 0.0F;
      public int age = 0;
      public int maxAge;
      public boolean dead = false;

      public Particle3D(Vec3d pos, Vec3d velocity, String type, float scale, int maxAge) {
         this.pos = pos;
         this.velocity = velocity;
         this.type = type;
         this.scale = scale;
         this.maxAge = maxAge;
         Random rand = new Random();
         this.rotX = rand.nextFloat() * 360.0F;
         this.rotY = rand.nextFloat() * 360.0F;
         this.rotZ = rand.nextFloat() * 360.0F;
         this.rotSpeedX = (rand.nextFloat() - 0.5F) * 4.0F;
         this.rotSpeedY = (rand.nextFloat() - 0.5F) * 4.0F;
         this.rotSpeedZ = (rand.nextFloat() - 0.5F) * 4.0F;
      }

      public void update(Vec3d playerPos, double radius, double height) {
         this.age++;
         this.pos = this.pos.add(this.velocity);
         this.rotX = this.rotX + this.rotSpeedX;
         this.rotY = this.rotY + this.rotSpeedY;
         this.rotZ = this.rotZ + this.rotSpeedZ;
         float lifeProgress = (float)this.age / this.maxAge;
         this.alpha = lifeProgress < 0.2F ? lifeProgress / 0.2F : (lifeProgress > 0.8F ? (1.0F - lifeProgress) / 0.2F : 1.0F);
         if (this.age >= this.maxAge || this.pos.distanceTo(playerPos) > radius * 1.5) {
            this.dead = true;
         }
      }
   }
}
