package com.reallyvisuals.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.particle.ParticlesMode;

/**
 * Conservative client-side performance profile for weak devices/FCL.
 * It avoids background GC and only changes cheap vanilla render options.
 */
public class PerformanceBoost extends Module {
   public final Module.BooleanSetting fastGraphics = new Module.BooleanSetting("Быстрая графика", true);
   public final Module.BooleanSetting noClouds = new Module.BooleanSetting("Отключить облака", true);
   public final Module.BooleanSetting minimalParticles = new Module.BooleanSetting("Меньше частиц", true);
   public final Module.BooleanSetting noEntityShadows = new Module.BooleanSetting("Без теней сущностей", true);
   public final Module.BooleanSetting noAmbientOcclusion = new Module.BooleanSetting("Без мягкого освещения", true);
   public final Module.NumberSetting entityDistance = new Module.NumberSetting("Дальность сущностей %", 75.0, 35.0, 100.0, 5.0);
   public final Module.NumberSetting cullDistance = new Module.NumberSetting("Дальность мелких сущностей", 48.0, 24.0, 96.0, 4.0);
   public final Module.BooleanSetting adaptiveVisuals = new Module.BooleanSetting("Снижать визуалы при просадке", true);
   public final Module.NumberSetting targetFps = new Module.NumberSetting("Порог FPS", 45.0, 25.0, 120.0, 5.0);

   private boolean captured;
   private GraphicsMode oldGraphicsMode;
   private CloudRenderMode oldCloudMode;
   private ParticlesMode oldParticles;
   private Boolean oldAo;
   private Boolean oldEntityShadows;
   private Double oldEntityDistance;
   private int tickCounter;
   private long throttleUntil;
   private int lastFps = 60;

   public PerformanceBoost() {
      super("FPS Boost", "Оптимизация FPS для слабых устройств и FCL", Category.UTILITIES, true, true);
      this.addSetting(this.fastGraphics);
      this.addSetting(this.noClouds);
      this.addSetting(this.minimalParticles);
      this.addSetting(this.noEntityShadows);
      this.addSetting(this.noAmbientOcclusion);
      this.addSetting(this.entityDistance);
      this.addSetting(this.cullDistance);
      this.addSetting(this.adaptiveVisuals);
      this.addSetting(this.targetFps);
   }

   @Override
   public void onEnable() {
      this.captureAndApply();
   }

   @Override
   public void onDisable() {
      this.restoreOptions();
   }

   @Override
   public void onTick() {
      if (!this.isEnabled()) {
         return;
      }

      if (!this.captured) {
         this.captureAndApply();
      }

      if (++this.tickCounter >= 20) {
         this.tickCounter = 0;
         this.applyProfile();
         this.lastFps = readFps();
         if (this.adaptiveVisuals.value && this.lastFps > 0 && this.lastFps < (int)this.targetFps.value) {
            this.throttleUntil = System.currentTimeMillis() + 1800L;
         }
      }
   }

   private void captureAndApply() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc == null || mc.options == null) {
         return;
      }
      if (!this.captured) {
         GameOptions o = mc.options;
         this.oldGraphicsMode = GraphicsMode.FANCY;
         this.oldCloudMode = (CloudRenderMode) o.getCloudRenderMode().getValue();
         this.oldParticles = (ParticlesMode) o.getParticles().getValue();
         this.oldAo = (Boolean) o.getAo().getValue();
         this.oldEntityShadows = (Boolean) o.getEntityShadows().getValue();
         this.oldEntityDistance = (Double) o.getEntityDistanceScaling().getValue();
         this.captured = true;
      }
      this.applyProfile();
   }

   private void applyProfile() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc == null || mc.options == null) {
         return;
      }
      GameOptions o = mc.options;
      if (this.fastGraphics.value) o.applyGraphicsMode(GraphicsMode.FAST);
      if (this.noClouds.value) o.getCloudRenderMode().setValue(CloudRenderMode.OFF);
      if (this.minimalParticles.value) o.getParticles().setValue(ParticlesMode.MINIMAL);
      if (this.noEntityShadows.value) o.getEntityShadows().setValue(false);
      if (this.noAmbientOcclusion.value) o.getAo().setValue(Boolean.FALSE);
      o.getEntityDistanceScaling().setValue(Double.valueOf(Math.max(0.35, Math.min(1.0, this.entityDistance.value / 100.0))));
   }

   private void restoreOptions() {
      if (!this.captured) {
         return;
      }
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc != null && mc.options != null) {
         GameOptions o = mc.options;
         if (this.oldGraphicsMode != null) o.applyGraphicsMode(this.oldGraphicsMode);
         if (this.oldCloudMode != null) o.getCloudRenderMode().setValue(this.oldCloudMode);
         if (this.oldParticles != null) o.getParticles().setValue(this.oldParticles);
         if (this.oldAo != null) o.getAo().setValue(this.oldAo);
         o.getEntityShadows().setValue(this.oldEntityShadows);
         o.getEntityDistanceScaling().setValue(this.oldEntityDistance);
      }
      this.captured = false;
      this.throttleUntil = 0L;
   }

   public boolean shouldThrottleVisuals() {
      return this.isEnabled() && this.adaptiveVisuals.value && System.currentTimeMillis() < this.throttleUntil;
   }

   public double getCullDistanceSquared() {
      double d = this.cullDistance.value;
      return d * d;
   }

   public int getLastFps() {
      return this.lastFps;
   }

   public static PerformanceBoost getInstanceSafe() {
      Module m = ModuleManager.getInstance().getModule("FPS Boost");
      return m instanceof PerformanceBoost ? (PerformanceBoost)m : null;
   }

   public static boolean throttleHeavyVisuals() {
      PerformanceBoost boost = getInstanceSafe();
      return boost != null && boost.shouldThrottleVisuals();
   }

   public static double smallEntityCullDistanceSquared() {
      PerformanceBoost boost = getInstanceSafe();
      return boost != null && boost.isEnabled() ? boost.getCullDistanceSquared() : 3600.0;
   }

   private static int readFps() {
      try {
         MinecraftClient mc = MinecraftClient.getInstance();
         String debug = String.valueOf(mc.getCurrentFps());
         if (debug != null) {
            int space = debug.indexOf(' ');
            String value = space > 0 ? debug.substring(0, space) : debug;
            return Integer.parseInt(value.trim());
         }
      } catch (Throwable ignored) {
      }
      return 60;
   }
}
